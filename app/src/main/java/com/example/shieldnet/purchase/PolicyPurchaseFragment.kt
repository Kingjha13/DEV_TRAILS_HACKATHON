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

    private var pendingOrderId: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPolicyPurchaseBinding.bind(view)

        Checkout.preload(requireContext())

        binding.tvPremiumAmount.text = "₹${args.premium}"
        binding.tvCoverageAmount.text = "₹${args.coverage}"
        binding.tvPlanTier.text =
            args.tier.replaceFirstChar { it.uppercase() } + " Plan"

        binding.btnPay.setOnClickListener {
            startPayment(args.premium)
        }

        viewModel.policyActivated.observe(viewLifecycleOwner) { policy: PolicyResponse? ->
            policy ?: return@observe
            Toast.makeText(requireContext(), "Policy Activated!", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_purchase_to_dashboard)
        }

        viewModel.error.observe(viewLifecycleOwner) {
            it?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun startPayment(amount: Int) {
        val checkout = Checkout()

        checkout.setKeyID("rzp_test_SZSJph7kCs2F1R")

        val options = JSONObject().apply {
            put("name", "ShieldNet")
            put("description", "Weekly Protection Plan")
            put("currency", "INR")
            put("amount", amount * 100)
        }

        checkout.open(requireActivity(), options)
    }

    override fun onPaymentSuccess(paymentId: String) {
        viewModel.activatePolicy(paymentId, pendingOrderId, args.tier)
    }

    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(requireContext(), "Payment failed", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}