package com.example.inventorymanagement.adaptor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.Customer

class CustomerAdapter(
    private var customers: List<Customer>,
    private val onCustomerClick: (Customer) -> Unit,
    private val onCallClick: (String) -> Unit,
    private val onViewBillsClick: (Customer) -> Unit // NEW CALLBACK
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvInitials: TextView = itemView.findViewById(R.id.tvAvatarInitials)
        val tvName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val tvOutstanding: TextView = itemView.findViewById(R.id.tvOutstanding)
        val cardOutstanding: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardOutstanding)
        val tvTotalPurchase: TextView = itemView.findViewById(R.id.tvTotalPurchase)
        val tvLastPurchase: TextView = itemView.findViewById(R.id.tvLastPurchase)
        val btnCall: View = itemView.findViewById(R.id.btnCall)
        val btnViewBills: TextView = itemView.findViewById(R.id.btnViewBills) // New Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = customers[position]

        val initials = if (customer.name.isNotBlank()) customer.name.take(1).uppercase() else "?"
        holder.tvInitials.text = initials
        holder.tvName.text = customer.name
        holder.tvPhone.text = customer.phone
        holder.tvOutstanding.text = String.format("$%.2f", customer.outstanding)
        holder.tvTotalPurchase.text = String.format("$%.2f", customer.total_purchase)
        holder.tvLastPurchase.text = customer.last_purchase ?: "New"

        // Dynamic Color Logic
        if (customer.outstanding < 0) {
            holder.cardOutstanding.setCardBackgroundColor(Color.parseColor("#FEEEEE")) // Red bg
            holder.tvOutstanding.setTextColor(Color.parseColor("#EE5D50")) // Red text
        } else {
            holder.cardOutstanding.setCardBackgroundColor(Color.parseColor("#E6FDF4")) // Green bg
            holder.tvOutstanding.setTextColor(Color.parseColor("#05CD99")) // Green text
        }

        // Click Listeners
        holder.itemView.setOnClickListener { onCustomerClick(customer) }
        holder.btnCall.setOnClickListener { onCallClick(customer.phone) }

        // NEW: History Click
        holder.btnViewBills.setOnClickListener { onViewBillsClick(customer) }
    }

    override fun getItemCount() = customers.size

    fun updateList(newList: List<Customer>) {
        customers = newList
        notifyDataSetChanged()
    }
}