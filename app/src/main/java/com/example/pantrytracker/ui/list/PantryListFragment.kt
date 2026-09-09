package com.example.pantrytracker.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pantrytracker.PantryApplication
import com.example.pantrytracker.R
import com.example.pantrytracker.databinding.FragmentPantryListBinding
import com.example.pantrytracker.ui.viewmodel.ExpiryFilter
import com.example.pantrytracker.ui.viewmodel.PantryViewModel
import com.example.pantrytracker.ui.viewmodel.PantryViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class PantryListFragment : Fragment() {

    private var _binding: FragmentPantryListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PantryViewModel by activityViewModels {
        val app = requireActivity().application as PantryApplication
        PantryViewModelFactory(app.container.repository)
    }

    private lateinit var pantryAdapter: PantryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPantryListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupSwipeToConsume()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerView() {
        pantryAdapter = PantryAdapter { item ->
            // In the future, item click can open edit screen
        }

        binding.recyclerViewItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pantryAdapter
        }
    }

    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipAll -> {
                    viewModel.setExpiryFilter(ExpiryFilter.ALL)
                    viewModel.setLocationFilter(null)
                }
                R.id.chipExpiringSoon -> {
                    viewModel.setExpiryFilter(ExpiryFilter.EXPIRING_SOON)
                    viewModel.setLocationFilter(null)
                }
                R.id.chipFridge -> {
                    viewModel.setExpiryFilter(ExpiryFilter.ALL)
                    val fridgeId = viewModel.uiState.value.locations.entries
                        .firstOrNull { it.value.equals("Fridge", ignoreCase = true) }?.key
                    viewModel.setLocationFilter(fridgeId)
                }
                R.id.chipPantry -> {
                    viewModel.setExpiryFilter(ExpiryFilter.ALL)
                    val pantryId = viewModel.uiState.value.locations.entries
                        .firstOrNull { it.value.contains("Pantry", ignoreCase = true) }?.key
                    viewModel.setLocationFilter(pantryId)
                }
                R.id.chipFreezer -> {
                    viewModel.setExpiryFilter(ExpiryFilter.ALL)
                    val freezerId = viewModel.uiState.value.locations.entries
                        .firstOrNull { it.value.equals("Freezer", ignoreCase = true) }?.key
                    viewModel.setLocationFilter(freezerId)
                }
            }
        }
    }

    private fun setupSwipeToConsume() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                if (position in 0 until pantryAdapter.currentList.size) {
                    val item = pantryAdapter.currentList[position]
                    viewModel.markConsumed(item)
                    Snackbar.make(binding.root, "${item.name} marked used", Snackbar.LENGTH_LONG)
                        .setAction("Undo") { viewModel.undoConsumed(item) }
                        .show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewItems)
    }

    private fun setupListeners() {
        binding.btnBannerFilter.setOnClickListener {
            binding.chipExpiringSoon.isChecked = true
        }

        binding.fabAddItem.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_pantryListFragment_to_addItemFragment)
            } catch (e: Exception) {
                // If navigation destination not yet ready, ignore or handle gracefully
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading

                    // Expiry Warning Banner (Section 7)
                    binding.warningBannerCard.isVisible = state.expiringCount > 0
                    binding.warningBannerText.text = "${state.expiringCount} items expiring within 3 days"

                    // Submit items & lookup data to adapter
                    pantryAdapter.setLookupData(state.categories, state.locations)
                    pantryAdapter.submitList(state.items)

                    // Empty state toggle
                    val isEmpty = state.items.isEmpty() && !state.isLoading
                    binding.viewEmptyState.isVisible = isEmpty
                    binding.recyclerViewItems.isVisible = !isEmpty

                    if (isEmpty) {
                        if (state.totalActiveCount > 0) {
                            binding.tvEmptyTitle.text = "No matching items"
                            binding.tvEmptySubtitle.text = "Try switching your active filter chip above."
                        } else {
                            binding.tvEmptyTitle.text = "Your pantry is empty"
                            binding.tvEmptySubtitle.text = "Tap '+' to add your first grocery item and start tracking expiry dates."
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
