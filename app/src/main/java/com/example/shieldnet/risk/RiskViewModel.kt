package com.example.shieldnet.risk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shieldnet.data.model.FraudApiResponse
import com.example.shieldnet.data.model.RiskRequest
import com.example.shieldnet.data.repository.ShieldNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiskScoreViewModel @Inject constructor(
    private val repo: ShieldNetRepository
) : ViewModel() {

    private val _riskData = MutableLiveData<FraudApiResponse?>()
    val riskData: LiveData<FraudApiResponse?> = _riskData

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadRiskScore() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val workerId = repo.workerId.first() ?: "unknown"
                val city     = repo.workerCity.first() ?: "Mumbai"

                val req = RiskRequest(
                    worker_id        = workerId,
                    city             = city,
                    event_type       = "weather",
                    policy_age_hours = 0.0,
                    severity         = 0.5
                )

                val result = repo.getRiskScore(req)
                _riskData.value = result

            } catch (e: Exception) {
                _error.value = "Could not fetch risk score: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}