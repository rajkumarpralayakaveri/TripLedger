package com.rkdevstudios.tripledger.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

class ColdStartRecoveryInterceptor(
    private val maxRetries: Int = 2,
    private val initialWaitMs: Long = 3000L,
    private val waitMultiplier: Double = 1.66,
    private val hardCapMs: Long = 75000L
) : Interceptor {

    companion object {
        private val recoveryLock = ReentrantLock()
        private val isRecovering = AtomicBoolean(false)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Guarantee 1: GET-only retries. POST/PUT/PATCH/DELETE are NEVER retried automatically.
        if (!request.method.equals("GET", ignoreCase = true)) {
            return chain.proceed(request)
        }

        val startTime = System.currentTimeMillis()

        // Attempt 1: Execute request with standard 15s timeout
        var response: Response? = null
        var lastException: IOException? = null

        try {
            response = chain.proceed(request)
            if (response.isSuccessful || response.code != 503) {
                ServerState.setOnline()
                return response
            }
        } catch (e: SocketTimeoutException) {
            lastException = e
        } catch (e: IOException) {
            // Non-timeout IOExceptions (e.g. standard connection errors) pass through directly
            throw e
        }

        // If initial request failed with timeout or 503, initiate controlled cold-start recovery
        ServerState.setWakingUp()

        var retryCount = 0
        var currentWaitMs = initialWaitMs

        while (retryCount < maxRetries) {
            val elapsedTime = System.currentTimeMillis() - startTime
            if (elapsedTime >= hardCapMs) {
                break
            }

            // Single-flight coordination lock: ensures concurrent GET requests wait for server recovery status
            // without sharing HTTP response objects across different endpoints.
            if (!isRecovering.get()) {
                if (recoveryLock.tryLock()) {
                    try {
                        isRecovering.set(true)
                        Thread.sleep(currentWaitMs)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } finally {
                        isRecovering.set(false)
                        recoveryLock.unlock()
                    }
                } else {
                    // Another request is coordinating recovery; wait on the lock then re-attempt own endpoint
                    try {
                        recoveryLock.lock()
                    } finally {
                        if (recoveryLock.isHeldByCurrentThread) {
                            recoveryLock.unlock()
                        }
                    }
                }
            } else {
                // Recovery in progress by another thread; pause briefly
                try {
                    Thread.sleep(currentWaitMs)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }

            retryCount++

            // Calculate bounded read/connect timeouts for retry attempts (capped within total hard cap)
            val newReadTimeout = (15 + (retryCount * 15)).coerceAtMost(45)
            val customChain = chain
                .withConnectTimeout(newReadTimeout, TimeUnit.SECONDS)
                .withReadTimeout(newReadTimeout, TimeUnit.SECONDS)

            try {
                response?.close()
                val retryResponse = customChain.proceed(request)
                if (retryResponse.isSuccessful || retryResponse.code != 503) {
                    ServerState.setOnline()
                    return retryResponse
                }
                response = retryResponse
            } catch (e: SocketTimeoutException) {
                lastException = e
            }

            currentWaitMs = (currentWaitMs * waitMultiplier).toLong()
        }

        // If all retries exhausted, update state to Offline and throw/return original result
        ServerState.setOffline()
        if (lastException != null) {
            throw lastException
        }
        return response ?: throw SocketTimeoutException("Cold-start recovery exhausted after 75s")
    }
}
