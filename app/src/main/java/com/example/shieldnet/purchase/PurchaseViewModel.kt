package com.example.shieldnet.purchase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shieldnet.data.model.PolicyCreateRequest
import com.example.shieldnet.data.model.PolicyResponse
import com.example.shieldnet.data.repository.ShieldNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val repo: ShieldNetRepository
) : ViewModel() {

    private val _policyActivated = MutableLiveData<PolicyResponse?>()
    val policyActivated: LiveData<PolicyResponse?> = _policyActivated

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun getWorkerPhone(): String = ""

    fun activatePolicy(paymentId: String, orderId: String, tier: String) {
        viewModelScope.launch {
            try {
                val workerId = repo.workerId.first() ?: return@launch

                val req = PolicyCreateRequest(
                    workerId = workerId,
                    planTier = tier,
                    razorpayPaymentId = paymentId,
                    razorpayOrderId = orderId
                )

                val res = repo.createPolicy(req)

                _policyActivated.value = res

            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            }
        }
    }
}
