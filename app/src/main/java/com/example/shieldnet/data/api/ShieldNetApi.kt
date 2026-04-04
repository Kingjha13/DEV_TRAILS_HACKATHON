package com.example.shieldnet.data.api

import com.example.shieldnet.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ShieldNetApi {

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body req: OtpRequest): Response<OtpSendResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body req: OtpVerifyRequest): Response<OtpVerifyResponse>

    @POST("workers/register")
    suspend fun registerWorker(@Body req: RegisterRequest): Response<RegisterResponse>

    @POST("risk/analyze")
    suspend fun getRiskScore(@Body req: RiskRequest): Response<FraudApiResponse>


    @GET("policies/active")
    suspend fun getActivePolicy(
        @Query("worker_id") workerId: String
    ): Response<PolicyResponse>

    @POST("policies/create")
    suspend fun createPolicy(
        @Body req: PolicyCreateRequest
    ): Response<PolicyResponse>

    @GET("claims/list")
    suspend fun getClaims(
        @Query("worker_id") workerId: String
    ): Response<List<ClaimResponse>>

    @GET("status/triggers")
    suspend fun getTriggerStatus(
        @Query("city") city: String
    ): Response<TriggerStatusResponse>
}