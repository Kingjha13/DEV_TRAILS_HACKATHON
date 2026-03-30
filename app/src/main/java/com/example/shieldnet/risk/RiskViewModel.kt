package com.example.shieldnet.risk

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shieldnet.data.model.RiskRequest
import com.example.shieldnet.data.model.RiskResponse
import com.example.shieldnet.data.repository.ShieldNetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiskViewModel @Inject constructor(
    private val repo: ShieldNetRepository
) : ViewModel() {

    private val _riskData = MutableLiveData<RiskResponse>()
    val riskData: LiveData<RiskResponse> = _riskData

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadRiskScore(lat: Double = 19.0760, lon: Double = 72.8777) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val city = repo.workerCity.first() ?: "Mumbai"

                val req = RiskRequest(
                    city = city,
                    platform = "swiggy",
                    deliveryZone = "central",
                    lat = lat,
                    lon = lon
                )

                val res = repo.getRiskScore(req)

                _riskData.value = res

            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
