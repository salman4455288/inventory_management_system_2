package com.example.inventorymanagement.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.Customer

class CustomerAdapter(private val customers: List<Customer>) :
    RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name = itemView.findViewById<TextView>(R.id.tvCustomerName)
        val email = itemView.findViewById<TextView>(R.id.tvEmail)
        val phone = itemView.findViewById<TextView>(R.id.tvPhone)
        val address = itemView.findViewById<TextView>(R.id.tvAddress)
        val outstanding = itemView.findViewById<TextView>(R.id.tvOutstanding)
        val total = itemView.findViewById<TextView>(R.id.tvTotalPurchase)
        val lastPurchase = itemView.findViewById<TextView>(R.id.tvLastPurchase)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = customers[position]
        holder.name.text = customer.name
        holder.email.text = customer.email
        holder.phone.text = customer.phone
        holder.address.text = customer.address
        holder.outstanding.text = "Outstanding: $${customer.outstanding}"
        holder.total.text = "Total: $${customer.totalPurchases}"
        holder.lastPurchase.text = customer.lastPurchase
    }

    override fun getItemCount() = customers.size
}
