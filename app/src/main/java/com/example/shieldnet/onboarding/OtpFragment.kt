package com.example.shieldnet.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shieldnet.R
import com.example.shieldnet.databinding.FragmentOtpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtpFragment  : Fragment(R.layout.fragment_otp) {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOtpBinding.bind(view)

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.length == 10) {
                viewModel.sendOtp(phone)
            } else {
                binding.etPhone.error = "Enter valid 10-digit number"
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length == 6) viewModel.verifyOtp(otp)
            else binding.etOtp.error = "Enter 6-digit OTP"
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnSendOtp.isEnabled = !loading
            binding.btnVerifyOtp.isEnabled = !loading
        }

        viewModel.otpSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                binding.layoutOtpInput.visibility = View.VISIBLE
                binding.btnSendOtp.text = getString(R.string.resend_otp)
                Toast.makeText(requireContext(), "OTP sent to your number!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            if (result.isRegistered) {
                findNavController().navigate(R.id.action_otp_to_dashboard)
            } else {
                findNavController().navigate(R.id.action_otp_to_register)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
