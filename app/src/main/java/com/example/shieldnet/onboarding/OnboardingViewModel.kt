package com.example.shieldnet.onboarding

import androidx.lifecycle.*
import com.example.shieldnet.data.model.*
import com.example.shieldnet.data.repository.ShieldNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ShieldNetRepository
) : ViewModel() {

    private var pendingPhone: String = ""

    private val _otpSent = MutableLiveData<String?>()
    val otpSent: LiveData<String?> = _otpSent

    private val _authResult = MutableLiveData<OtpVerifyResponse?>()
    val authResult: LiveData<OtpVerifyResponse?> = _authResult

    private val _registerResult = MutableLiveData<RegisterResponse?>()
    val registerResult: LiveData<RegisterResponse?> = _registerResult

    private val _policyResult = MutableLiveData<PolicyResponse?>()
    val policyResult: LiveData<PolicyResponse?> = _policyResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    fun sendOtp(phone: String) {
        pendingPhone = phone
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = repo.sendOtp(phone)
                _otpSent.value = res.message
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to send OTP"
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = repo.verifyOtp(pendingPhone, otp)

                if (!res.workerId.isNullOrEmpty()) {
                    repo.saveSession(res.token, res.workerId, pendingPhone)
                    println("✅ OTP SAVED workerId = ${res.workerId}")
                }

                _authResult.value = res
            } catch (e: Exception) {
                _error.value = e.message ?: "Invalid OTP"
            } finally {
                _loading.value = false
            }
        }
    }


    fun register(name: String, city: String, platform: String, avg: Int, upi: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val res = repo.registerWorker(
                    RegisterRequest(name, pendingPhone, city, platform, avg, upi)
                )
                repo.saveSession(res.token, res.id, pendingPhone)
                println("✅ REGISTER SAVED workerId = ${res.id}")
                repo.saveCity(city)
                _registerResult.value = res
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _loading.value = false
            }
        }
    }


    fun confirmPayment(razorpayId: String, tier: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val id = repo.getSyncWorkerId() ?: throw Exception("Worker ID not found")
                val res = repo.createPolicy(
                    PolicyCreateRequest(id, tier, razorpayId, "ORD_${System.currentTimeMillis()}")
                )
                _policyResult.value = res
            } catch (e: Exception) {
                _error.value = e.message ?: "Payment confirmation failed"
            } finally {
                _loading.value = false
            }
        }
    }
}