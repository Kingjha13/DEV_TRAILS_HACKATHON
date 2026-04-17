package com.example.shieldnet.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shieldnet.R
import com.example.shieldnet.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    private val claimsAdapter = ClaimsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)
        setupRecyclerView()
        setupClickListeners()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDashboard()
    }

    private fun setupRecyclerView() {
        binding.rvClaims.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter        = claimsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnArNav.setOnClickListener {
            Toast.makeText(requireContext(), "AR Intelligence — Scanning area...", Toast.LENGTH_SHORT).show()
        }
        binding.btnBuyPolicyCta.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_risk_score)
        }
    }

    private fun setupObservers() {

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading && binding.tvTriggerTitle.text.toString() == "System Monitoring...") {
                binding.scrollContent.visibility = View.GONE
            } else if (!isLoading) {
                binding.scrollContent.visibility = View.VISIBLE
            }
        }

        viewModel.activePolicy.observe(viewLifecycleOwner) { policy ->
            if (policy != null) {
                if (binding.cardPolicy.visibility == View.GONE) {
                    binding.cardPolicy.alpha = 0f
                    binding.cardPolicy.visibility = View.VISIBLE
                    binding.cardPolicy.animate().alpha(1f).setDuration(600).start()
                }
                binding.cardNoPolicyCta.visibility = View.GONE
                binding.tvCoveredBadge.visibility  = View.VISIBLE

                binding.tvPolicyTier.text     = "${policy.planTier.uppercase(Locale.ROOT)} PROTECTION"
                binding.tvPolicyCoverage.text = "₹${policy.coverageInr}"
                binding.tvPolicyExpiry.text   = "VALID UNTIL ${formatDate(policy.expiresAt).uppercase(Locale.ROOT)}"
                binding.chipPolicyStatus.text = policy.status.uppercase(Locale.ROOT)
            } else {
                binding.cardPolicy.visibility      = View.GONE
                binding.cardNoPolicyCta.visibility = View.VISIBLE
                binding.tvCoveredBadge.visibility  = View.GONE
            }
        }

        viewModel.triggerStatus.observe(viewLifecycleOwner) { status ->
            if (status.allClear || status.activeTriggers.isEmpty()) {
                binding.cardTrigger.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.bg_card))
                binding.tvTriggerTitle.text = "Shield Active ✓"
                binding.tvTriggerTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.brand_primary))
                binding.tvTriggerDesc.text = "Monitoring ${status.city} for disruptions..."
                binding.ivTriggerIcon.setImageResource(R.drawable.ic_shield_check)
                binding.ivTriggerIcon.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.bg_dark)
                binding.ivTriggerIcon.imageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.brand_primary)
            } else {
                binding.cardTrigger.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.trigger_bg))
                val trigger     = status.activeTriggers.firstOrNull { it.thresholdBreached }
                    ?: status.activeTriggers.first()
                val eventName   = trigger.type.replace("_", " ")
                    .replaceFirstChar { c -> c.uppercase() }
                val severityPct = (trigger.severity * 100).toInt()

                binding.tvTriggerTitle.text = "⚡ $eventName Detected"
                binding.tvTriggerTitle.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.trigger_accent))
                binding.tvTriggerDesc.text  = "Severity $severityPct% — Automatic payout triggered"
                binding.ivTriggerIcon.setImageResource(R.drawable.ic_alert)
                binding.ivTriggerIcon.backgroundTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.trigger_accent)
                binding.ivTriggerIcon.imageTintList =
                    ContextCompat.getColorStateList(requireContext(), R.color.white)
            }
        }

        viewModel.claims.observe(viewLifecycleOwner) { claims ->
            claimsAdapter.submitList(claims.toList())

            binding.tvNoClaimsMsg.visibility =
                if (claims.isEmpty()) View.VISIBLE else View.GONE

            val latestPaid = claims.firstOrNull { it.status == "paid" }
            if (latestPaid != null) {
                binding.cardPayoutAlert.visibility = View.VISIBLE
                binding.tvPayoutAmount.text = "₹${latestPaid.approvedAmount} Credited"
                binding.tvPayoutRef.text    = "Auto-payout: ${latestPaid.payoutRef}"

                binding.cardPayoutAlert.alpha       = 0f
                binding.cardPayoutAlert.translationY = 50f
                binding.cardPayoutAlert.animate()
                    .alpha(1f).translationY(0f).setDuration(600).start()
            } else {
                binding.cardPayoutAlert.visibility = View.GONE
            }

            println("📱 [DASHBOARD UI] Rendering ${claims.size} claim(s) in RecyclerView")
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun formatDate(isoDate: String): String = try {
        val sdf  = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(isoDate) ?: return isoDate
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) { isoDate }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}