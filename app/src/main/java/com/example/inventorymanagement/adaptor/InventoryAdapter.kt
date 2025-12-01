package com.example.inventorymanagement.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.Product

// CHANGED: Added 'onEditClick' callback to the constructor
class InventoryAdapter(
    private var productList: List<Product>,
    private val onEditClick: (Product) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvSku: TextView = itemView.findViewById(R.id.tvSku)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val tvMin: TextView = itemView.findViewById(R.id.tvMin)
        val tvCost: TextView = itemView.findViewById(R.id.tvCost)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvSupplier: TextView = itemView.findViewById(R.id.tvSupplier)
        val tvPercent: TextView = itemView.findViewById(R.id.tvStockPercent)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressStock)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val product = productList[position]

        holder.tvName.text = product.name
        holder.tvSku.text = "SKU: ${product.sku}"
        holder.tvCategory.text = "Category: ${product.category}"
        holder.tvStock.text = "Stock: ${product.stock_qty} units"
        holder.tvMin.text = "Min: ${product.min_stock}"
        holder.tvCost.text = "Cost: $${product.cost_price}"
        holder.tvPrice.text = "Price: $${product.sale_price}"
        holder.tvSupplier.text = "Supplier: ${product.supplier ?: "N/A"}"

        // Stock Status Logic
        if (product.stock_qty <= product.min_stock) {
            holder.tvStatus.text = "Low Stock"
            holder.tvStatus.setBackgroundColor(Color.parseColor("#E57373")) // Red
        } else {
            holder.tvStatus.text = "In Stock"
            holder.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
        }

        // Progress Bar
        val targetStock = if (product.min_stock > 0) product.min_stock * 3 else 100
        val percentage = (product.stock_qty.toDouble() / targetStock) * 100
        val displayPercent = if (percentage > 100) 100 else percentage.toInt()

        holder.progressBar.progress = displayPercent
        holder.tvPercent.text = "$displayPercent%"

        // --- CLICK LISTENER ---
        holder.btnEdit.setOnClickListener {
            onEditClick(product) // Pass the product back to the Fragment
        }
    }

    override fun getItemCount(): Int = productList.size

    fun updateData(newProducts: List<Product>) {
        productList = newProducts
        notifyDataSetChanged()
    }
}