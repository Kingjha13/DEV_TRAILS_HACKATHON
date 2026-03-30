package com.example.shieldnet.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.example.shieldnet.data.api.ShieldNetApi
import com.example.shieldnet.data.model.*
import com.example.shieldnet.di.TOKEN_KEY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val WORKER_ID_KEY = stringPreferencesKey("worker_id")
val WORKER_PHONE_KEY = stringPreferencesKey("worker_phone")
val WORKER_CITY_KEY = stringPreferencesKey("worker_city")

@Singleton
class ShieldNetRepository @Inject constructor(
    private val api: ShieldNetApi,
    private val dataStore: DataStore<Preferences>
) {

    val workerId: Flow<String?> = dataStore.data.map { it[WORKER_ID_KEY] }
    val workerPhone: Flow<String?> = dataStore.data.map { it[WORKER_PHONE_KEY] }
    val workerCity: Flow<String?> = dataStore.data.map { it[WORKER_CITY_KEY] }

    suspend fun saveSession(token: String, workerId: String, phone: String) {
        dataStore.edit {
            it[TOKEN_KEY] = token
            it[WORKER_ID_KEY] = workerId
            it[WORKER_PHONE_KEY] = phone
        }
    }

    suspend fun saveCity(city: String) {
        dataStore.edit { it[WORKER_CITY_KEY] = city }
    }

    suspend fun clearSession() {
        dataStore.edit { it.clear() }
    }

    suspend fun sendOtp(phone: String) {
        api.sendOtp(OtpRequest(phone))
    }

    suspend fun verifyOtp(phone: String, otp: String): OtpVerifyResponse {
        return api.verifyOtp(OtpVerifyRequest(phone, otp)).body()!!
    }

    suspend fun registerWorker(req: RegisterRequest): RegisterResponse {
        return api.registerWorker(req).body()!!
    }

    suspend fun getRiskScore(req: RiskRequest): RiskResponse {
        return api.getRiskScore(req).body()!!
    }

    suspend fun getActivePolicy(workerId: String): PolicyResponse? {
        return api.getActivePolicy(workerId).body()
    }

    suspend fun createPolicy(req: PolicyCreateRequest): PolicyResponse {
        return api.createPolicy(req).body()!!
    }

    suspend fun getClaims(workerId: String): List<ClaimResponse> {
        return api.getClaims(workerId).body() ?: emptyList()
    }

    suspend fun getTriggerStatus(city: String): TriggerStatusResponse {
        return api.getTriggerStatus(city).body()!!
    }
}