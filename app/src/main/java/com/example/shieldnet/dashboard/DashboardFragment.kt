package com.example.shieldnet.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shieldnet.R
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shieldnet.ar.ArNavigationActivity
import com.example.shieldnet.databinding.FragmentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        viewModel.loadDashboard()

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.scrollContent.visibility = if (loading) View.GONE else View.VISIBLE
        }

        viewModel.activePolicy.observe(viewLifecycleOwner) { policy ->
            if (policy != null) {
                binding.cardPolicy.visibility  = View.VISIBLE
                binding.cardNoPolicyCta.visibility = View.GONE
                binding.tvPolicyTier.text      = policy.planTier.uppercase() + " PLAN"
                binding.tvPolicyCoverage.text  = "Covered up to ₹${policy.coverageInr}"
                binding.tvPolicyExpiry.text    = "Valid until ${formatDate(policy.expiresAt)}"
                binding.chipPolicyStatus.text  = policy.status.uppercase()
                val chipColor = if (policy.status == "active") R.color.status_paid else R.color.status_pending
                binding.chipPolicyStatus.setChipBackgroundColorResource(chipColor)
            } else {
                binding.cardPolicy.visibility      = View.GONE
                binding.cardNoPolicyCta.visibility = View.VISIBLE
            }
        }

        viewModel.triggerStatus.observe(viewLifecycleOwner) { status ->
            if (status.allClear) {
                binding.cardTrigger.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.trigger_safe))
                binding.tvTriggerTitle.text = "All Clear ✓"
                binding.tvTriggerDesc.text  = "No disruptions detected in your area"
                binding.ivTriggerIcon.setImageResource(R.drawable.ic_shield_check)
            } else {
                binding.cardTrigger.setCardBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.trigger_alert))
                binding.tvTriggerTitle.text = "⚠ Disruption Detected!"
                val types = status.activeTriggers.joinToString(" • ") {
                    it.type.replace("_", " ").replaceFirstChar { c -> c.uppercase() }
                }
                binding.tvTriggerDesc.text = "$types — auto-processing claim"
                binding.ivTriggerIcon.setImageResource(R.drawable.ic_alert)
            }
        }

        val claimsAdapter = ClaimsAdapter()
        binding.rvClaims.layoutManager = LinearLayoutManager(requireContext())
        binding.rvClaims.adapter = claimsAdapter

        viewModel.claims.observe(viewLifecycleOwner) { claims ->
            claimsAdapter.submitList(claims)
            binding.tvNoClaimsMsg.visibility = if (claims.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnArNav.setOnClickListener {
            startActivity(Intent(requireContext(), ArNavigationActivity::class.java))
        }

        binding.btnBuyPolicyCta.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_risk_score)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun formatDate(isoDate: String): String = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(isoDate) ?: return isoDate
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
    } catch (e: Exception) { isoDate }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
