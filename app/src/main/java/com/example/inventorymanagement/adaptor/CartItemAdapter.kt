package com.example.inventorymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.CartItem

class CartItemAdapter(
    private val cartItems: MutableList<CartItem>,
    private val onCartChanged: () -> Unit // Callback when totals need updating
) : RecyclerView.Adapter<CartItemAdapter.CartViewHolder>() {

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvTotalPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val tvUnitPrice: TextView = itemView.findViewById(R.id.tvUnitPrice)
        val tvQty: TextView = itemView.findViewById(R.id.tvQuantity)

        val btnPlus: ImageView = itemView.findViewById(R.id.btnPlus)
        val btnMinus: ImageView = itemView.findViewById(R.id.btnMinus)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit) // Placeholder for now
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        // Inflate the new item_cart layout you provided
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartItems[position]

        // 1. Set Texts
        holder.tvName.text = item.name
        holder.tvQty.text = item.quantity.toString()

        // Format Unit Price (e.g., "$ 2.50")
        holder.tvUnitPrice.text = String.format("$ %.2f", item.price)

        // Format Total Price (e.g., "$ 5.00")
        holder.tvTotalPrice.text = String.format("$ %.2f", item.total)

        // 2. Plus Button Logic
        holder.btnPlus.setOnClickListener {
            item.quantity++
            item.total = item.price * item.quantity
            notifyItemChanged(position)
            onCartChanged()
        }

        // 3. Minus Button Logic
        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                item.quantity--
                item.total = item.price * item.quantity
                notifyItemChanged(position)
                onCartChanged()
            } else {
                // If quantity is 1, minus button acts as delete (optional preference)
                // Or you can just do nothing. Let's act as delete for better UX.
                removeItem(position)
            }
        }

        // 4. Delete Button Logic (Specific to your new XML)
        holder.btnDelete.setOnClickListener {
            removeItem(position)
        }

        // 5. Edit Button Logic (Placeholder)
        holder.btnEdit.setOnClickListener {
            // Logic to edit price manually or add discount can go here later
        }
    }

    private fun removeItem(position: Int) {
        if (position >= 0 && position < cartItems.size) {
            cartItems.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, cartItems.size)
            onCartChanged()
        }
    }

    override fun getItemCount(): Int = cartItems.size
}