package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
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
import org.bson.Document
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import com.mongodb.client.model.Sorts


@Serializable data class OtpRequest(val phone: String)
@Serializable data class OtpSendResponse(val message: String, val success: Boolean)
@Serializable data class OtpVerifyRequest(val phone: String, val otp: String)
@Serializable data class OtpVerifyResponse(
    val token: String,
    @SerialName("worker_id") val workerId: String?,
    @SerialName("is_registered") val isRegistered: Boolean
)

@Serializable data class RegisterRequest(
    val name: String,
    val phone: String,
    val city: String,
    val platform: String,
    @SerialName("weekly_avg") val weeklyAvg: Int,
    @SerialName("upi_handle") val upiHandle: String
)
@Serializable data class RegisterResponse(
    val id: String,
    val name: String,
    val phone: String,
    @SerialName("risk_score") val riskScore: Float?,
    val token: String
)

@Serializable data class PolicyCreateRequest(
    @SerialName("worker_id") val workerId: String,
    @SerialName("plan_tier") val planTier: String,
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerialName("razorpay_order_id") val razorpayOrderId: String
)
@Serializable data class PolicyResponse(
    val id: String,
    @SerialName("plan_tier") val planTier: String,
    @SerialName("premium_inr") val premiumInr: Int,
    @SerialName("coverage_inr") val coverageInr: Int,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("expires_at") val expiresAt: String,
    val status: String
)

@Serializable data class ClaimResponse(
    val id: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("estimated_loss") val estimatedLoss: Int,
    @SerialName("approved_amount") val approvedAmount: Int?,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("payout_ref") val payoutRef: String?
)

@Serializable data class TriggerStatusResponse(
    val city: String,
    @SerialName("active_triggers") val activeTriggers: List<ActiveTrigger>,
    @SerialName("all_clear") val allClear: Boolean
)
@Serializable data class ActiveTrigger(
    val type: String,
    val severity: Float,
    val description: String,
    val threshold: Float,
    @SerialName("threshold_breached") val thresholdBreached: Boolean,
    @SerialName("detected_at") val detectedAt: String
)

@Serializable data class StatusResponse(
    val status: String,
    val cloud: Boolean
)

object ShieldNetEngine {
    private val uri = "mongodb+srv://kingjha:Avanishsupriya@cluster0.bc3gakw.mongodb.net/shieldnet?retryWrites=true&w=majority"
    private val client = try { MongoClients.create(uri) } catch (e: Exception) { null }
    val db: MongoDatabase? = client?.getDatabase("shieldnet")

    val workersInMem = ConcurrentHashMap<String, Document>()
    val policiesInMem = ConcurrentHashMap<String, Document>()
    val claimsInMem = ConcurrentHashMap<String, Document>()
    val otpStore = ConcurrentHashMap<String, String>()

    fun now(): String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val jsonConfig = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    install(ContentNegotiation) { json(jsonConfig) }

    routing {

        get("/") {
            call.respond(StatusResponse("online", ShieldNetEngine.db != null))
        }

        post("/auth/send-otp") {
            try {
                val req = call.receive<OtpRequest>()
                ShieldNetEngine.otpStore[req.phone] = "123456"
                call.respond(OtpSendResponse("OTP sent successfully. [DEV: 123456]", true))
            } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest) }
        }

        post("/auth/verify-otp") {
            try {
                val req = call.receive<OtpVerifyRequest>()
                val mongoDoc = try { ShieldNetEngine.db?.getCollection("workers")?.find(Document("phone", req.phone))?.firstOrNull() } catch (e: Exception) { null }
                val finalDoc = mongoDoc ?: ShieldNetEngine.workersInMem.values.find { it.getString("phone") == req.phone }

                call.respond(OtpVerifyResponse(
                    token = "tk_${System.currentTimeMillis()}",
                    workerId = finalDoc?.getString("worker_id"),
                    isRegistered = finalDoc != null
                ))
            } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest) }
        }

        post("/workers/register") {
            try {
                val req = call.receive<RegisterRequest>()
                val workerId = "worker_${System.currentTimeMillis()}"
                val doc = Document()
                    .append("worker_id", workerId).append("name", req.name)
                    .append("phone", req.phone).append("city", req.city)
                    .append("platform", req.platform).append("created_at", ShieldNetEngine.now())

                try { ShieldNetEngine.db?.getCollection("workers")?.insertOne(doc) } catch (e: Exception) {}
                ShieldNetEngine.workersInMem[workerId] = doc

                call.respond(RegisterResponse(workerId, req.name, req.phone, 0.15f, "tk_$workerId"))
            } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest) }
        }

        post("/policies/create") {
            try {
                val req = call.receive<PolicyCreateRequest>()
                val policyId = "pol_${System.currentTimeMillis()}"

                val pDoc = Document()
                    .append("policy_id", policyId).append("worker_id", req.workerId)
                    .append("status", "active").append("created_at", ShieldNetEngine.now())

                try { ShieldNetEngine.db?.getCollection("policies")?.insertOne(pDoc) } catch (e: Exception) {}
                ShieldNetEngine.policiesInMem[policyId] = pDoc

                val claimId = "clm_${System.currentTimeMillis()}"
                val cDoc = Document()
                    .append("claim_id", claimId).append("worker_id", req.workerId)
                    .append("event_type", "Heavy Rainfall").append("approved_amount", 1200)
                    .append("status", "paid").append("created_at", ShieldNetEngine.now())
                    .append("payout_ref", "TXN_${Random.nextInt(10000, 99999)}")

                try { ShieldNetEngine.db?.getCollection("claims")?.insertOne(cDoc) } catch (e: Exception) {}
                ShieldNetEngine.claimsInMem[claimId] = cDoc
                println("🚀 [SUCCESS] Payout Event Created")

                call.respond(PolicyResponse(
                    id = policyId, planTier = req.planTier, premiumInr = 199, coverageInr = 25000,
                    startsAt = ShieldNetEngine.now(), expiresAt = "2026-05-16", status = "active"
                ))
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError) }
        }

        get("/claims/list") {
            try {
                val workerId = call.request.queryParameters["worker_id"] ?: ""
                val cloudData = try { ShieldNetEngine.db?.getCollection("claims")?.find(Document("worker_id", workerId))?.toList() ?: emptyList() } catch (e: Exception) { emptyList<Document>() }
                val memoryData = ShieldNetEngine.claimsInMem.values.filter { it.getString("worker_id") == workerId }
                val combined = (cloudData + memoryData).distinctBy { it.getString("claim_id") }

                val finalResults = combined.map { doc ->
                    ClaimResponse(
                        id = doc.getString("claim_id") ?: "clm_0",
                        eventType = doc.getString("event_type") ?: "Rainfall Event",
                        estimatedLoss = 1500,
                        approvedAmount = doc.getInteger("approved_amount") ?: 1200,
                        status = doc.getString("status") ?: "paid",
                        createdAt = doc.getString("created_at") ?: ShieldNetEngine.now(),
                        payoutRef = doc.getString("payout_ref") ?: "TXN_OK"
                    )
                }.sortedByDescending { it.createdAt }

                call.respond(finalResults)
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError) }
        }

        get("/policies/active") {
            try {
                val workerId = call.request.queryParameters["worker_id"] ?: ""
                val mongoDoc = try { ShieldNetEngine.db?.getCollection("policies")?.find(Document("worker_id", workerId).append("status", "active"))?.firstOrNull() } catch (e: Exception) { null }
                val finalDoc = mongoDoc ?: ShieldNetEngine.policiesInMem.values.find { it.getString("worker_id") == workerId && it.getString("status") == "active" }

                if (finalDoc != null) {
                    call.respond(PolicyResponse(
                        id = finalDoc.getString("policy_id") ?: "", planTier = "Standard",
                        premiumInr = 199, coverageInr = 25000,
                        startsAt = finalDoc.getString("created_at") ?: ShieldNetEngine.now(),
                        expiresAt = "2026-05-16", status = "active"
                    ))
                } else { call.respond(HttpStatusCode.NotFound) }
            } catch (e: Exception) { call.respond(HttpStatusCode.InternalServerError) }
        }

        get("/status/triggers") {
            call.respond(TriggerStatusResponse("Mumbai", listOf(
                ActiveTrigger("Rainfall", 0.85f, "Heavy rainfall detected", 0.70f, true, ShieldNetEngine.now())
            ), false))
        }
    }
}
