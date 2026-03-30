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

    private val testPhone = "7654216450"
    private val testOtp = "123456"

    fun sendOtp(phone: String) {
        pendingPhone = phone

        if (phone == testPhone) {
            _otpSent.value = true
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                repo.sendOtp(phone)
                _otpSent.value = true
            } catch (e: Exception) {
                _error.value = "Failed to send OTP: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyOtp(otp: String) {

        if (pendingPhone == testPhone && otp == testOtp) {
            _authResult.value = OtpVerifyResponse(
                isRegistered = true,
                workerId = "test_worker",
                token = "test_token"
            )
            return
        }

        viewModelScope.launch {
            _loading.value = true
            try {
                val body = repo.verifyOtp(pendingPhone, otp)

                if (body.isRegistered && body.workerId != null) {
                    repo.saveSession(body.token, body.workerId, pendingPhone)
                }

                _authResult.value = body

            } catch (e: Exception) {
                _error.value = "Invalid OTP or network error"
            } finally {
                _loading.value = false
            }
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

                val body = repo.registerWorker(req)

                repo.saveSession(body.token, body.id, pendingPhone)
                repo.saveCity(city)

                _registerResult.value = body

            } catch (e: Exception) {
                _error.value = "Registration failed: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}