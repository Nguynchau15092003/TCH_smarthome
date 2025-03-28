package com.example.smarthomevoice.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class DeviceRepository {
    private val TAG = "DeviceRepository"
    private val baseUrl = "https://voicecontrolsmarthomeserver.onrender.com/"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    suspend fun controlFan(state: Boolean): Result<ApiResponse> {
        return try {
            Log.d(TAG, "Sending fan control request to: $baseUrl/api/device/fan")
            val response = apiService.controlFan(DeviceStateRequest(state))
            if (response.isSuccessful) {
                Log.d(TAG, "Fan control response: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Fan control failed: $errorBody")
                Result.failure(Exception(errorBody ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fan control exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun controlDoor(command: String): Result<ApiResponse> {
        return try {
            Log.d(TAG, "Sending door control request to: $baseUrl/api/device/servo/door")
            val response = apiService.controlDoor(ServoCommandRequest(command))
            if (response.isSuccessful) {
                Log.d(TAG, "Door control response: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Door control failed: $errorBody")
                Result.failure(Exception(errorBody ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Door control exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun controlCurtain(command: String): Result<ApiResponse> {
        return try {
            Log.d(TAG, "Sending curtain control request to: $baseUrl/api/device/servo/curtain")
            val response = apiService.controlCurtain(ServoCommandRequest(command))
            if (response.isSuccessful) {
                Log.d(TAG, "Curtain control response: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Curtain control failed: $errorBody")
                Result.failure(Exception(errorBody ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Curtain control exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun controlRoomLight(state: Boolean): Result<ApiResponse> {
        return try {
            Log.d(TAG, "Sending room light control request to: $baseUrl/api/device/light/room")
            val response = apiService.controlRoomLight(DeviceStateRequest(state))
            if (response.isSuccessful) {
                Log.d(TAG, "Room light control response: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Room light control failed: $errorBody")
                Result.failure(Exception(errorBody ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Room light control exception: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun controlReadingLamp(state: Boolean): Result<ApiResponse> {
        return try {
            Log.d(TAG, "Sending reading lamp control request to: $baseUrl/api/device/light/reading")
            val response = apiService.controlReadingLamp(DeviceStateRequest(state))
            if (response.isSuccessful) {
                Log.d(TAG, "Reading lamp control response: ${response.body()}")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Reading lamp control failed: $errorBody")
                Result.failure(Exception(errorBody ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reading lamp control exception: ${e.message}")
            Result.failure(e)
        }
    }
} 