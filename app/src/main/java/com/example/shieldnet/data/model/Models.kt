package com.example.shieldnet.data.model

import com.google.gson.annotations.SerializedName


data class OtpRequest(val phone: String)

data class OtpVerifyRequest(val phone: String, val otp: String)

data class RegisterRequest(
    val name: String,
    val phone: String,
    val city: String,
    val platform: String,
    @SerializedName("weekly_avg") val weeklyAvg: Int,
    @SerializedName("upi_handle") val upiHandle: String
)

data class RiskRequest(
    val city: String,
    val platform: String,
    @SerializedName("delivery_zone") val deliveryZone: String,
    val lat: Double,
    val lon: Double
)

data class PolicyCreateRequest(
    @SerializedName("worker_id") val workerId: String,
    @SerializedName("plan_tier") val planTier: String,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_order_id") val razorpayOrderId: String
)


data class OtpSendResponse(val message: String, val success: Boolean)

data class OtpVerifyResponse(
    val token: String,
    @SerializedName("worker_id") val workerId: String?,
    @SerializedName("is_registered") val isRegistered: Boolean
)

data class RegisterResponse(
    val id: String,
    val name: String,
    val phone: String,
    @SerializedName("risk_score") val riskScore: Float?,
    val token: String
)

data class RiskResponse(
    @SerializedName("risk_score") val riskScore: Float,
    @SerializedName("risk_level") val riskLevel: String,
    @SerializedName("weekly_premium") val weeklyPremium: Int,
    @SerializedName("coverage_amount") val coverageAmount: Int,
    val factors: RiskFactors
)

data class RiskFactors(
    @SerializedName("rainfall_risk") val rainfallRisk: Float,
    @SerializedName("flood_freq") val floodFreq: Float,
    @SerializedName("aqi_risk") val aqiRisk: Float,
    @SerializedName("congestion_index") val congestionIndex: Float
)

data class PolicyResponse(
    val id: String,
    @SerializedName("plan_tier") val planTier: String,
    @SerializedName("premium_inr") val premiumInr: Int,
    @SerializedName("coverage_inr") val coverageInr: Int,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("expires_at") val expiresAt: String,
    val status: String
)

data class ClaimResponse(
    val id: String,
    @SerializedName("event_type") val eventType: String,
    @SerializedName("estimated_loss") val estimatedLoss: Int,
    @SerializedName("approved_amount") val approvedAmount: Int?,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("payout_ref") val payoutRef: String?
)

data class TriggerStatusResponse(
    val city: String,
    @SerializedName("active_triggers") val activeTriggers: List<ActiveTrigger>,
    @SerializedName("all_clear") val allClear: Boolean
)

data class ActiveTrigger(
    val type: String,
    val severity: Float,
    val description: String,
    @SerializedName("detected_at") val detectedAt: String
)

data class ApiError(val message: String, val code: Int = 0)
