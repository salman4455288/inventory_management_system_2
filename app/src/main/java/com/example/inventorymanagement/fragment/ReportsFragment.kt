package com.example.inventorymanagement.fragment

import android.content.ContentValues
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.inventorymanagement.R
import com.example.inventorymanagement.adapter.ReportChartAdapter
import com.example.inventorymanagement.adapter.ReportStockAdapter
import com.example.inventorymanagement.util.BaseURL
import com.example.inventorymanagement.util.NotificationHelper
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {

    // --- VIEWS ---
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnTimeframe: View
    private lateinit var tvTimeframe: TextView
    private lateinit var btnDownload: ImageView

    private lateinit var btnSalesTab: Button
    private lateinit var btnInventoryTab: Button
    private lateinit var btnFinancialTab: Button

    private lateinit var cardTotalSales: View
    private lateinit var cardTotalOrders: View
    private lateinit var cardAvgOrder: View
    private lateinit var cardNetProfit: View

    private lateinit var frameSales: View
    private lateinit var frameInventory: View
    private lateinit var frameFinancial: View

    // --- INTERNAL VIEWS ---
    private var tvSalesRevenue: TextView? = null
    private var recyclerDailySales: RecyclerView? = null
    private var recyclerCategories: RecyclerView? = null
    private var layoutTopProducts: LinearLayout? = null

    private var cardInvTotal: View? = null
    private var cardInvLow: View? = null
    private var cardInvValue: View? = null
    private var recyclerStockLevels: RecyclerView? = null

    private var cardFinRevenue: View? = null
    private var cardFinExpense: View? = null
    private var cardFinProfit: View? = null
    private var cardFinMargin: View? = null
    private var recyclerProfitTrend: RecyclerView? = null

    // DATE VARIABLES
    private var currentStartDate = ""
    private var currentEndDate = ""
    private var currentTab = "sales"
    private var currentTimeframe = "This Month"

    // DATA HOLDER FOR PDF
    private var lastReportData: JSONObject? = null
    private var lastHeaderData: JSONObject? = null

    private val BASE_URL: String by lazy { BaseURL.getUrl(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_reports, container, false)
        initializeViews(view)
        setupClickListeners()

        // Default to "This Month"
        calculateDateRange("This Month")

        return view
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh) // Ensure you added this in XML

        btnTimeframe = view.findViewById(R.id.btnTimeframe)
        tvTimeframe = view.findViewById(R.id.tvTimeframe)
        btnDownload = view.findViewById(R.id.btnDownload)

        cardTotalSales = view.findViewById(R.id.cardTotalSales)
        cardTotalOrders = view.findViewById(R.id.cardTotalOrders)
        cardAvgOrder = view.findViewById(R.id.cardAvgOrder)
        cardNetProfit = view.findViewById(R.id.cardNetProfit)

        btnSalesTab = view.findViewById(R.id.btnSalesTab)
        btnInventoryTab = view.findViewById(R.id.btnInventoryTab)
        btnFinancialTab = view.findViewById(R.id.btnFinancialTab)

        frameSales = view.findViewById(R.id.frameSales)
        frameInventory = view.findViewById(R.id.frameInventory)
        frameFinancial = view.findViewById(R.id.frameFinancial)

        // Bind Sales Frame
        tvSalesRevenue = frameSales.findViewById(R.id.tvSalesRevenue)
        recyclerDailySales = frameSales.findViewById(R.id.recyclerDailySales)
        recyclerCategories = frameSales.findViewById(R.id.recyclerCategories)
        layoutTopProducts = frameSales.findViewById(R.id.layoutTopProducts)

        // Bind Inventory Frame
        cardInvTotal = frameInventory.findViewById(R.id.cardInvTotal)
        cardInvLow = frameInventory.findViewById(R.id.cardInvLow)
        cardInvValue = frameInventory.findViewById(R.id.cardInvValue)
        recyclerStockLevels = frameInventory.findViewById(R.id.recyclerStockLevels)

        // Bind Financial Frame
        cardFinRevenue = frameFinancial.findViewById(R.id.cardFinRevenue)
        cardFinExpense = frameFinancial.findViewById(R.id.cardFinExpense)
        cardFinProfit = frameFinancial.findViewById(R.id.cardFinProfit)
        cardFinMargin = frameFinancial.findViewById(R.id.cardFinMargin)
        recyclerProfitTrend = frameFinancial.findViewById(R.id.recyclerProfitTrend)

        setupRecycler(recyclerDailySales)
        setupRecycler(recyclerCategories)
        setupRecycler(recyclerStockLevels)
        setupRecycler(recyclerProfitTrend)
    }

    private fun setupRecycler(recycler: RecyclerView?) {
        recycler?.layoutManager = LinearLayoutManager(context)
        recycler?.isNestedScrollingEnabled = false
    }

    private fun setupClickListeners() {
        btnTimeframe.setOnClickListener { showTimeframeMenu() }
        btnSalesTab.setOnClickListener { switchTab("sales") }
        btnInventoryTab.setOnClickListener { switchTab("inventory") }
        btnFinancialTab.setOnClickListener { switchTab("financial") }

        // PDF Download
        btnDownload.setOnClickListener {
            if (lastReportData != null) {
                createAndSavePdf()
            } else {
                Toast.makeText(context, "Data loading...", Toast.LENGTH_SHORT).show()
            }
        }

        // Refresh
        if (::swipeRefresh.isInitialized) {
            swipeRefresh.setOnRefreshListener { refreshData() }
        }
    }

    // --- PDF LOGIC ---
    private fun createAndSavePdf() {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Header
            paint.color = Color.BLACK
            paint.textSize = 24f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Inventory Report", 40f, 60f, paint)

            paint.textSize = 14f
            paint.typeface = android.graphics.Typeface.DEFAULT
            paint.color = Color.DKGRAY
            canvas.drawText("Generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()), 40f, 85f, paint)
            canvas.drawText("Period: ${tvTimeframe.text}", 40f, 105f, paint)

            // Divider
            paint.color = Color.LTGRAY
            paint.strokeWidth = 2f
            canvas.drawLine(40f, 120f, 555f, 120f, paint)

            // Content
            var yPos = 160f
            paint.color = Color.BLACK
            paint.textSize = 16f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            canvas.drawText("Summary ($currentTab)", 40f, yPos, paint)
            yPos += 30f

            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.DEFAULT

            // Header Stats
            if (lastHeaderData != null) {
                val h = lastHeaderData!!
                val sales = h.optString("total_sales", "0")
                val orders = h.optString("total_orders", "0")
                canvas.drawText("Total Sales: $$sales", 40f, yPos, paint)
                canvas.drawText("Total Orders: $orders", 250f, yPos, paint)
                yPos += 40f
            }

            // Table Header
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawRect(40f, yPos - 15f, 555f, yPos + 10f, paint)
            paint.color = Color.BLACK

            if (currentTab == "sales") {
                canvas.drawText("Date", 50f, yPos, paint)
                canvas.drawText("Orders", 200f, yPos, paint)
                canvas.drawText("Sales ($)", 350f, yPos, paint)
                yPos += 25f

                val daily = lastReportData!!.optJSONArray("daily_sales")
                if (daily != null) {
                    for (i in 0 until daily.length()) {
                        val item = daily.getJSONObject(i)
                        canvas.drawText(item.optString("date"), 50f, yPos, paint)
                        canvas.drawText(item.optString("orders"), 200f, yPos, paint)
                        canvas.drawText(item.optString("sales"), 350f, yPos, paint)
                        yPos += 20f
                    }
                }
            } else if (currentTab == "inventory") {
                canvas.drawText("Item Name", 50f, yPos, paint)
                canvas.drawText("Stock", 300f, yPos, paint)
                canvas.drawText("Value", 450f, yPos, paint)
                yPos += 25f

                val stock = lastReportData!!.optJSONArray("stock_levels")
                if (stock != null) {
                    for (i in 0 until stock.length()) {
                        val item = stock.getJSONObject(i)
                        canvas.drawText(item.optString("name"), 50f, yPos, paint)
                        canvas.drawText(item.optString("stock_qty"), 300f, yPos, paint)
                        canvas.drawText(item.optString("value"), 450f, yPos, paint)
                        yPos += 20f
                    }
                }
            }

            pdfDocument.finishPage(page)

            // Save
            val fileName = "Report_${System.currentTimeMillis()}.pdf"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = requireContext().contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                    NotificationHelper.showReportDownloadNotification(requireContext(), fileName)
                }
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // --- DATE LOGIC ---
    private fun calculateDateRange(option: String) {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var end = calendar.time

        when (option) {
            "Today" -> {}
            "Yesterday" -> { calendar.add(Calendar.DAY_OF_YEAR, -1); end = calendar.time }
            "This Week" -> { calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek) }
            "Last Week" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startWeek = calendar.time
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                end = calendar.time
                calendar.time = startWeek
            }
            "This Month" -> { calendar.set(Calendar.DAY_OF_MONTH, 1) }
            "Last Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startMonth = calendar.time
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                end = calendar.time
                calendar.time = startMonth
            }
            "This Year" -> { calendar.set(Calendar.DAY_OF_YEAR, 1) }
        }

        currentStartDate = format.format(calendar.time) + " 00:00:00"
        currentEndDate = format.format(end) + " 23:59:59"

        tvTimeframe.text = option
        currentTimeframe = option
        refreshData()
    }

    private fun showCustomDatePicker() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select Date Range")
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val display = SimpleDateFormat("MMM dd", Locale.getDefault())
            currentStartDate = format.format(Date(selection.first)) + " 00:00:00"
            currentEndDate = format.format(Date(selection.second)) + " 23:59:59"
            tvTimeframe.text = "${display.format(Date(selection.first))} - ${display.format(Date(selection.second))}"
            refreshData()
        }
        picker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimeframeMenu() {
        val popup = PopupMenu(requireContext(), btnTimeframe)
        val options = listOf("Today", "Yesterday", "This Week", "Last Week", "This Month", "Last Month", "This Year", "Custom Range")
        options.forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener {
            if (it.title == "Custom Range") showCustomDatePicker()
            else calculateDateRange(it.title.toString())
            true
        }
        popup.show()
    }

    private fun refreshData() {
        fetchHeaderStats()
        loadTabContent(currentTab)
        if (::swipeRefresh.isInitialized) swipeRefresh.isRefreshing = false
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        frameSales.visibility = if (tab == "sales") View.VISIBLE else View.GONE
        frameInventory.visibility = if (tab == "inventory") View.VISIBLE else View.GONE
        frameFinancial.visibility = if (tab == "financial") View.VISIBLE else View.GONE

        resetTabStyles()
        when(tab) {
            "sales" -> styleActiveTab(btnSalesTab)
            "inventory" -> styleActiveTab(btnInventoryTab)
            "financial" -> styleActiveTab(btnFinancialTab)
        }
        loadTabContent(tab)
    }

    private fun resetTabStyles() {
        val defaultTint = ColorStateList.valueOf(Color.parseColor("#E5E7EB"))
        val defaultText = Color.parseColor("#4B5563")
        listOf(btnSalesTab, btnInventoryTab, btnFinancialTab).forEach { it.backgroundTintList = defaultTint; it.setTextColor(defaultText) }
    }
    private fun styleActiveTab(btn: Button) {
        btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#111827")); btn.setTextColor(Color.WHITE)
    }

    // --- API CALLS ---
    private fun fetchHeaderStats() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_reports.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") +
                        "&start_date=" + URLEncoder.encode(currentStartDate, "UTF-8") +
                        "&end_date=" + URLEncoder.encode(currentEndDate, "UTF-8")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        lastHeaderData = json
                        val sales = json.optDouble("total_sales", 0.0)
                        val orders = json.optInt("total_orders", 0)
                        val avg = json.optDouble("avg_order", 0.0)
                        val profit = json.optDouble("net_profit", 0.0)

                        updateHeaderCard(cardTotalSales, "Total Sales", String.format("$%.0f", sales), "")
                        updateHeaderCard(cardTotalOrders, "Total Orders", orders.toString(), "")
                        updateHeaderCard(cardAvgOrder, "Avg Order", String.format("$%.0f", avg), "")
                        updateHeaderCard(cardNetProfit, "Net Profit", String.format("$%.0f", profit), "")
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun loadTabContent(type: String) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_detailed_reports.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") +
                        "&type=" + URLEncoder.encode(type, "UTF-8") +
                        "&start_date=" + URLEncoder.encode(currentStartDate, "UTF-8") +
                        "&end_date=" + URLEncoder.encode(currentEndDate, "UTF-8")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    if(!isAdded) return@withContext
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            lastReportData = json
                            updateTabUI(type, json)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateTabUI(type: String, json: JSONObject) {
        when (type) {
            "sales" -> {
                val total = json.optDouble("total_sales", 0.0)
                tvSalesRevenue?.text = String.format("$%.2f", total)

                // Charts & Lists
                val daily = json.optJSONArray("daily_sales") ?: JSONArray()
                val dailyList = ArrayList<ChartItem>()
                for(i in 0 until daily.length()) {
                    val obj = daily.getJSONObject(i)
                    dailyList.add(ChartItem(obj.optString("date"), "$" + obj.optString("sales"), obj.optDouble("sales")))
                }
                recyclerDailySales?.adapter = ReportChartAdapter(dailyList)

                layoutTopProducts?.removeAllViews()
                val top = json.optJSONArray("top_products") ?: JSONArray()
                for(i in 0 until top.length()) {
                    val p = top.getJSONObject(i)
                    addListRow(layoutTopProducts, "${i+1}. ${p.optString("name")}", "${p.optString("sales")}")
                }

                val cats = json.optJSONArray("categories") ?: JSONArray()
                val catList = ArrayList<ChartItem>()
                var maxVal = 0.0
                for(i in 0 until cats.length()) { if(cats.getJSONObject(i).optDouble("sales") > maxVal) maxVal = cats.getJSONObject(i).optDouble("sales") }
                for(i in 0 until cats.length()) {
                    val obj = cats.getJSONObject(i)
                    val v = obj.optDouble("sales")
                    val p = if(maxVal > 0) (v/maxVal)*100 else 0.0
                    catList.add(ChartItem(obj.optString("name"), "${p.toInt()}%", p))
                }
                recyclerCategories?.adapter = ReportChartAdapter(catList)
            }
            "inventory" -> {
                val stats = json.optJSONObject("stats")
                if (stats != null) {
                    setMetric(cardInvTotal, "Items", stats.optString("total_items"))
                    setMetric(cardInvLow, "Low Stock", stats.optString("low_stock"))
                    setMetric(cardInvValue, "Value", "$" + String.format("%,.0f", stats.optDouble("asset_value")))
                }
                val stockList = ArrayList<JSONObject>()
                val array = json.optJSONArray("stock_levels") ?: JSONArray()
                for(i in 0 until array.length()) stockList.add(array.getJSONObject(i))
                recyclerStockLevels?.adapter = ReportStockAdapter(stockList)
            }
            "financial" -> {
                val rev = json.optDouble("revenue", 0.0)
                val exp = json.optDouble("expenses", 0.0)
                val profit = json.optDouble("net_profit", 0.0)
                val margin = if(rev > 0) (profit/rev)*100 else 0.0

                updateHeaderCard(cardFinRevenue, "Revenue", String.format("$%.0f", rev), "")
                updateHeaderCard(cardFinExpense, "Expenses", String.format("$%.0f", exp), "")
                updateHeaderCard(cardFinProfit, "Net Profit", String.format("$%.0f", profit), "")
                updateHeaderCard(cardFinMargin, "Margin", String.format("%.1f%%", margin), "")

                val trend = json.optJSONArray("profit_trend") ?: JSONArray()
                val trendList = ArrayList<ChartItem>()
                for(i in 0 until trend.length()) {
                    val obj = trend.getJSONObject(i)
                    trendList.add(ChartItem(obj.optString("date"), "$" + obj.optString("profit"), obj.optDouble("profit")))
                }
                recyclerProfitTrend?.adapter = ReportChartAdapter(trendList)
            }
        }
    }

    private fun updateHeaderCard(view: View?, label: String, value: String, trend: String) {
        view?.findViewById<TextView>(R.id.tvLabel)?.text = label
        view?.findViewById<TextView>(R.id.tvValue)?.text = value
        view?.findViewById<TextView>(R.id.tvTrend)?.text = trend
    }

    private fun setMetric(view: View?, label: String, value: String) {
        view?.findViewById<TextView>(R.id.tvStatLabel)?.text = label
        view?.findViewById<TextView>(R.id.tvStatValue)?.text = value
    }

    private fun addListRow(container: LinearLayout?, title: String, value: String) {
        if (container == null) return
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,16,0,16) }
        val t1 = TextView(context).apply { text=title; setTextColor(Color.BLACK); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
        val t2 = TextView(context).apply { text=value; setTextColor(Color.DKGRAY); typeface=android.graphics.Typeface.DEFAULT_BOLD }
        row.addView(t1); row.addView(t2); container.addView(row)
        container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(-1, 1); setBackgroundColor(Color.parseColor("#E0E0E0")) })
    }

    data class ChartItem(val label: String, val displayValue: String, val progress: Double)
    companion object { @JvmStatic fun newInstance() = ReportsFragment() }
}