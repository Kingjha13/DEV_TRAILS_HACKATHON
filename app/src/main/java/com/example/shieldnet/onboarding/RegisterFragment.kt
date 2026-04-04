package com.example.shieldnet.onboarding

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.shieldnet.R
import com.example.shieldnet.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OnboardingViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        val cities = listOf("Mumbai", "Delhi", "Bangalore", "Pune")
        binding.spinnerCity.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, cities)

        val platforms = listOf("Swiggy", "Zomato", "Blinkit", "Zepto")
        binding.spinnerPlatform.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, platforms)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val upi = binding.etUpi.text.toString()
            val earn = binding.etWeeklyEarnings.text.toString().toIntOrNull() ?: 0

            if (name.isNotEmpty() && upi.isNotEmpty() && earn > 0) {
                viewModel.register(name, binding.spinnerCity.selectedItem.toString(),
                    binding.spinnerPlatform.selectedItem.toString(), earn, upi)
            }
        }

        viewModel.registerResult.observe(viewLifecycleOwner) {
            if (it != null) findNavController().navigate(R.id.action_register_to_risk_score)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}