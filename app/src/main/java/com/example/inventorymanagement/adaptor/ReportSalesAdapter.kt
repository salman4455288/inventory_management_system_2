package com.example.inventorymanagement.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import org.json.JSONObject

class ReportSalesAdapter(private var items: List<JSONObject>) :
    RecyclerView.Adapter<ReportSalesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.tvDate)
        val count: TextView = view.findViewById(R.id.tvOrdersCount)
        val amount: TextView = view.findViewById(R.id.tvAmount)
        val progress: ProgressBar = view.findViewById(R.id.progressSales)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report_sale, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val total = item.getDouble("total")

        holder.date.text = item.getString("date")
        holder.count.text = "${item.getInt("orders")} orders"
        holder.amount.text = String.format("$%.0f", total)

        // Calculate progress relative to highest sale (simplified: assume 5000 is max target)
        holder.progress.max = 5000
        holder.progress.progress = total.toInt()
    }
    override fun getItemCount() = items.size
}