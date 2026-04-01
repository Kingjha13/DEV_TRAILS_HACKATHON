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

    suspend fun sendOtp(phone: String):
            OtpSendResponse{
        return api.sendOtp(OtpRequest(phone)).body()!!
    }

    suspend fun verifyOtp(phone: String, otp: String): OtpVerifyResponse {
        return api.verifyOtp(OtpVerifyRequest(phone, otp)).body()!!
    }

    suspend fun registerWorker(req: RegisterRequest): RegisterResponse {
        val res = api.registerWorker(req)

        if (res.isSuccessful && res.body() != null) {
            return res.body()!!
        } else {
            throw Exception("Register failed: ${res.code()}")
        }
    }
    suspend fun saveUser(workerId: String, phone: String, city: String) {
        dataStore.edit {
            it[WORKER_ID_KEY] = workerId
            it[WORKER_PHONE_KEY] = phone
            it[WORKER_CITY_KEY] = city
        }
    }

    suspend fun getRiskScore(req: RiskRequest): FraudApiResponse {

        val response = api.getRiskScore(req)

        if (response.isSuccessful && response.body() != null) {
            return response.body()!!
        } else {
            throw Exception("API Error: ${response.code()} - ${response.errorBody()?.string()}")
        }
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