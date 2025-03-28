package com.example.smarthomevoice.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/device/fan")
    suspend fun controlFan(@Body request: DeviceStateRequest): Response<ApiResponse>

    @POST("/api/device/servo/door")
    suspend fun controlDoor(@Body request: ServoCommandRequest): Response<ApiResponse>

    @POST("/api/device/servo/curtain")
    suspend fun controlCurtain(@Body request: ServoCommandRequest): Response<ApiResponse>

    @POST("/api/device/light/room")
    suspend fun controlRoomLight(@Body request: DeviceStateRequest): Response<ApiResponse>

    @POST("/api/device/light/reading")
    suspend fun controlReadingLamp(@Body request: DeviceStateRequest): Response<ApiResponse>
}

data class DeviceStateRequest(
    val state: Boolean
)

data class ServoCommandRequest(
    val command: String
)

data class ApiResponse(
    val success: Boolean,
    val error: String? = null
) 