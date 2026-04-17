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
            _binding?.let { b ->
                if (b.progressLoading.visibility == View.VISIBLE) {
                    showFallbackUI(score = 52, reason = "timeout")
                }
            }
        }, 5000)

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
            println("📊 [RISK FRAGMENT] API score=$score decision=${risk.decision}")

            renderScore(b, score)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            println("⚠️ [RISK FRAGMENT] Error: $msg — showing fallback")
            showFallbackUI(score = 52, reason = msg)
        }

        viewModel.loadRiskScore()
    }

    private fun renderScore(b: com.example.shieldnet.databinding.FragmentRiskScoreBinding, score: Int) {
        b.progressLoading.visibility = View.GONE
        b.contentGroup.visibility    = View.VISIBLE

        b.tvScoreNumber.text   = score.toString()
        b.progressRisk.progress = score

        val (level, premiumInr, coverageDisplay, tier) = when {
            score < 35 -> RiskPlan("LOW RISK",    99,  "₹10,000", "basic")
            score < 65 -> RiskPlan("MEDIUM RISK", 149, "₹25,000", "standard")
            else       -> RiskPlan("HIGH RISK",   199, "₹50,000", "premium")
        }

        b.tvRiskLevel.text = level
        b.tvPremium.text   = "₹$premiumInr / week"
        b.tvCoverage.text  = "Coverage up to $coverageDisplay"

        setupRadarChart(score / 100f)

        b.btnBuyPolicy.setOnClickListener {
            val coverageInt = coverageDisplay.replace("₹", "").replace(",", "").trim().toIntOrNull() ?: 10000
            val action = RiskScoreFragmentDirections
                .actionRiskScoreToPurchase(premiumInr, coverageInt, tier)
            findNavController().navigate(action)
        }
    }

    private fun showFallbackUI(score: Int, reason: String) {
        val b = _binding ?: return
        println("ℹ️ [RISK FALLBACK] score=$score reason=$reason")
        renderScore(b, score)
    }

    private fun setupRadarChart(normalizedScore: Float) {
        val b = _binding ?: return
        try {
            val labels  = listOf("Rainfall", "Flood", "AQI", "Traffic")
            val entries = listOf(
                RadarEntry(normalizedScore * 80f + 10f),
                RadarEntry(normalizedScore * 60f + 15f),
                RadarEntry(normalizedScore * 70f + 20f),
                RadarEntry(normalizedScore * 50f + 25f)
            )
            val dataSet = RadarDataSet(entries, "Risk Factors").apply {
                color     = 0xFF00E5CC.toInt()
                fillColor = 0xFF00E5CC.toInt()
                setDrawFilled(true)
                fillAlpha = 60
                lineWidth = 2f
            }
            b.radarChart.apply {
                data = RadarData(dataSet)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                xAxis.textColor      = 0xFF8892A4.toInt()
                yAxis.textColor      = 0xFF8892A4.toInt()
                yAxis.setDrawLabels(false)
                legend.isEnabled      = false
                description.isEnabled = false
                invalidate()
            }
        } catch (e: Exception) {
            println("⚠️ [RADAR CHART] Render error: ${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class RiskPlan(
        val level: String,
        val premiumInr: Int,
        val coverageDisplay: String,
        val tier: String
    )
}