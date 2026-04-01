package com.example.shieldnet.risk

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shieldnet.R
import com.example.shieldnet.databinding.FragmentRiskScoreBinding
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RiskScoreFragment : Fragment(R.layout.fragment_risk_score) {

    private var _binding: FragmentRiskScoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RiskScoreViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRiskScoreBinding.bind(view)

        binding.progressLoading.postDelayed({
            if (binding.progressLoading.visibility == View.VISIBLE) {
                showFallbackUI()
            }
        }, 4000)

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
            binding.contentGroup.visibility    = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.riskData.observe(viewLifecycleOwner) { risk ->
            risk ?: return@observe

            val score = (risk.fraud_score * 100).toInt().coerceIn(0, 100)
            binding.tvScoreNumber.text = score.toString()
            binding.progressRisk.progress = score

            val (level, premium, coverage, tier) = when {
                score < 35 -> listOf("LOW RISK", "₹49", "₹15,000", "low")
                score < 65 -> listOf("MEDIUM RISK", "₹99", "₹10,000", "medium")
                else       -> listOf("HIGH RISK", "₹149", "₹7,000", "high")
            }

            val premiumInt = premium.replace("₹", "").toIntOrNull() ?: 99
            val coverageInt = coverage.replace("₹", "").replace(",", "").toIntOrNull() ?: 10000

            binding.tvRiskLevel.text = level
            binding.tvPremium.text   = "$premium / week"
            binding.tvCoverage.text  = "Coverage up to $coverage"

            setupRadarChart(risk.fraud_score.toFloat())

            binding.btnBuyPolicy.setOnClickListener {
                val action = RiskScoreFragmentDirections
                    .actionRiskScoreToPurchase(premiumInt, coverageInt, tier as String)
                findNavController().navigate(action)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            showFallbackUI()
        }

        viewModel.loadRiskScore()
    }

    private fun showFallbackUI() {
        binding.progressLoading.visibility = View.GONE
        binding.contentGroup.visibility    = View.VISIBLE

        binding.tvScoreNumber.text   = "52"
        binding.progressRisk.progress = 52
        binding.tvRiskLevel.text     = "MEDIUM RISK"
        binding.tvPremium.text       = "₹99 / week"
        binding.tvCoverage.text      = "Coverage up to ₹10,000"

        setupRadarChart(0.52f)

        binding.btnBuyPolicy.setOnClickListener {
            val action = RiskScoreFragmentDirections
                .actionRiskScoreToPurchase(99, 10000, "medium")
            findNavController().navigate(action)
        }
    }

    private fun setupRadarChart(score: Float) {
        try {
            val labels = listOf("Rainfall", "Flood", "AQI", "Traffic")
            val entries = listOf(
                RadarEntry(score * 80f + 10f),
                RadarEntry(score * 60f + 15f),
                RadarEntry(score * 70f + 20f),
                RadarEntry(score * 50f + 25f)
            )

            val dataSet = RadarDataSet(entries, "Risk Factors").apply {
                color = 0xFF00E5CC.toInt()
                fillColor = 0xFF00E5CC.toInt()
                setDrawFilled(true)
                fillAlpha = 60
                lineWidth = 2f
                setDrawHighlightCircleEnabled(true)
            }

            binding.radarChart.apply {
                data = RadarData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.textColor = 0xFF8892A4.toInt()
                yAxis.textColor = 0xFF8892A4.toInt()
                yAxis.setDrawLabels(false)
                legend.isEnabled = false
                description.isEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                invalidate()
            }
        } catch (e: Exception) {
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}