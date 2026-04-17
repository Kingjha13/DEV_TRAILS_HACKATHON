package com.example.shieldnet.purchase

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shieldnet.data.model.PolicyCreateRequest
import com.example.shieldnet.data.model.PolicyResponse
import com.example.shieldnet.data.repository.ShieldNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun activatePolicy(paymentId: String, orderId: String, tier: String) {
        viewModelScope.launch {
            try {
                val workerId = repo.getSyncWorkerId()

                println("💳 PAYMENT USING workerId = [$workerId]")

                if (workerId.isNullOrEmpty()) {
                    _error.value = "Worker ID missing"
                    return@launch
                }

                val req = PolicyCreateRequest(
                    workerId = workerId,
                    planTier = tier,
                    razorpayPaymentId = paymentId,
                    razorpayOrderId = orderId
                )

                println("🚀 CALLING createPolicy API with workerId = $workerId")

                val res = repo.createPolicy(req)

                println("✅ POLICY RESPONSE RECEIVED: ${res.id}")


                val payout = when (tier.lowercase()) {
                    "premium" -> 500
                    "standard" -> 300
                    else -> 150
                }

                val events = listOf(
                    "Heavy Rainfall",
                    "Flood Alert",
                    "Heatwave",
                    "Traffic Disruption"
                )

                val event = events.random()

                val claimText =
                    "$event|$payout|paid|${System.currentTimeMillis()}"

                repo.saveLocalClaim(claimText)

                println("💰 LOCAL CLAIM CREATED: ₹$payout ($event)")


                _policyActivated.value = res

            } catch (e: Exception) {

                println("❌ API FAILED: ${e.message}")


                val payout = when (tier.lowercase()) {
                    "premium" -> 500
                    "standard" -> 300
                    else -> 150
                }

                val claimText =
                    "Offline Event|$payout|paid|${System.currentTimeMillis()}"

                repo.saveLocalClaim(claimText)

                println("💰 OFFLINE CLAIM CREATED: ₹$payout")

                _error.value = "Network issue — showing local payout"
            }
        }
    }
}