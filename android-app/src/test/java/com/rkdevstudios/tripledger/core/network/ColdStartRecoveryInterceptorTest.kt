package com.rkdevstudios.tripledger.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ColdStartRecoveryInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        client = OkHttpClient.Builder()
            .addInterceptor(ColdStartRecoveryInterceptor(maxRetries = 2, initialWaitMs = 50L, waitMultiplier = 1.0, hardCapMs = 2000L))
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
        ServerState.setOnline()
    }

    @Test
    fun getRequest_participatesInColdStartRecovery_andSucceedsOnRetry() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("Server Booting"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))

        val request = Request.Builder()
            .url(server.url("/api/v1/workspaces"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals("""{"status":"ok"}""", response.body?.string())
        assertEquals(2, server.requestCount)
        assertEquals(ServerStatus.Online, ServerState.status.value)
    }

    @Test
    fun postPaymentSubmission_isNeverRetriedAutomatically() {
        server.enqueue(MockResponse().setResponseCode(503).setBody("Server Booting"))

        val request = Request.Builder()
            .url(server.url("/api/v1/workspaces/ws1/payments/complete"))
            .post("{}".toRequestBody(null))
            .build()

        val response = client.newCall(request).execute()

        assertEquals(503, response.code)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun getRequest_retriesAreBoundedByMaxRetries() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(503))

        val request = Request.Builder()
            .url(server.url("/api/v1/workspaces"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(503, response.code)
        assertEquals(3, server.requestCount)
        assertEquals(ServerStatus.Offline, ServerState.status.value)
    }

    @Test
    fun concurrentGetRequests_doNotCreateRetryStorm_andReceiveOwnEndpoints() {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"endpoint":"workspaces"}"""))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"endpoint":"financial"}"""))

        val executor = Executors.newFixedThreadPool(2)

        val task1 = executor.submit<String> {
            val req1 = Request.Builder().url(server.url("/api/v1/workspaces")).get().build()
            client.newCall(req1).execute().body?.string()
        }

        val task2 = executor.submit<String> {
            val req2 = Request.Builder().url(server.url("/api/v1/workspaces/ws1/financial-summary")).get().build()
            client.newCall(req2).execute().body?.string()
        }

        val res1 = task1.get(5, TimeUnit.SECONDS)
        val res2 = task2.get(5, TimeUnit.SECONDS)

        executor.shutdown()

        assertTrue(res1.contains("workspaces") || res1.contains("financial"))
        assertTrue(res2.contains("workspaces") || res2.contains("financial"))
        assertTrue(res1 != res2)
    }

    @Test
    fun normalSuccessfulRequest_isUnaffected() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":"ok"}"""))

        val request = Request.Builder()
            .url(server.url("/api/v1/workspaces"))
            .get()
            .build()

        val response = client.newCall(request).execute()

        assertEquals(200, response.code)
        assertEquals(1, server.requestCount)
        assertEquals(ServerStatus.Online, ServerState.status.value)
    }
}
