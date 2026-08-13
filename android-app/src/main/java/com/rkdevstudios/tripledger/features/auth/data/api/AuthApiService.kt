package com.rkdevstudios.tripledger.features.auth.data.api

import com.rkdevstudios.tripledger.core.network.NetworkResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): NetworkResponse<LoginResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): NetworkResponse<Void>
}

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String
)

data class LogoutRequestDto(
    val refreshToken: String
)
