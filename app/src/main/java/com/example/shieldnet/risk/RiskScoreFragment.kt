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

        // 🔥 Start loading
        binding.tvScoreNumber.text = "--"
        binding.tvRiskLevel.text = "Analyzing..."

        viewModel.loadRiskScore()

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.riskData.observe(viewLifecycleOwner) { risk: RiskResponse ->

            val scorePercent = (risk.riskScore * 100).toInt()

            // ✅ FIXED UI
            binding.tvScoreNumber.text = "$scorePercent%"
            binding.tvRiskLevel.text = risk.riskLevel.uppercase()

            // ✅ COLOR LOGIC
            when (risk.riskLevel.lowercase()) {
                "approve" -> binding.tvRiskLevel.setTextColor(requireContext().getColor(R.color.green))
                "review" -> binding.tvRiskLevel.setTextColor(requireContext().getColor(R.color.orange))
                "reject" -> binding.tvRiskLevel.setTextColor(requireContext().getColor(R.color.red))
            }

            setupRadarChart(risk)
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()

                // 🔥 fallback UI
                binding.tvRiskLevel.text = "Error"
                binding.tvScoreNumber.text = "--"
            }
        }
    }

    private fun setupRadarChart(risk: RiskResponse) {

        val entries = listOf(
            RadarEntry(risk.factors.rainfallRisk * 5),
            RadarEntry(risk.factors.floodFreq * 5),
            RadarEntry(risk.factors.aqiRisk * 5),
            RadarEntry(risk.factors.congestionIndex * 5)
        )

        val dataSet = RadarDataSet(entries, "Risk Factors").apply {
            lineWidth = 2f
            valueTextSize = 10f
        }

        binding.radarChart.apply {
            data = RadarData(dataSet)

            description.isEnabled = false
            legend.isEnabled = false

            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}