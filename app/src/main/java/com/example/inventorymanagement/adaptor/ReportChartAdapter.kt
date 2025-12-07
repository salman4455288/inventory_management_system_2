package com.example.inventorymanagement.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.fragment.ReportsFragment

class ReportChartAdapter(private val items: List<ReportsFragment.ChartItem>) :
    RecyclerView.Adapter<ReportChartAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val left: TextView = view.findViewById(R.id.tvLeft)
        val right: TextView = view.findViewById(R.id.tvRight)
        val progress: ProgressBar = view.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chart_bar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.left.text = item.label
        holder.right.text = item.displayValue

        // Dynamic Max calculation logic or fixed max
        // For simplicity, we set max to a high fixed value or relative
        // Better logic: calculate max in Fragment and pass it here
        holder.progress.max = 100

        // Normalize progress if needed, assuming input is already scaled or just raw
        // If raw, we need max. For percentage charts, use 0-100.
        // For sales, we might clamp it.
        holder.progress.progress = item.progress.toInt().coerceAtMost(100)
    }
    override fun getItemCount() = items.size
}