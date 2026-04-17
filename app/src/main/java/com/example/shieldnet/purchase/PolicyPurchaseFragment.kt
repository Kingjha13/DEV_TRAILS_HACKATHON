package com.example.shieldnet.purchase

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shieldnet.R
import com.example.shieldnet.data.model.PolicyResponse
import com.example.shieldnet.databinding.FragmentPolicyPurchaseBinding
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject

@AndroidEntryPoint
class PolicyPurchaseFragment : Fragment(R.layout.fragment_policy_purchase),
    PaymentResultListener {

    private var _binding: FragmentPolicyPurchaseBinding? = null
    private val binding get() = _binding!!

    private val args: PolicyPurchaseFragmentArgs by navArgs()
    private val viewModel: PurchaseViewModel by viewModels()

    /**
     * Tracks which tier the user tapped BEFORE Razorpay opens.
     * Set synchronously in the click handler so onPaymentSuccess always
     * has the correct value even if the system delays the callback.
     */
    private var selectedTier: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPolicyPurchaseBinding.bind(view)

        Checkout.preload(requireContext()) // call exactly once

        binding.btnBuyBasic.setOnClickListener {
            selectedTier = "basic"
            startRazorpayPayment(99)
        }

        binding.btnBuyStandard.setOnClickListener {
            selectedTier = "standard"
            startRazorpayPayment(149)
        }

        binding.btnBuyPremium.setOnClickListener {
            selectedTier = "premium"
            startRazorpayPayment(199)
        }

        viewModel.policyActivated.observe(viewLifecycleOwner) { policy: PolicyResponse? ->
            policy ?: return@observe
            Toast.makeText(
                requireContext(),
                "✅ ${policy.planTier.uppercase()} Policy Activated! Coverage ₹${policy.coverageInr}",
                Toast.LENGTH_LONG
            ).show()
            // Navigate to dashboard — onResume there will call loadDashboard() automatically
            findNavController().navigate(R.id.action_purchase_to_dashboard)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun startRazorpayPayment(amountInr: Int) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_SZSJph7kCs2F1R")

        val options = JSONObject().apply {
            put("name",        "ShieldNet")
            put("description", "Weekly ${selectedTier.uppercase()} Protection")
            put("currency",    "INR")
            put("amount",      amountInr * 100) // paise
        }

        try {
            checkout.open(requireActivity(), options)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Payment gateway error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentSuccess(paymentId: String) {
        val tier = selectedTier.ifEmpty { "standard" }
        println("💳 [PAYMENT SUCCESS] paymentId=$paymentId tier=$tier")
        viewModel.activatePolicy(paymentId, "order_${System.currentTimeMillis()}", tier)
    }

    override fun onPaymentError(code: Int, response: String?) {
        println("❌ [PAYMENT ERROR] code=$code response=$response")
        Toast.makeText(requireContext(), "Payment failed. Please try again.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}