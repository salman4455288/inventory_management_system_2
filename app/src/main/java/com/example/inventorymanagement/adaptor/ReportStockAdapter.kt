package com.example.inventorymanagement.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import org.json.JSONObject

class ReportStockAdapter(private var items: List<JSONObject>) :
    RecyclerView.Adapter<ReportStockAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvItemName)
        val details: TextView = view.findViewById(R.id.tvStockDetails)
        val progress: ProgressBar = view.findViewById(R.id.progressStock)
        val badge: TextView = view.findViewById(R.id.tvStatusBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val qty = item.getInt("stock_qty")
        val min = item.getInt("min_stock")
        val value = item.getDouble("value")

        holder.name.text = item.getString("name")
        holder.details.text = "$qty units · $${String.format("%.2f", value)}"

        // Progress Calculation (Max is min_stock * 2 usually)
        val max = if (min > 0) min * 2 else 100
        holder.progress.max = max
        holder.progress.progress = qty

        if (qty <= min) {
            holder.badge.visibility = View.VISIBLE
            holder.badge.text = "Low Stock"
        } else {
            holder.badge.visibility = View.GONE
        }
    }
    override fun getItemCount() = items.size
}
