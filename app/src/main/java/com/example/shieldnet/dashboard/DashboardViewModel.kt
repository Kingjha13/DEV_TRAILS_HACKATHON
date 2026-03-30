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
            try {
                val workerId = repo.workerId.first() ?: return@launch
                val city = repo.workerCity.first() ?: "Mumbai"

                val policy = repo.getActivePolicy(workerId)
                val trigger = repo.getTriggerStatus(city)
                val claims = repo.getClaims(workerId)

                _activePolicy.value = policy
                _triggerStatus.value = trigger
                _claims.value = claims

            } catch (e: Exception) {
                _error.value = "Failed to load dashboard: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}