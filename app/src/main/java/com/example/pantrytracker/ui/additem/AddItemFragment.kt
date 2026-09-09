package com.example.pantrytracker.ui.additem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.pantrytracker.PantryApplication
import com.example.pantrytracker.R
import com.example.pantrytracker.data.Category
import com.example.pantrytracker.data.Location
import com.example.pantrytracker.databinding.FragmentAddItemBinding
import com.example.pantrytracker.ui.viewmodel.PantryViewModel
import com.example.pantrytracker.ui.viewmodel.PantryViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PantryViewModel by activityViewModels {
        val app = requireActivity().application as PantryApplication
        PantryViewModelFactory(app.container.repository)
    }

    private var categoriesList: List<Category> = emptyList()
    private var locationsList: List<Location> = emptyList()

    private val oneDayMillis = 24L * 60 * 60 * 1000
    private var selectedExpiryDate: Long = System.currentTimeMillis() + (7 * oneDayMillis)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled; navigate back
        findNavController().popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupUnitsDropdown()
        setupExpiryPresets()
        setupDatePicker()
        setupSaveButton()
        updateExpiryDisplay()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupUnitsDropdown() {
        val units = listOf("pcs", "kg", "g", "liters", "ml", "carton", "pack", "can", "bottle", "loaf", "box")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.actvUnit.setAdapter(adapter)
    }

    private fun setupExpiryPresets() {
        binding.chipGroupPresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val now = System.currentTimeMillis()
            when (checkedIds.first()) {
                R.id.chip3Days -> selectedExpiryDate = now + (3 * oneDayMillis)
                R.id.chip1Week -> selectedExpiryDate = now + (7 * oneDayMillis)
                R.id.chip2Weeks -> selectedExpiryDate = now + (14 * oneDayMillis)
                R.id.chip1Month -> selectedExpiryDate = now + (30 * oneDayMillis)
            }
            updateExpiryDisplay()
        }
    }

    private fun setupDatePicker() {
        binding.cardDatePicker.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Expiry Date")
                .setSelection(selectedExpiryDate)
                .build()

            datePicker.addOnPositiveButtonClickListener { selectedEpoch ->
                selectedExpiryDate = selectedEpoch
                binding.chipGroupPresets.clearCheck()
                updateExpiryDisplay()
            }

            datePicker.show(parentFragmentManager, "MATERIAL_DATE_PICKER")
        }
    }

    private fun updateExpiryDisplay() {
        val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = "Expires: ${formatter.format(selectedExpiryDate)}"

        val daysLeft = (selectedExpiryDate - System.currentTimeMillis()) / oneDayMillis
        binding.tvSelectedDaysLeft.text = when {
            daysLeft < 0 -> "Expired (${-daysLeft} days ago)"
            daysLeft == 0L -> "Expires today"
            else -> "$daysLeft days remaining"
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allCategories.collect { categories ->
                        categoriesList = categories
                        val names = categories.map { it.name }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        binding.actvCategory.setAdapter(adapter)
                        if (binding.actvCategory.text.isNullOrEmpty() && categories.isNotEmpty()) {
                            binding.actvCategory.setText(categories.first().name, false)
                        }
                    }
                }
                launch {
                    viewModel.allLocations.collect { locations ->
                        locationsList = locations
                        val names = locations.map { it.name }
                        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                        binding.actvLocation.setAdapter(adapter)
                        if (binding.actvLocation.text.isNullOrEmpty() && locations.isNotEmpty()) {
                            binding.actvLocation.setText(locations.first().name, false)
                        }
                    }
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSaveItem.setOnClickListener {
            val name = binding.etItemName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                binding.tilItemName.error = "Item name is required"
                return@setOnClickListener
            }
            binding.tilItemName.error = null

            val categoryName = binding.actvCategory.text.toString()
            val category = categoriesList.firstOrNull { it.name == categoryName } ?: categoriesList.firstOrNull()
            if (category == null) {
                binding.tilCategory.error = "Please select a category"
                return@setOnClickListener
            }
            binding.tilCategory.error = null

            val locationName = binding.actvLocation.text.toString()
            val location = locationsList.firstOrNull { it.name == locationName } ?: locationsList.firstOrNull()
            if (location == null) {
                binding.tilLocation.error = "Please select a location"
                return@setOnClickListener
            }
            binding.tilLocation.error = null

            val quantity = binding.etQuantity.text?.toString()?.toFloatOrNull() ?: 1.0f
            val unit = binding.actvUnit.text.toString().trim().ifEmpty { "pcs" }

            viewModel.addItem(
                name = name,
                categoryId = category.id,
                locationId = location.id,
                quantity = quantity,
                unit = unit,
                purchaseDate = System.currentTimeMillis(),
                expiryDate = selectedExpiryDate
            ) {
                handlePostAddNavigation()
            }
        }
    }

    private fun handlePostAddNavigation() {
        // Section 6: POST_NOTIFICATIONS runtime permission (Android 13+) — request it right after the user adds their first item
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}