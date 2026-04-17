package com.example

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
@kotlinx.serialization.Serializable
data class SimpleResponse(
    val success: Boolean,
    val worker_id: String
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
    @SerialName("razorpay_payment_id") val razorpayPaymentId: String? = null,
    @SerialName("razorpay_order_id") val razorpayOrderId: String? = null
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

@Serializable data class StatusResponse(val status: String, val cloud: Boolean)

@Serializable data class RiskRequest(
    val worker_id: String,
    val city: String,
    val event_type: String,
    val policy_age_hours: Double,
    val severity: Double
)
@Serializable data class FraudApiResponse(val fraud_score: Double, val risk_label: String)


object StorageHandler {
    private val uri = System.getenv("MONGO_URI")?.takeIf { it.isNotBlank() } ?: "mongodb://localhost:27017/shieldnet"
    private val client = try { MongoClients.create(uri) } catch (e: Exception) {
        println("⚠️ [MONGO] Could not connect: ${e.message}")
        null
    }
    val db: MongoDatabase? = try { client?.getDatabase("shieldnet") } catch (e: Exception) { null }

    val workersInMem   = ConcurrentHashMap<String, Document>()
    val policiesInMem  = ConcurrentHashMap<String, Document>()
    val claimsInMem    = ConcurrentHashMap<String, Document>()
    val otpStore       = ConcurrentHashMap<String, String>()

    fun now(): String     = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    fun expires(): String = DateTimeFormatter.ISO_INSTANT.format(
        Instant.now().plus(java.time.Duration.ofDays(30))
    )

    fun insertClaim(doc: Document) {
        val claimId = doc.getString("claim_id")
            ?: error("claim_id missing in document")
        claimsInMem[claimId] = doc
        println("✅ [IN-MEM] Claim stored -> $claimId (total in-mem: ${claimsInMem.size})")

        try {
            db?.getCollection("claims")?.insertOne(doc)
            println("✅ [MONGO] Claim stored -> $claimId")
        } catch (e: Exception) {
            println("⚠️ [MONGO] Claim write failed (in-mem fallback active): ${e.message}")
        }
    }

    fun insertPolicy(doc: Document) {
        val policyId = doc.getString("policy_id") ?: error("policy_id missing")
        policiesInMem[policyId] = doc
        try {
            db?.getCollection("policies")?.insertOne(doc)
            println("✅ [MONGO] Policy stored -> $policyId")
        } catch (e: Exception) {
            println("⚠️ [MONGO] Policy write failed (in-mem fallback active): ${e.message}")
        }
    }

    fun insertWorker(doc: Document) {
        val workerId = doc.getString("worker_id") ?: error("worker_id missing")
        workersInMem[workerId] = doc
        try {
            db?.getCollection("workers")?.insertOne(doc)
            println("✅ [MONGO] Worker stored -> $workerId")
        } catch (e: Exception) {
            println("⚠️ [MONGO] Worker write failed (in-mem fallback active): ${e.message}")
        }
    }
    fun getClaimsForWorker(workerId: String): List<Document> {
        val mongoDocs = try {
            db?.getCollection("claims")
                ?.find(Document("worker_id", workerId))
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) {
            println("⚠️ [MONGO] Claims read failed: ${e.message}")
            emptyList()
        }

        val memDocs = claimsInMem.values.filter { it.getString("worker_id") == workerId }

        val merged = LinkedHashMap<String, Document>()
        mongoDocs.forEach { merged[it.getString("claim_id") ?: it.getObjectId("_id").toString()] = it }
        memDocs.forEach  { merged.putIfAbsent(it.getString("claim_id") ?: "", it) }

        println("📋 [CLAIMS] Worker $workerId → ${merged.size} claim(s) " +
                "(mongo=${mongoDocs.size}, mem=${memDocs.size})")
        return merged.values.toList()
    }

    fun getActivePolicyForWorker(workerId: String): Document? {
        val mongoPolicies = try {
            db?.getCollection("policies")
                ?.find(Document("worker_id", workerId))
                ?.toList()
                ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val memPolicies = policiesInMem.values.filter { it.getString("worker_id") == workerId }
        val all = (mongoPolicies + memPolicies).distinctBy { it.getString("policy_id") }
        return all.filter { it.getString("status") == "active" }
            .maxByOrNull { it.getString("created_at") ?: "" }
    }

    fun findWorkerByPhone(phone: String): Document? {
        val fromMongo = try {
            db?.getCollection("workers")
                ?.find(Document("phone", phone))
                ?.first()
        } catch (e: Exception) { null }
        return fromMongo ?: workersInMem.values.find { it.getString("phone") == phone }
    }
}


fun approvedAmountForPlan(planTier: String): Int = when (planTier.lowercase().trim()) {
    "basic"    -> 1000
    "standard" -> 2500
    "premium"  -> 5000
    else       -> 1000
}

fun premiumForPlan(planTier: String): Int = when (planTier.lowercase().trim()) {
    "basic"    -> 99
    "standard" -> 149
    "premium"  -> 199
    else       -> 99
}

fun coverageForPlan(planTier: String): Int = when (planTier.lowercase().trim()) {
    "basic"    -> 10000
    "standard" -> 25000
    "premium"  -> 50000
    else       -> 10000
}


fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    val jsonConfig = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    install(ContentNegotiation) { json(jsonConfig) }

    routing {

        get("/health") { call.respond(StatusResponse("online", StorageHandler.db != null)) }
        get("/")       { call.respond(StatusResponse("online", StorageHandler.db != null)) }

        post("/auth/send-otp") {
            try {
                val req = call.receive<OtpRequest>()
                StorageHandler.otpStore[req.phone] = "123456"
                call.respond(OtpSendResponse("OTP sent successfully. [DEV: 123456]", true))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("/auth/verify-otp") {
            try {
                val req = call.receive<OtpVerifyRequest>()
                val existingWorker = StorageHandler.findWorkerByPhone(req.phone)
                call.respond(OtpVerifyResponse(
                    token      = "tk_${System.currentTimeMillis()}",
                    workerId   = existingWorker?.getString("worker_id"),
                    isRegistered = existingWorker != null
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("/workers/register") {
            try {
                val req = call.receive<RegisterRequest>()

                val existing = StorageHandler.findWorkerByPhone(req.phone)

                if (existing != null) {
                    val existingId = existing.getString("worker_id")

                    println("⚠️ Reusing existing worker: $existingId")

                    call.respond(RegisterResponse(
                        id = existingId,
                        name = existing.getString("name"),
                        phone = existing.getString("phone"),
                        riskScore = null,
                        token = "tk_$existingId"
                    ))
                    return@post
                }

                val workerId = "worker_${System.currentTimeMillis()}"

                val doc = Document()
                    .append("worker_id", workerId)
                    .append("name", req.name)
                    .append("phone", req.phone)
                    .append("city", req.city)
                    .append("platform", req.platform)
                    .append("upi_handle", req.upiHandle)
                    .append("weekly_avg", req.weeklyAvg)
                    .append("created_at", StorageHandler.now())

                StorageHandler.insertWorker(doc)

                println("🚀 [WORKER REGISTERED] $workerId")

                call.respond(RegisterResponse(
                    id = workerId,
                    name = req.name,
                    phone = req.phone,
                    riskScore = null,
                    token = "tk_$workerId"
                ))

            } catch (e: Exception) {
                println("❌ [REGISTER ERROR] ${e.message}")
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        post("/risk/analyze") {
            try {
                val req = call.receive<RiskRequest>()
                val baseScore = when (req.city.lowercase()) {
                    "mumbai"    -> Random.nextDouble(0.45, 0.80)
                    "delhi"     -> Random.nextDouble(0.50, 0.85)
                    "bangalore" -> Random.nextDouble(0.30, 0.65)
                    "pune"      -> Random.nextDouble(0.35, 0.70)
                    else        -> Random.nextDouble(0.40, 0.75)
                }
                val label = when {
                    baseScore < 0.35 -> "LOW"
                    baseScore < 0.65 -> "MEDIUM"
                    else             -> "HIGH"
                }
                println("📊 [RISK] Worker=${req.worker_id} City=${req.city} Score=${"%.2f".format(baseScore)} Label=$label")
                call.respond(FraudApiResponse(baseScore, label))
            } catch (e: Exception) {
                // Fallback: return a medium risk score
                call.respond(FraudApiResponse(0.52, "MEDIUM"))
            }
        }

        post("/policies/create") {
            try {
                println("🔥 POLICY API HIT")

                val req = call.receive<PolicyCreateRequest>()
                println("🔥 RECEIVED workerId = ${req.workerId}")

                val policyId = "pol_${System.currentTimeMillis()}"
                val claimId  = "clm_${System.currentTimeMillis()}"

                val pDoc = Document()
                    .append("policy_id", policyId)
                    .append("worker_id", req.workerId)
                    .append("plan_tier", req.planTier)
                    .append("created_at", StorageHandler.now())

                StorageHandler.insertPolicy(pDoc)
                println("🛡️ POLICY SAVED")


                val cDoc = Document()
                    .append("claim_id", claimId)
                    .append("worker_id", req.workerId)
                    .append("approved_amount", 999)
                    .append("status", "paid")
                    .append("created_at", StorageHandler.now())

                StorageHandler.insertClaim(cDoc)
                println("⚡ CLAIM INSERTED")

                call.respond(
                    PolicyResponse(
                        id = policyId,
                        planTier = req.planTier,
                        premiumInr = 100,
                        coverageInr = 10000,
                        startsAt = StorageHandler.now(),
                        expiresAt = StorageHandler.expires(),
                        status = "active"
                    )
                )

            } catch (e: Exception) {
                println("❌ ERROR: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }
        get("/policies/active") {
            try {
                val workerId = call.request.queryParameters["worker_id"] ?: ""
                if (workerId.isEmpty()) { call.respond(HttpStatusCode.BadRequest); return@get }

                val doc = StorageHandler.getActivePolicyForWorker(workerId)
                if (doc != null) {
                    call.respond(PolicyResponse(
                        id          = doc.getString("policy_id") ?: "",
                        planTier    = doc.getString("plan_tier") ?: "standard",
                        premiumInr  = doc.getInteger("premium") ?: 149,
                        coverageInr = doc.getInteger("coverage") ?: 25000,
                        startsAt    = doc.getString("created_at") ?: StorageHandler.now(),
                        expiresAt   = doc.getString("expires_at") ?: StorageHandler.expires(),
                        status      = "active"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/claims/list") {
            try {
                val workerId = call.request.queryParameters["worker_id"] ?: ""

                println("📥 FETCH CLAIMS for $workerId")

                if (workerId.isEmpty()) {
                    call.respond(emptyList<ClaimResponse>())
                    return@get
                }

                val docs = StorageHandler.getClaimsForWorker(workerId)

                val response = docs.map {
                    ClaimResponse(
                        id = it.getString("claim_id") ?: "",
                        eventType = it.getString("event_type") ?: "test_event",
                        estimatedLoss = it.getInteger("estimated_loss") ?: 0,
                        approvedAmount = it.getInteger("approved_amount"),
                        status = it.getString("status") ?: "paid",
                        createdAt = it.getString("created_at") ?: StorageHandler.now(),
                        payoutRef = it.getString("payout_ref")
                    )
                }

                println("📦 FOUND ${response.size} CLAIMS")

                call.respond(response)

            } catch (e: Exception) {
                println("❌ FETCH ERROR: ${e.message}")
                call.respond(emptyList<ClaimResponse>())
            }
        }
        get("/test/create-claim") {
            val workerId = call.request.queryParameters["worker_id"] ?: "test_worker"

            val claimId = "test_${System.currentTimeMillis()}"

            val cDoc = Document()
                .append("claim_id", claimId)
                .append("worker_id", workerId)
                .append("approved_amount", 1111)
                .append("status", "paid")
                .append("created_at", StorageHandler.now())

            StorageHandler.insertClaim(cDoc)

            println("🧪 TEST CLAIM CREATED for $workerId")

            call.respond(SimpleResponse(true, workerId))
        }

        get("/status/triggers") {
            val city = call.request.queryParameters["city"] ?: "Mumbai"
            val severity = when (city.lowercase()) {
                "mumbai"    -> 0.70f + Random.nextFloat() * 0.20f
                "delhi"     -> 0.60f + Random.nextFloat() * 0.25f
                "bangalore" -> 0.40f + Random.nextFloat() * 0.20f
                "pune"      -> 0.45f + Random.nextFloat() * 0.20f
                else        -> 0.50f + Random.nextFloat() * 0.20f
            }
            val breached = severity > 0.70f
            call.respond(TriggerStatusResponse(
                city = city,
                activeTriggers = listOf(
                    ActiveTrigger(
                        type             = "Heavy_Rainfall",
                        severity         = severity,
                        description      = "Precipitation monitoring active — $city",
                        threshold        = 0.70f,
                        thresholdBreached = breached,
                        detectedAt       = StorageHandler.now()
                    )
                ),
                allClear = !breached
            ))
        }
    }
}