package com.rkdevstudios.tripledger.core.network

data class NetworkResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?
)

data class ApiError(
    val code: String,
    val message: String
)
