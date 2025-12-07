package com.example.inventorymanagement.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.dataclass.Product
import com.example.inventorymanagement.util.BaseURL
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class ManualProductAdapter(
    private var productList: List<Product>,
    private val onAddClick: (Product) -> Unit
) : RecyclerView.Adapter<ManualProductAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvBarcode: TextView = itemView.findViewById(R.id.tvBarcode)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val btnAdd: Button = itemView.findViewById(R.id.btnAdd)
        val ivImage: ImageView = itemView.findViewById(R.id.ivProductImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_manual_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        // --- DEBUG LOG: Check if adapter is binding this item ---
        Log.d("IMAGE_DEBUG", "Binding Item: ${product.name}, Raw Image URL: '${product.image_url}'")

        holder.tvName.text = product.name
        holder.tvPrice.text = String.format("$%.2f", product.sale_price)
        holder.tvBarcode.text = "Barcode: ${product.barcode ?: "N/A"}"
        holder.tvStock.text = "Stock: ${product.stock_qty}"

        // --- IMPROVED IMAGE LOADING ---
        if (!product.image_url.isNullOrEmpty()) {
            val context = holder.itemView.context
            var baseUrl = BaseURL.getUrl(context)

            // 1. Fix Base URL slash
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/"
            }

            // 2. Clean Path & FIX WINDOWS BACKSLASHES
            var imagePath = product.image_url.replace("\\", "/")
            if (imagePath.startsWith("/")) {
                imagePath = imagePath.substring(1)
            }

            // 3. Construct Final URL
            val fullUrl = if (imagePath.startsWith("http")) {
                imagePath
            } else {
                baseUrl + imagePath
            }

            Log.d("IMAGE_DEBUG", "Attempting to load: $fullUrl")

            // 4. Load with Logging Callback
            Picasso.get()
                .load(fullUrl)
                .placeholder(R.drawable.package_img)
                .error(R.drawable.package_img)
                .into(holder.ivImage, object : Callback {
                    override fun onSuccess() {
                        Log.d("IMAGE_DEBUG", "Success: $fullUrl")
                    }

                    override fun onError(e: Exception?) {
                        Log.e("IMAGE_DEBUG", "FAILED to load: $fullUrl", e)
                    }
                })
        } else {
            Log.d("IMAGE_DEBUG", "Image URL is empty/null for ${product.name}. Setting placeholder.")
            holder.ivImage.setImageResource(R.drawable.package_img)
        }

        holder.btnAdd.setOnClickListener {
            onAddClick(product)
        }
    }

    override fun getItemCount(): Int = productList.size

    fun updateList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }
}