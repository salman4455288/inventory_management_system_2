package com.example.inventorymanagement.fragment

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.adapter.ReportChartAdapter
import com.example.inventorymanagement.adapter.ReportStockAdapter
import com.example.inventorymanagement.util.BaseURL
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var btnTimeframe: View
    private lateinit var tvTimeframe: TextView
    private lateinit var btnDownload: ImageView // Download Button

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

    // DATA FOR PDF
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
        btnTimeframe = view.findViewById(R.id.btnTimeframe)
        tvTimeframe = view.findViewById(R.id.tvTimeframe)
        btnDownload = view.findViewById(R.id.btnDownload) // Init Download Btn

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

        // Bind Sales
        tvSalesRevenue = frameSales.findViewById(R.id.tvSalesRevenue)
        recyclerDailySales = frameSales.findViewById(R.id.recyclerDailySales)
        recyclerCategories = frameSales.findViewById(R.id.recyclerCategories)
        layoutTopProducts = frameSales.findViewById(R.id.layoutTopProducts)

        // Bind Inventory
        cardInvTotal = frameInventory.findViewById(R.id.cardInvTotal)
        cardInvLow = frameInventory.findViewById(R.id.cardInvLow)
        cardInvValue = frameInventory.findViewById(R.id.cardInvValue)
        recyclerStockLevels = frameInventory.findViewById(R.id.recyclerStockLevels)

        // Bind Financial
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

        // DOWNLOAD ACTION
        btnDownload.setOnClickListener {
            if (lastReportData != null) {
                createAndSavePdf()
            } else {
                Toast.makeText(context, "Data not loaded yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- PDF GENERATION LOGIC ---
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createAndSavePdf() {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (Points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // 1. Header Styling
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        canvas.drawText("Inventory Report", 40f, 60f, paint)

        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = Color.DKGRAY
        canvas.drawText("Generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()), 40f, 85f, paint)
        canvas.drawText("Period: ${tvTimeframe.text}", 40f, 105f, paint)

        // Divider Line
        paint.color = Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(40f, 120f, 555f, 120f, paint)

        // 2. Summary Section (From Header Stats)
        var yPos = 160f
        paint.color = Color.BLACK
        paint.textSize = 16f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("Summary ($currentTab)", 40f, yPos, paint)

        yPos += 30f
        paint.textSize = 12f
        paint.typeface = android.graphics.Typeface.DEFAULT

        // Print Header Stats if available
        if (lastHeaderData != null) {
            val h = lastHeaderData!!
            val sales = h.optString("total_sales", "0")
            val orders = h.optString("total_orders", "0")
            val profit = h.optString("net_profit", "0")

            canvas.drawText("Total Sales: $$sales", 40f, yPos, paint)
            canvas.drawText("Total Orders: $orders", 200f, yPos, paint)
            canvas.drawText("Net Profit: $$profit", 360f, yPos, paint)
            yPos += 40f
        }

        // 3. Detailed Data Table (Based on active tab)
        paint.textSize = 16f
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("Detailed Data", 40f, yPos, paint)
        yPos += 30f

        // Draw Table Header
        paint.color = Color.parseColor("#EEEEEE")
        canvas.drawRect(40f, yPos - 15f, 555f, yPos + 10f, paint)
        paint.color = Color.BLACK
        paint.textSize = 12f

        if (currentTab == "sales") {
            canvas.drawText("Date", 50f, yPos, paint)
            canvas.drawText("Orders", 200f, yPos, paint)
            canvas.drawText("Sales ($)", 350f, yPos, paint)
            yPos += 25f

            val daily = lastReportData!!.optJSONArray("daily_sales")
            if (daily != null) {
                paint.typeface = android.graphics.Typeface.DEFAULT
                for (i in 0 until daily.length()) {
                    val item = daily.getJSONObject(i)
                    canvas.drawText(item.getString("date"), 50f, yPos, paint)
                    canvas.drawText(item.getString("orders"), 200f, yPos, paint)
                    canvas.drawText(item.getString("sales"), 350f, yPos, paint)
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
                paint.typeface = android.graphics.Typeface.DEFAULT
                for (i in 0 until stock.length()) {
                    val item = stock.getJSONObject(i)
                    canvas.drawText(item.getString("name"), 50f, yPos, paint)
                    canvas.drawText(item.getString("stock"), 300f, yPos, paint)
                    canvas.drawText(item.getString("value"), 450f, yPos, paint)
                    yPos += 20f
                }
            }
        }

        pdfDocument.finishPage(page)

        // 4. Save File
        val fileName = "Report_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = requireContext().contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        try {
            if (uri != null) {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    Toast.makeText(context, "Report Downloaded!", Toast.LENGTH_SHORT).show()
                    if (outputStream != null) {
                        pdfDocument.writeTo(outputStream)
                        outputStream.close()
                        Toast.makeText(context, "Report Downloaded!", Toast.LENGTH_SHORT).show()

                        com.example.inventorymanagement.util.NotificationHelper.showReportDownloadNotification(requireContext(), fileName)
                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }

    // --- DATE LOGIC ---
    private fun calculateDateRange(option: String) {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
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

        // Apply Time
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
            val start = selection.first
            val end = selection.second

            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val displayFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

            currentStartDate = format.format(Date(start)) + " 00:00:00"
            currentEndDate = format.format(Date(end)) + " 23:59:59"

            tvTimeframe.text = "${displayFormat.format(Date(start))} - ${displayFormat.format(Date(end))}"
            currentTimeframe = "Custom"
            refreshData()
        }
        picker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun showTimeframeMenu() {
        val popup = PopupMenu(requireContext(), btnTimeframe)
        val options = listOf("Today", "Yesterday", "This Week", "Last Week", "This Month", "Last Month", "This Year", "Custom Range")
        options.forEach { popup.menu.add(it) }
        popup.setOnMenuItemClickListener { item ->
            if (item.title == "Custom Range") showCustomDatePicker()
            else calculateDateRange(item.title.toString())
            true
        }
        popup.show()
    }

    private fun refreshData() {
        fetchHeaderStats()
        loadTabContent(currentTab)
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
        callApi("get_reports.php") { json ->
            lastHeaderData = json // Save for PDF
            val sales = json.getDouble("total_sales")
            val orders = json.getInt("total_orders")
            val avg = json.getDouble("avg_order")
            val profit = json.getDouble("net_profit")

            updateHeaderCard(cardTotalSales, "Total Sales", String.format("$%.0f", sales), "")
            updateHeaderCard(cardTotalOrders, "Total Orders", orders.toString(), "")
            updateHeaderCard(cardAvgOrder, "Avg Order", String.format("$%.0f", avg), "")
            updateHeaderCard(cardNetProfit, "Net Profit", String.format("$%.0f", profit), "")
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
                            lastReportData = json // Save for PDF
                            updateTabUI(type, json)
                            if (type == "sales") updateHeaderUI(json)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateHeaderUI(json: JSONObject) {
        if (json.has("total_sales")) {
            val sales = json.getDouble("total_sales")
            val orders = json.getInt("total_orders")
            val avg = if(orders > 0) sales/orders else 0.0
            updateHeaderCard(cardTotalSales, "Total Sales", String.format("$%.0f", sales), "")
            updateHeaderCard(cardTotalOrders, "Total Orders", orders.toString(), "")
            updateHeaderCard(cardAvgOrder, "Avg Order", String.format("$%.0f", avg), "")
        }
    }

    private fun updateTabUI(type: String, json: JSONObject) {
        // ... (Keep existing UI update logic for charts/lists) ...
        // Re-paste logic from previous response if needed,
        // essentially mapping JSON to Recycler Adapters
        when (type) {
            "sales" -> {
                val total = json.getDouble("total_sales")
                tvSalesRevenue?.text = String.format("$%.2f", total)
                val daily = json.getJSONArray("daily_sales")
                val dailyList = ArrayList<ChartItem>()
                for(i in 0 until daily.length()) {
                    val obj = daily.getJSONObject(i)
                    dailyList.add(ChartItem(obj.getString("date"), "$" + obj.getString("sales"), obj.getDouble("sales")))
                }
                recyclerDailySales?.adapter = ReportChartAdapter(dailyList)
                layoutTopProducts?.removeAllViews()
                val top = json.getJSONArray("top_products")
                for(i in 0 until top.length()) {
                    val p = top.getJSONObject(i)
                    addListRow(layoutTopProducts, "${i+1}. ${p.getString("name")}", "${p.getString("sales")}")
                }
            }
            "inventory" -> {
                val stats = json.getJSONObject("stats")
                cardInvTotal?.let { setMetric(it, "Items", stats.getString("total_items")) }
                cardInvLow?.let { setMetric(it, "Low Stock", stats.getString("low_stock")) }
                cardInvValue?.let { setMetric(it, "Value", "$" + String.format("%,.0f", stats.getDouble("asset_value"))) }
                val stockList = ArrayList<JSONObject>()
                val array = json.getJSONArray("stock_levels")
                for (i in 0 until array.length()) stockList.add(array.getJSONObject(i))
                recyclerStockLevels?.adapter = ReportStockAdapter(stockList)
            }
            "financial" -> {
                val rev = json.getDouble("revenue")
                val exp = json.getDouble("expenses")
                val profit = json.getDouble("net_profit")
                val margin = if(rev>0) (profit/rev)*100 else 0.0
                cardFinRevenue?.let { updateHeaderCard(it, "Revenue", String.format("$%.0f", rev), "") }
                cardFinExpense?.let { updateHeaderCard(it, "Expenses", String.format("$%.0f", exp), "") }
                cardFinProfit?.let { updateHeaderCard(it, "Net Profit", String.format("$%.0f", profit), "") }
                cardFinMargin?.let { updateHeaderCard(it, "Margin", String.format("%.1f%%", margin), "") }
                if (json.has("profit_trend")) {
                    val trend = json.getJSONArray("profit_trend")
                    val trendList = ArrayList<ChartItem>()
                    for(i in 0 until trend.length()) {
                        val obj = trend.getJSONObject(i)
                        trendList.add(ChartItem(obj.getString("date"), "$" + obj.getString("profit"), obj.getDouble("profit")))
                    }
                    recyclerProfitTrend?.adapter = ReportChartAdapter(trendList)
                }
            }
        }
    }

    private fun updateHeaderCard(view: View, label: String, value: String, trend: String) {
        view.findViewById<TextView>(R.id.tvLabel)?.text = label
        view.findViewById<TextView>(R.id.tvValue)?.text = value
        view.findViewById<TextView>(R.id.tvTrend)?.text = trend
    }

    private fun setMetric(view: View, label: String, value: String) {
        view.findViewById<TextView>(R.id.tvStatLabel)?.text = label
        view.findViewById<TextView>(R.id.tvStatValue)?.text = value
    }

    private fun addListRow(container: LinearLayout?, title: String, value: String) {
        if (container == null) return
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,16,0,16) }
        val t1 = TextView(context).apply { text=title; setTextColor(Color.BLACK); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
        val t2 = TextView(context).apply { text=value; setTextColor(Color.DKGRAY); typeface=android.graphics.Typeface.DEFAULT_BOLD }
        row.addView(t1); row.addView(t2); container.addView(row)
        container.addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(-1, 1); setBackgroundColor(Color.parseColor("#E0E0E0")) })
    }

    private fun callApi(endpoint: String, onSuccess: (JSONObject) -> Unit) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + endpoint)
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") +
                        "&timeframe=" + URLEncoder.encode(currentTimeframe, "UTF-8") +
                        "&start_date=" + URLEncoder.encode(currentStartDate, "UTF-8") +
                        "&end_date=" + URLEncoder.encode(currentEndDate, "UTF-8")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    if(isAdded) onSuccess(JSONObject(response))
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    data class ChartItem(val label: String, val displayValue: String, val progress: Double)
    companion object { @JvmStatic fun newInstance() = ReportsFragment() }
}