package com.example.inventorymanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.Product

class InventoryAdapter(private val products: MutableList<Product>) :
    RecyclerView.Adapter<InventoryAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvSku: TextView = itemView.findViewById(R.id.tvSku)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val tvMin: TextView = itemView.findViewById(R.id.tvMin)
        val tvCost: TextView = itemView.findViewById(R.id.tvCost)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvSupplier: TextView = itemView.findViewById(R.id.tvSupplier)
        val tvStockLevel: TextView = itemView.findViewById(R.id.tvStockLevel)
        val tvStockPercent: TextView = itemView.findViewById(R.id.tvStockPercent)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressStock)
        val btnEdit: ImageView = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inventory, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvStatus.text = product.status
        holder.tvSku.text = "SKU: ${product.sku}"
        holder.tvCategory.text = "Category: ${product.category}"
        holder.tvStock.text = "Stock: ${product.stock} units"
        holder.tvMin.text = "Min: ${product.minStock}"
        holder.tvCost.text = "Cost: $${String.format("%.2f", product.cost)}"
        holder.tvPrice.text = "Price: $${String.format("%.2f", product.price)}"
        holder.tvSupplier.text = "Supplier: ${product.supplier}"
        holder.tvStockLevel.text = "Stock Level"
        holder.tvStockPercent.text = "${product.stockPercent}%"
        holder.progressBar.progress = product.stockPercent

        // Set status color
        when (product.status) {
            "Low Stock" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_low_stock)
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.white))
            }
            "In Stock" -> {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_in_stock)
                holder.tvStatus.setTextColor(holder.itemView.context.getColor(android.R.color.white))
            }
        }

        holder.btnDelete.setOnClickListener {
            products.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun getItemCount() = products.size
}