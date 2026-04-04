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

        view.postDelayed({
            _binding?.let { safeBinding ->
                if (safeBinding.progressLoading.visibility == View.VISIBLE) {
                    showFallbackUI()
                }
            }
        }, 4000)

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            _binding?.let { b ->
                b.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
                b.contentGroup.visibility    = if (loading) View.GONE else View.VISIBLE
            }
        }

        viewModel.riskData.observe(viewLifecycleOwner) { risk ->
            risk ?: return@observe
            val b = _binding ?: return@observe

            val score = (risk.fraud_score * 100).toInt().coerceIn(0, 100)
            b.tvScoreNumber.text = score.toString()
            b.progressRisk.progress = score

            val (level, premium, coverage, tier) = when {
                score < 35 -> listOf("LOW RISK", "₹49", "₹15,000", "low")
                score < 65 -> listOf("MEDIUM RISK", "₹99", "₹10,000", "medium")
                else       -> listOf("HIGH RISK", "₹149", "₹7,000", "high")
            }

            val premiumInt = premium.replace("₹", "").trim().toIntOrNull() ?: 99
            val coverageInt = coverage.replace("₹", "").replace(",", "").trim().toIntOrNull() ?: 10000

            b.tvRiskLevel.text = level
            b.tvPremium.text   = "$premium / week"
            b.tvCoverage.text  = "Coverage up to $coverage"

            setupRadarChart(risk.fraud_score.toFloat())

            b.btnBuyPolicy.setOnClickListener {
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
        val b = _binding ?: return
        b.progressLoading.visibility = View.GONE
        b.contentGroup.visibility    = View.VISIBLE

        b.tvScoreNumber.text   = "52"
        b.progressRisk.progress = 52
        b.tvRiskLevel.text     = "MEDIUM RISK"
        b.tvPremium.text       = "₹99 / week"
        b.tvCoverage.text      = "Coverage up to ₹10,000"

        setupRadarChart(0.52f)

        b.btnBuyPolicy.setOnClickListener {
            val action = RiskScoreFragmentDirections
                .actionRiskScoreToPurchase(99, 10000, "medium")
            findNavController().navigate(action)
        }
    }

    private fun setupRadarChart(score: Float) {
        val b = _binding ?: return
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
            }

            b.radarChart.apply {
                data = RadarData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.textColor = 0xFF8892A4.toInt()
                yAxis.textColor = 0xFF8892A4.toInt()
                yAxis.setDrawLabels(false)
                legend.isEnabled = false
                description.isEnabled = false
                invalidate()
            }
        } catch (e: Exception) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}