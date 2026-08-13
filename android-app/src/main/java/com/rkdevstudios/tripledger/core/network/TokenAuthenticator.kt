package com.rkdevstudios.tripledger.core.network

import com.google.gson.Gson
import com.rkdevstudios.tripledger.core.auth.SessionManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val baseUrl: String
) : Authenticator {

    private val gson = Gson()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            sessionManager.clearSession()
            return null
        }

        synchronized(this) {
            val currentToken = sessionManager.getAccessToken()
            val requestHeaderToken = response.request.header("Authorization")?.replace("Bearer ", "")

            if (currentToken != requestHeaderToken && !currentToken.isNullOrBlank()) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                sessionManager.clearSession()
                return null
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val refreshRequestBody = "{\"refreshToken\":\"$refreshToken\"}".toRequestBody(mediaType)

            val refreshRequest = Request.Builder()
                .url(baseUrl + "api/v1/auth/refresh")
                .post(refreshRequestBody)
                .build()

            val refreshClient = OkHttpClient.Builder().build()
            try {
                refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                    if (refreshResponse.isSuccessful) {
                        val bodyString = refreshResponse.body?.string()
                        val apiResponse = gson.fromJson(bodyString, ApiResponseWrapper::class.java)
                        val data = apiResponse?.data

                        if (data != null && data.containsKey("accessToken") && data.containsKey("refreshToken")) {
                            val newAccess = data["accessToken"] as String
                            val newRefresh = data["refreshToken"] as String

                            sessionManager.updateAccessToken(newAccess, newRefresh)

                            return response.request.newBuilder()
                                .header("Authorization", "Bearer $newAccess")
                                .build()
                        }
                    }
                }
            } catch (e: IOException) {
                // Ignore and fallback
            }

            sessionManager.clearSession()
            return null
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var parent = response.priorResponse
        while (parent != null) {
            result++
            parent = parent.priorResponse
        }
        return result
    }

    private class ApiResponseWrapper {
        var success: Boolean = false
        var data: Map<String, Any>? = null
    }
}
