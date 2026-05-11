package com.example.jjhg.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.jjhg.data.Customer
import com.example.jjhg.data.CustomerWithBalance
import com.example.jjhg.databinding.ItemCustomerBinding

class CustomerAdapter(
    private val onClick: (Customer) -> Unit,
    private val onDelete: (Customer) -> Unit
) : ListAdapter<CustomerWithBalance, CustomerAdapter.CustomerViewHolder>(CustomerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding, onClick, onDelete)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CustomerViewHolder(
        private val binding: ItemCustomerBinding,
        private val onClick: (Customer) -> Unit,
        private val onDelete: (Customer) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CustomerWithBalance) {
            binding.textViewName.text = item.customer.name
            binding.textViewBalance.text = "₹${String.format(java.util.Locale.getDefault(), "%.2f", Math.abs(item.balance))}"
            
            if (item.balance > 0) {
                binding.textViewBalance.setTextColor(android.graphics.Color.parseColor("#E53935"))
                binding.textViewLastTransaction.text = "You will give"
            } else if (item.balance < 0) {
                binding.textViewBalance.setTextColor(android.graphics.Color.parseColor("#43A047"))
                binding.textViewLastTransaction.text = "You will take"
            } else {
                binding.textViewBalance.setTextColor(android.graphics.Color.GRAY)
                binding.textViewLastTransaction.text = "Settled"
            }

            binding.imageViewProfile.load(item.customer.photoUri) {
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
                crossfade(true)
            }

            binding.root.setOnClickListener { onClick(item.customer) }
            binding.root.setOnLongClickListener {
                onDelete(item.customer)
                true
            }
        }
    }

    object CustomerDiffCallback : DiffUtil.ItemCallback<CustomerWithBalance>() {
        override fun areItemsTheSame(oldItem: CustomerWithBalance, newItem: CustomerWithBalance): Boolean =
            oldItem.customer.id == newItem.customer.id

        override fun areContentsTheSame(oldItem: CustomerWithBalance, newItem: CustomerWithBalance): Boolean =
            oldItem == newItem
    }
}
