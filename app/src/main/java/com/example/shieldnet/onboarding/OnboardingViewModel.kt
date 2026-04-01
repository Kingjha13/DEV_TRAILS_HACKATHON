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

    private val _otpSent = MutableLiveData<Boolean>()
    val otpSent: LiveData<Boolean> = _otpSent

    private val _authResult = MutableLiveData<OtpVerifyResponse?>()
    val authResult: LiveData<OtpVerifyResponse?> = _authResult

    private val _registerResult = MutableLiveData<RegisterResponse?>()
    val registerResult: LiveData<RegisterResponse?> = _registerResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun sendOtp(phone: String) {
        pendingPhone = phone

        _otpSent.value = true
    }


    fun verifyOtp(otp: String) {

        if (otp == "123456") {

            val fakeResponse = OtpVerifyResponse(
                token = "demo_token",
                workerId = "worker_${System.currentTimeMillis()}",
                isRegistered = false
            )

            viewModelScope.launch {
                repo.saveSession(
                    fakeResponse.token,
                    fakeResponse.workerId ?: "",
                    pendingPhone
                )

                _authResult.value = fakeResponse
            }

        } else {
            _error.value = "Invalid OTP (use 123456)"
        }
    }

    fun register(
        name: String,
        city: String,
        platform: String,
        weeklyAvg: Int,
        upiHandle: String
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {

                val req = RegisterRequest(
                    name = name,
                    phone = pendingPhone,
                    city = city,
                    platform = platform,
                    weeklyAvg = weeklyAvg,
                    upiHandle = upiHandle
                )

                val res = repo.registerWorker(req)

                repo.saveSession(res.token, res.id, pendingPhone)
                repo.saveCity(city)

                _registerResult.value = res

            } catch (e: Exception) {
                _error.value = "Registration failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}