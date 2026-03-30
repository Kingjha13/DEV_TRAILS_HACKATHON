package com.example.shieldnet.onboarding

import com.example.shieldnet.R
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shieldnet.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        val platforms = listOf("Swiggy", "Zomato", "Blinkit", "Zepto", "Dunzo", "Other")
        binding.spinnerPlatform.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, platforms)

        val cities = listOf("Mumbai", "Delhi", "Bangalore", "Pune", "Chennai",
            "Hyderabad", "Kolkata", "Ahmedabad", "Jaipur", "Surat", "Vadodara")
        binding.spinnerCity.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, cities)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val upi  = binding.etUpi.text.toString().trim()
            val earnings = binding.etWeeklyEarnings.text.toString().toIntOrNull() ?: 0

            var valid = true
            if (name.isEmpty())    { binding.etName.error = "Required"; valid = false }
            if (upi.isEmpty())     { binding.etUpi.error = "Required"; valid = false }
            if (earnings == 0)     { binding.etWeeklyEarnings.error = "Enter your weekly earnings"; valid = false }
            if (!valid) return@setOnClickListener

            viewModel.register(
                name = name,
                city = binding.spinnerCity.selectedItem.toString(),
                platform = binding.spinnerPlatform.selectedItem.toString().lowercase(),
                weeklyAvg = earnings,
                upiHandle = upi
            )
        }

        viewModel.loading.observe(viewLifecycleOwner) {
            binding.btnRegister.isEnabled = !it
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            findNavController().navigate(R.id.action_register_to_risk_score)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            msg ?: return@observe
            android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
