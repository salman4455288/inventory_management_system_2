package com.example.inventorymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import org.json.JSONObject

class RecentTransactionsAdapter(private var transactions: List<JSONObject>) :
    RecyclerView.Adapter<RecentTransactionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // You'll need a simple item layout for this.
        // I'll assume you have 'item_transaction.xml' or we can reuse 'item_cart' structure simply.
        // For professional look, create item_transaction.xml
        val tvId: TextView = view.findViewById(R.id.tvTransId)
        val tvAmount: TextView = view.findViewById(R.id.tvTransAmount)
        val tvDate: TextView = view.findViewById(R.id.tvTransDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = transactions[position]
        holder.tvId.text = "Sale #${item.getString("id")}"
        holder.tvAmount.text = "$${item.getString("total_amount")}"
        holder.tvDate.text = item.getString("created_at")
    }

    override fun getItemCount() = transactions.size

    fun updateList(newList: List<JSONObject>) {
        transactions = newList
        notifyDataSetChanged()
    }
}