package com.example.shieldnet.data.api;

import com.example.shieldnet.data.model.*;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ShieldNetApi {

    @POST("auth/send-otp")
    Call<OtpSendResponse> sendOtp(@Body OtpRequest req);

    @POST("auth/verify-otp")
    Call<OtpVerifyResponse> verifyOtp(@Body OtpVerifyRequest req);

    @POST("workers/register")
    Call<RegisterResponse> registerWorker(@Body RegisterRequest req);

    @POST("risk/analyze")
    Call<FraudApiResponse> getRiskScore(@Body RiskRequest req);

    @GET("policies/active")
    Call<PolicyResponse> getActivePolicy(
            @Query("worker_id") String workerId
    );

    @POST("policies/create")
    Call<PolicyResponse> createPolicy(
            @Body PolicyCreateRequest req
    );

    @GET("claims/list")
    Call<List<ClaimResponse>> getClaims(
            @Query("worker_id") String workerId
    );

    @GET("status/triggers")
    Call<TriggerStatusResponse> getTriggerStatus(
            @Query("city") String city
    );
}