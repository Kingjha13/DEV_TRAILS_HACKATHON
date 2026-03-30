package com.example.shieldnet.data.api

import com.example.shieldnet.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ShieldNetApi {

    @POST("api/auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<OtpSendResponse>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<OtpVerifyResponse>

    @POST("api/workers/register")
    suspend fun registerWorker(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/risk/score")
    suspend fun getRiskScore(@Body request: RiskRequest): Response<RiskResponse>

    @GET("api/policies/{workerId}/active")
    suspend fun getActivePolicy(@Path("workerId") workerId: String): Response<PolicyResponse>

    @POST("api/policies/create")
    suspend fun createPolicy(@Body request: PolicyCreateRequest): Response<PolicyResponse>

    @GET("api/claims/{workerId}")
    suspend fun getClaims(@Path("workerId") workerId: String): Response<List<ClaimResponse>>

    @GET("api/triggers/status")
    suspend fun getTriggerStatus(@Query("city") city: String): Response<TriggerStatusResponse>
}