package com.example.pantrytracker.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pantrytracker.R
import com.example.pantrytracker.data.PantryItem
import com.example.pantrytracker.databinding.ItemPantryBinding
import com.example.pantrytracker.domain.ExpiryStatus
import com.example.pantrytracker.domain.daysUntilExpiry
import com.example.pantrytracker.domain.expiryStatus
import kotlin.math.abs

class PantryAdapter(
    private val onItemClick: ((PantryItem) -> Unit)? = null
) : ListAdapter<PantryItem, PantryAdapter.PantryViewHolder>(PantryDiffCallback) {

    private var categoryMap: Map<Long, String> = emptyMap()
    private var locationMap: Map<Long, String> = emptyMap()

    fun setLookupData(categories: Map<Long, String>, locations: Map<Long, String>) {
        categoryMap = categories
        locationMap = locations
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PantryViewHolder {
        val binding = ItemPantryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PantryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PantryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PantryViewHolder(
        private val binding: ItemPantryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PantryItem) {
            val context = binding.root.context
            binding.tvItemName.text = item.name

            val locationName = locationMap[item.locationId] ?: "Other"
            val categoryName = categoryMap[item.categoryId] ?: "General"
            val qtyStr = if (item.quantity % 1f == 0f) {
                "${item.quantity.toInt()} ${item.unit}"
            } else {
                "${item.quantity} ${item.unit}"
            }
            binding.tvItemDetails.text = "$locationName • $categoryName • $qtyStr"

            val status = item.expiryStatus()
            val daysLeft = item.daysUntilExpiry()

            when (status) {
                ExpiryStatus.FRESH -> {
                    binding.tvStatusBadge.text = "${daysLeft}d left"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_fresh)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_fresh_text))
                    binding.viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.status_fresh_text))
                    binding.cardPantryItem.strokeColor = ContextCompat.getColor(context, R.color.status_fresh_stroke)
                }
                ExpiryStatus.EXPIRING_SOON -> {
                    binding.tvStatusBadge.text = if (daysLeft == 0L) "Expires today" else "${daysLeft}d left"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expiring)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expiring_text))
                    binding.viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.status_expiring_text))
                    binding.cardPantryItem.strokeColor = ContextCompat.getColor(context, R.color.status_expiring_stroke)
                }
                ExpiryStatus.EXPIRED -> {
                    val daysAgo = abs(daysLeft)
                    binding.tvStatusBadge.text = if (daysAgo == 0L) "Expired today" else "Expired ${daysAgo}d ago"
                    binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expired)
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_expired_text))
                    binding.viewStatusIndicator.setBackgroundColor(ContextCompat.getColor(context, R.color.status_expired_text))
                    binding.cardPantryItem.strokeColor = ContextCompat.getColor(context, R.color.status_expired_stroke)
                }
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    companion object PantryDiffCallback : DiffUtil.ItemCallback<PantryItem>() {
        override fun areItemsTheSame(oldItem: PantryItem, newItem: PantryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PantryItem, newItem: PantryItem): Boolean {
            return oldItem == newItem
        }
    }
}
