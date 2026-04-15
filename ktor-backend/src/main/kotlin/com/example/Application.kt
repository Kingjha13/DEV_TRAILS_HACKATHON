package com.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random



@Serializable data class OtpRequest(val phone: String)

@Serializable data class OtpSendResponse(val message: String, val success: Boolean)

@Serializable data class OtpVerifyRequest(val phone: String, val otp: String)

@Serializable data class OtpVerifyResponse(
    val token: String,
    @SerialName("worker_id")    val workerId: String?,
    @SerialName("is_registered") val isRegistered: Boolean
)

@Serializable data class RegisterRequest(
    val name: String,
    val phone: String,
    val city: String,
    val platform: String,
    @SerialName("weekly_avg")  val weeklyAvg: Int,
    @SerialName("upi_handle")  val upiHandle: String
)

@Serializable data class RegisterResponse(
    val id: String,
    val name: String,
    val phone: String,
    @SerialName("risk_score") val riskScore: Float?,
    val token: String
)

@Serializable data class PolicyCreateRequest(
    @SerialName("worker_id")           val workerId: String,
    @SerialName("plan_tier")           val planTier: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_order_id")   val razorpayOrderId: String
)

@Serializable data class PolicyResponse(
    val id: String,
    @SerialName("plan_tier")    val planTier: String,
    @SerialName("premium_inr")  val premiumInr: Int,
    @SerialName("coverage_inr") val coverageInr: Int,
    @SerialName("starts_at")    val startsAt: String,
    @SerialName("expires_at")   val expiresAt: String,
    val status: String
)

@Serializable data class ClaimResponse(
    val id: String,
    @SerialName("event_type")      val eventType: String,
    @SerialName("estimated_loss")  val estimatedLoss: Int,
    @SerialName("approved_amount") val approvedAmount: Int?,
    val status: String,
    @SerialName("created_at")  val createdAt: String,
    @SerialName("payout_ref")  val payoutRef: String?
)

@Serializable data class ActiveTrigger(
    val type: String,
    val severity: Float,
    val description: String,
    val threshold: Float,
    @SerialName("threshold_breached") val thresholdBreached: Boolean,
    @SerialName("detected_at") val detectedAt: String
)

@Serializable data class TriggerStatusResponse(
    val city: String,
    @SerialName("active_triggers") val activeTriggers: List<ActiveTrigger>,
    @SerialName("all_clear")       val allClear: Boolean
)

@Serializable data class DemoTriggerRequest(
    @SerialName("worker_id")        val workerId: String,
    val city: String,
    @SerialName("event_type")       val eventType: String,
    @SerialName("policy_age_hours") val policyAgeHours: Double,
    val severity: Double
)

@Serializable data class DemoTriggerResponse(
    val triggered: Boolean,
    @SerialName("fraud_score")    val fraudScore: Double,
    val decision: String,
    val flags: List<String>,
    @SerialName("payout_inr")    val payoutInr: Int?,
    @SerialName("payout_ref")    val payoutRef: String?,
    val message: String
)

val otpStore = ConcurrentHashMap<String, String>()

val phoneToWorker = ConcurrentHashMap<String, String>()

val activePolicies = ConcurrentHashMap<String, PolicyResponse>()

val claimsStore = ConcurrentHashMap<String, MutableList<ClaimResponse>>()

fun nowIso(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

fun isoAfterDays(days: Long): String =
    DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(days * 86400))

fun premiumForTier(tier: String) = when (tier.lowercase()) {
    "basic"    -> 99
    "standard" -> 199
    else       -> 399
}

fun coverageForTier(tier: String) = when (tier.lowercase()) {
    "basic"    -> 10_000
    "standard" -> 25_000
    else       -> 50_000
}


fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {

    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    install(ContentNegotiation) { json(json) }

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) { json(json) }
    }

    val fraudUrl = System.getenv("FRAUD_URL")
        ?: "https://shieldnet-fraud.onrender.com/score"

    routing {

        get("/") {
            call.respond(mapOf("message" to "ShieldNet backend running"))
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }


        post("/auth/send-otp") {
            val req = call.receive<OtpRequest>()
            val otp = (100000..999999).random().toString()
            otpStore[req.phone] = otp
            println("OTP for ${req.phone}: $otp")

            call.respond(OtpSendResponse(
                message = "OTP sent to ${req.phone}. [DEV: $otp]",
                success = true
            ))
        }

        post("/auth/verify-otp") {
            val req = call.receive<OtpVerifyRequest>()
            val expected = otpStore[req.phone]

            if (expected == null || expected != req.otp) {
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Invalid or expired OTP"))
                return@post
            }

            otpStore.remove(req.phone)

            val existingWorkerId = phoneToWorker[req.phone]
            val token = "token_${System.currentTimeMillis()}_${Random.nextInt(9999)}"

            call.respond(OtpVerifyResponse(
                token        = token,
                workerId     = existingWorkerId,
                isRegistered = existingWorkerId != null
            ))
        }


        post("/workers/register") {
            try {
                val req = call.receive<RegisterRequest>()
                val workerId = "worker_${System.currentTimeMillis()}"
                phoneToWorker[req.phone] = workerId

                val token = "token_${System.currentTimeMillis()}_${Random.nextInt(9999)}"

                call.respond(RegisterResponse(
                    id        = workerId,
                    name      = req.name,
                    phone     = req.phone,
                    riskScore = null,
                    token     = token
                ))
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Registration failed")))
            }
        }


        post("/risk/analyze") {
            try {
                val req = call.receive<FraudRequest>()
                val response: FraudResponse = httpClient.post(fraudUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.body()
                call.respond(response)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Risk analysis failed")))
            }
        }


        get("/policies/active") {
            val workerId = call.request.queryParameters["worker_id"]
            if (workerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "worker_id required"))
                return@get
            }
            val policy = activePolicies[workerId]
            if (policy == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active policy"))
            } else {
                call.respond(policy)
            }
        }

        post("/policies/create") {
            try {
                val req = call.receive<PolicyCreateRequest>()
                val policy = PolicyResponse(
                    id          = "pol_${System.currentTimeMillis()}",
                    planTier    = req.planTier,
                    premiumInr  = premiumForTier(req.planTier),
                    coverageInr = coverageForTier(req.planTier),
                    startsAt    = nowIso(),
                    expiresAt   = isoAfterDays(30),
                    status      = "active"
                )
                activePolicies[req.workerId] = policy
                call.respond(policy)
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Policy creation failed")))
            }
        }


        get("/claims/list") {
            val workerId = call.request.queryParameters["worker_id"]
            if (workerId == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "worker_id required"))
                return@get
            }
            call.respond(claimsStore[workerId] ?: emptyList<ClaimResponse>())
        }

        get("/status/triggers") {
            val city = call.request.queryParameters["city"] ?: "Chennai"
            val triggersByCity = mapOf(
                "Chennai" to listOf(
                    ActiveTrigger(
                        type              = "rainfall",
                        severity          = 0.82f,
                        description       = "Heavy rainfall detected — index 8.2 / 10",
                        threshold         = 0.75f,
                        thresholdBreached = true,
                        detectedAt        = nowIso()
                    )
                ),
                "Mumbai" to listOf(
                    ActiveTrigger(
                        type              = "platform_outage",
                        severity          = 0.91f,
                        description       = "Swiggy platform degraded — uptime below SLA for 2.5h",
                        threshold         = 0.80f,
                        thresholdBreached = true,
                        detectedAt        = nowIso()
                    )
                ),
                "Delhi" to listOf(
                    ActiveTrigger(
                        type              = "heat_index",
                        severity          = 0.78f,
                        description       = "Extreme heat — 43°C sustained for 3h",
                        threshold         = 0.75f,
                        thresholdBreached = true,
                        detectedAt        = nowIso()
                    )
                ),
                "Bengaluru" to listOf(
                    ActiveTrigger(
                        type              = "traffic_congestion",
                        severity          = 0.61f,
                        description       = "Congestion index 61% — below payout threshold",
                        threshold         = 0.75f,
                        thresholdBreached = false,
                        detectedAt        = nowIso()
                    )
                )
            )

            val activeTriggers = triggersByCity[city] ?: emptyList()
            val allClear = activeTriggers.none { it.thresholdBreached }

            call.respond(TriggerStatusResponse(
                city           = city,
                activeTriggers = activeTriggers,
                allClear       = allClear
            ))
        }

        post("/demo/trigger") {
            try {
                val req = call.receive<DemoTriggerRequest>()

                if (req.severity < 0.20) {
                    call.respond(
                        DemoTriggerResponse(
                            triggered  = false,
                            fraudScore = 0.0,
                            decision   = "reject",
                            flags      = listOf("Event severity ${req.severity} is below minimum claimable threshold (0.20)"),
                            payoutInr  = null,
                            payoutRef  = null,
                            message    = "Event did not cross the parametric threshold. No payout triggered."
                        )
                    )
                    return@post
                }

                val fraudReq = FraudRequest(
                    worker_id        = req.workerId,
                    city             = req.city,
                    event_type       = req.eventType,
                    policy_age_hours = req.policyAgeHours,
                    severity         = req.severity
                )
                val fraudResp: FraudResponse = httpClient.post(fraudUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(fraudReq)
                }.body()

                if (fraudResp.decision == "approve") {
                    val payoutAmount = (req.severity * 600).toInt().coerceIn(200, 600)
                    val payoutRef    = "PAY_${System.currentTimeMillis()}"
                    val claim = ClaimResponse(
                        id             = "clm_${System.currentTimeMillis()}",
                        eventType      = req.eventType,
                        estimatedLoss  = payoutAmount + 100,
                        approvedAmount = payoutAmount,
                        status         = "paid",
                        createdAt      = nowIso(),
                        payoutRef      = payoutRef
                    )
                    claimsStore.getOrPut(req.workerId) { mutableListOf() }.add(claim)

                    call.respond(
                        DemoTriggerResponse(
                            triggered  = true,
                            fraudScore = fraudResp.fraud_score,
                            decision   = "approve",
                            flags      = fraudResp.flags,
                            payoutInr  = payoutAmount,
                            payoutRef  = payoutRef,
                            message    = "Parametric trigger approved. \u20b9$payoutAmount credited to ${req.workerId}. No claim was filed."
                        )
                    )
                } else {
                    call.respond(
                        DemoTriggerResponse(
                            triggered  = false,
                            fraudScore = fraudResp.fraud_score,
                            decision   = fraudResp.decision,
                            flags      = fraudResp.flags,
                            payoutInr  = null,
                            payoutRef  = null,
                            message    = "Trigger blocked by fraud engine. Decision: ${fraudResp.decision}."
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Demo trigger failed"))
                )
            }
        }
    }
}