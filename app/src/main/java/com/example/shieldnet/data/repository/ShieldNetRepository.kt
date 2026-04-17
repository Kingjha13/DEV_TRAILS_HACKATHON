package com.example.shieldnet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.shieldnet.data.api.ShieldNetApi
import com.example.shieldnet.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShieldNetRepository @Inject constructor(
    private val api: ShieldNetApi,
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        val TOKEN_KEY        = stringPreferencesKey("auth_token")
        val WORKER_ID_KEY    = stringPreferencesKey("worker_id")
        val WORKER_PHONE_KEY = stringPreferencesKey("worker_phone")
        val WORKER_CITY_KEY  = stringPreferencesKey("worker_city")
        val PENDING_POLICY   = stringPreferencesKey("pending_policy")
        val LOCAL_CLAIMS = stringPreferencesKey("local_claims")
    }

    val workerId:    Flow<String?> = dataStore.data.map { it[WORKER_ID_KEY] }
    val workerPhone: Flow<String?> = dataStore.data.map { it[WORKER_PHONE_KEY] }
    val workerCity:  Flow<String?> = dataStore.data.map { it[WORKER_CITY_KEY] }

    suspend fun saveSession(token: String, workerId: String, phone: String) {
        dataStore.edit {
            it[TOKEN_KEY]        = token
            it[WORKER_ID_KEY]    = workerId
            it[WORKER_PHONE_KEY] = phone
        }
    }

    suspend fun saveCity(city: String) {
        dataStore.edit { it[WORKER_CITY_KEY] = city }
    }

    suspend fun saveLocalClaim(claim: String) {
        dataStore.edit {
            val existing = it[LOCAL_CLAIMS] ?: ""
            it[LOCAL_CLAIMS] = existing + "||" + claim
        }
    }
    suspend fun getLocalClaims(): List<LocalClaim> {
        val data = dataStore.data.first()[LOCAL_CLAIMS] ?: ""

        return data.split("||")
            .filter { it.isNotEmpty() }
            .map {
                val parts = it.split("|")
                LocalClaim(
                    eventType = parts[0],
                    amount = parts[1].toInt(),
                    status = parts[2],
                    date = parts[3]
                )
            }
    }
    suspend fun getSyncWorkerId(): String? =
        dataStore.data.map { it[WORKER_ID_KEY] }.first()

    suspend fun savePendingPolicy(tier: String) {
        dataStore.edit { it[PENDING_POLICY] = tier }
    }

    suspend fun getPendingPolicy(): String? =
        dataStore.data.map { it[PENDING_POLICY] }.first()

    suspend fun clearPendingPolicy() {
        dataStore.edit { it.remove(PENDING_POLICY) }
    }
    suspend fun sendOtp(phone: String): OtpSendResponse = withContext(Dispatchers.IO) {
        val res = api.sendOtp(OtpRequest(phone)).execute()
        res.body() ?: throw Exception("Server error sending OTP")
    }

    suspend fun verifyOtp(phone: String, otp: String): OtpVerifyResponse = withContext(Dispatchers.IO) {
        val res = api.verifyOtp(OtpVerifyRequest(phone, otp)).execute()
        res.body() ?: throw Exception("Invalid OTP")
    }

    suspend fun registerWorker(req: RegisterRequest): RegisterResponse = withContext(Dispatchers.IO) {
        val res = api.registerWorker(req).execute()
        if (res.isSuccessful) return@withContext res.body()!!
        throw Exception("Registration failed: ${res.code()}")
    }

    suspend fun getRiskScore(req: RiskRequest): FraudApiResponse = withContext(Dispatchers.IO) {
        val res = api.getRiskScore(req).execute()
        if (res.isSuccessful) return@withContext res.body()!!
        throw Exception("Risk analysis failed: ${res.code()}")
    }

    suspend fun createPolicy(req: PolicyCreateRequest): PolicyResponse = withContext(Dispatchers.IO) {
        val res = api.createPolicy(req).execute()
        if (res.isSuccessful) return@withContext res.body()!!
        throw Exception("Payment verification failed: ${res.code()}")
    }

    suspend fun getActivePolicy(workerId: String): PolicyResponse? = withContext(Dispatchers.IO) {
        val res = api.getActivePolicy(workerId).execute()
        when {
            res.isSuccessful  -> res.body()
            res.code() == 404 -> null
            else -> throw Exception("Failed to load policy: ${res.code()}")
        }
    }

    suspend fun getClaims(workerId: String): List<ClaimResponse> = withContext(Dispatchers.IO) {
        val res = api.getClaims(workerId).execute()
        if (res.isSuccessful) return@withContext res.body() ?: emptyList()
        throw Exception("Failed to load claims: ${res.code()}")
    }

    suspend fun getTriggerStatus(city: String): TriggerStatusResponse = withContext(Dispatchers.IO) {
        val res = api.getTriggerStatus(city).execute()
        if (res.isSuccessful) return@withContext res.body()!!
        throw Exception("Failed to load trigger status: ${res.code()}")
    }
}