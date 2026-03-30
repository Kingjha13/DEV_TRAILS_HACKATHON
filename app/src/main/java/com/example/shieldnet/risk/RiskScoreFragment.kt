package com.example.shieldnet.risk

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shieldnet.R
import com.example.shieldnet.data.model.RiskResponse
import com.example.shieldnet.databinding.FragmentRiskScoreBinding
import com.github.mikephil.charting.data.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RiskScoreFragment : Fragment(R.layout.fragment_risk_score) {

    private var _binding: FragmentRiskScoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RiskViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRiskScoreBinding.bind(view)

        viewModel.loadRiskScore()

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.riskData.observe(viewLifecycleOwner) { risk: RiskResponse ->
            val scorePercent = (risk.riskScore * 100).toInt()

            binding.tvScoreNumber.text = "$scorePercent"
            binding.tvRiskLevel.text = risk.riskLevel.uppercase()

            setupRadarChart(risk)
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun setupRadarChart(risk: RiskResponse) {
        val entries = listOf(
            RadarEntry(risk.factors.rainfallRisk * 5),
            RadarEntry(risk.factors.floodFreq * 5),
            RadarEntry(risk.factors.aqiRisk * 5),
            RadarEntry(risk.factors.congestionIndex)
        )

        val dataSet = RadarDataSet(entries, "Risk Factors")

        binding.radarChart.apply {
            data = RadarData(dataSet)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}