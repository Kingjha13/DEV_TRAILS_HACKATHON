package com.example.shieldnet.onboarding

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.shieldnet.R
import com.example.shieldnet.databinding.FragmentOtpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtpFragment : Fragment(R.layout.fragment_otp) {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by activityViewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOtpBinding.bind(view)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        binding.btnSendOtp.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            if (phone.length == 10) {
                viewModel.sendOtp(phone)
            } else {
                Toast.makeText(requireContext(), "Enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length == 6) {
                viewModel.verifyOtp(otp)
            } else {
                Toast.makeText(requireContext(), "Enter the 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.btnSendOtp.isEnabled   = !loading
            binding.btnVerifyOtp.isEnabled = !loading
        }

        viewModel.otpSent.observe(viewLifecycleOwner) { message ->
            if (message == null) return@observe

            binding.layoutOtpInput.visibility = View.VISIBLE

            showOtpNotification(message)

            val devOtp = Regex("""\[DEV:\s*(\d{6})]""").find(message)?.groupValues?.get(1)
            if (devOtp != null) {
                binding.etOtp.postDelayed({
                    binding.etOtp.setText(devOtp)
                }, 800)
            }
        }

        viewModel.authResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            findNavController().navigate(
                if (result.isRegistered) R.id.action_otp_to_dashboard
                else R.id.action_otp_to_register
            )
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
        }
    }

    private fun showOtpNotification(message: String) {
        val channelId = "otp_channel"
        val manager = requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Auth", NotificationManager.IMPORTANCE_HIGH)
            )
        }

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ShieldNet OTP")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(requireContext()).notify(1, notification)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}