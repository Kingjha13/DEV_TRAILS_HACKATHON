package com.example.shieldnet.dashboard

import androidx.lifecycle.*
import com.example.shieldnet.data.model.*
import com.example.shieldnet.data.repository.ShieldNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: ShieldNetRepository
) : ViewModel() {

    private val _activePolicy = MutableLiveData<PolicyResponse?>()
    val activePolicy: LiveData<PolicyResponse?> = _activePolicy

    private val _triggerStatus = MutableLiveData<TriggerStatusResponse>()
    val triggerStatus: LiveData<TriggerStatusResponse> = _triggerStatus

    private val _claims = MutableLiveData<List<ClaimResponse>>()
    val claims: LiveData<List<ClaimResponse>> = _claims

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadDashboard() {
        viewModelScope.launch {
            _loading.value = true
            _error.value   = null

            try {
                val workerId = repo.workerId.first()
                    ?: run {
                        _loading.value = false
                        return@launch
                    }
                val city = repo.workerCity.first() ?: "Mumbai"

                val policy     = runCatching { repo.getActivePolicy(workerId) }.getOrNull()
                val trigger    = runCatching { repo.getTriggerStatus(city) }.getOrElse {
                    TriggerStatusResponse(city, emptyList(), true)
                }
                val claimsList = repo.getLocalClaims().map {
                    ClaimResponse(
                        id = System.currentTimeMillis().toString(),
                        eventType = it.eventType,
                        estimatedLoss = it.amount,
                        approvedAmount = it.amount,
                        status = it.status,
                        createdAt = it.date,
                        payoutRef = null
                    )
                }

                _activePolicy.value  = policy
                _triggerStatus.value = trigger
                _claims.value        = claimsList

                println("📊 [DASHBOARD] policy=${policy?.id} " +
                        "claims=${claimsList.size} " +
                        "trigger=${trigger.city} allClear=${trigger.allClear}")

            } catch (e: Exception) {
                val pendingTier = repo.getPendingPolicy()
                if (pendingTier != null) {
                    val premium  = when (pendingTier.lowercase()) { "premium" -> 199; "standard" -> 149; else -> 99 }
                    val coverage = when (pendingTier.lowercase()) { "premium" -> 50000; "standard" -> 25000; else -> 10000 }

                    _activePolicy.value = PolicyResponse(
                        id          = "pol_offline",
                        planTier    = pendingTier,
                        premiumInr  = premium,
                        coverageInr = coverage,
                        startsAt    = "Pending Sync",
                        expiresAt   = "Pending Sync",
                        status      = "OFFLINE"
                    )
                    _triggerStatus.value = TriggerStatusResponse("Offline", emptyList(), true)
                    _claims.value        = emptyList()
                    _error.value         = "Offline Mode — data will sync when network is restored."
                } else {
                    _error.value = "Could not load dashboard: ${e.message}"
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun refreshAfterPayment() {
        loadDashboard()
    }
}