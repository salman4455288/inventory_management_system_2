package com.example.inventorymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.CartItem

class CartItemAdapter(private val cartItems: MutableList<CartItem>) :
    RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder>() {

    inner class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvItemPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val btnMinus: ImageView = itemView.findViewById(R.id.btnMinus)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        val btnPlus: ImageView = itemView.findViewById(R.id.btnPlus)
        val tvUnitPrice: TextView = itemView.findViewById(R.id.tvUnitPrice)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartItemViewHolder, position: Int) {
        val item = cartItems[position]

        holder.tvItemName.text = item.name
        holder.tvItemPrice.text = String.format("$%.2f", item.price)
        holder.tvQuantity.text = item.quantity.toString()
        holder.tvUnitPrice.text = String.format("$ %.2f", item.unitPrice)

        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                notifyItemChanged(position)
            }
        }

        holder.btnPlus.setOnClickListener {
            item.quantity++
            notifyItemChanged(position)
        }

        holder.btnDelete.setOnClickListener {
            cartItems.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount() = cartItems.size
}