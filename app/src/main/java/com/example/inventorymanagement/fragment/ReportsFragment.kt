package com.example.inventorymanagement.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.inventorymanagement.R

class ReportsFragment : Fragment() {

    private lateinit var btnTimeframe: Button
    private lateinit var btnSalesTab: Button
    private lateinit var btnInventoryTab: Button
    private lateinit var btnCustomersTab: Button
    private lateinit var btnFinancialTab: Button

    private lateinit var tvTotalSales: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvAvgOrder: TextView
    private lateinit var tvNetProfit: TextView

    private lateinit var frameSales: View
    private lateinit var frameInventory: View
    private lateinit var frameCustomers: View
    private lateinit var frameFinancial: View

    private var currentTimeframe = "This Month"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_reports, container, false)

        // Initialize views
        initializeViews(view)

        // Set up click listeners
        setupClickListeners()

        // Load initial data
        loadReportData(currentTimeframe)

        // Show sales frame by default
        showFrame("sales")

        return view
    }

    private fun initializeViews(view: View) {
        // Initialize timeframe button
        btnTimeframe = view.findViewById(R.id.btnTimeframe)

        // Initialize statistics TextViews
        tvTotalSales = view.findViewById(R.id.tvTotalSales)
        tvTotalOrders = view.findViewById(R.id.tvTotalOrders)
        tvAvgOrder = view.findViewById(R.id.tvAvgOrder)
        tvNetProfit = view.findViewById(R.id.tvNetProfit)

        // Initialize tab buttons
        btnSalesTab = view.findViewById(R.id.btnSalesTab)
        btnInventoryTab = view.findViewById(R.id.btnInventoryTab)
        btnCustomersTab = view.findViewById(R.id.btnCustomersTab)
        btnFinancialTab = view.findViewById(R.id.btnFinancialTab)

        // Initialize frames
        frameSales = view.findViewById(R.id.frameSales)
        frameInventory = view.findViewById(R.id.frameInventory)
        frameCustomers = view.findViewById(R.id.frameCustomers)
        frameFinancial = view.findViewById(R.id.frameFinancial)
    }

    private fun setupClickListeners() {
        // Timeframe button
        btnTimeframe.setOnClickListener { showTimeframeMenu() }

        // Tab buttons
        btnSalesTab.setOnClickListener { showFrame("sales") }
        btnInventoryTab.setOnClickListener { showFrame("inventory") }
        btnCustomersTab.setOnClickListener { showFrame("customers") }
        btnFinancialTab.setOnClickListener { showFrame("financial") }
    }

    private fun showFrame(frameType: String) {
        // Hide all frames first
        frameSales.visibility = View.GONE
        frameInventory.visibility = View.GONE
        frameCustomers.visibility = View.GONE
        frameFinancial.visibility = View.GONE

        // Reset all tab button colors
        val grayColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val blackColor = ContextCompat.getColor(requireContext(), android.R.color.black)

        btnSalesTab.setTextColor(grayColor)
        btnInventoryTab.setTextColor(grayColor)
        btnCustomersTab.setTextColor(grayColor)
        btnFinancialTab.setTextColor(grayColor)

        // Show selected frame and highlight button
        when (frameType) {
            "sales" -> {
                frameSales.visibility = View.VISIBLE
                btnSalesTab.setTextColor(blackColor)
            }
            "inventory" -> {
                frameInventory.visibility = View.VISIBLE
                btnInventoryTab.setTextColor(blackColor)
            }
            "customers" -> {
                frameCustomers.visibility = View.VISIBLE
                btnCustomersTab.setTextColor(blackColor)
            }
            "financial" -> {
                frameFinancial.visibility = View.VISIBLE
                btnFinancialTab.setTextColor(blackColor)
            }
        }
    }

    private fun showTimeframeMenu() {
        val popup = PopupMenu(requireContext(), btnTimeframe)

        // Add menu items
        popup.menu.add("Today")
        popup.menu.add("This Week")
        popup.menu.add("This Month")
        popup.menu.add("Last Month")
        popup.menu.add("This Quarter")
        popup.menu.add("This Year")
        popup.menu.add("Custom Range")

        // Set menu item click listener
        popup.setOnMenuItemClickListener { item ->
            currentTimeframe = item.title.toString()
            btnTimeframe.text = currentTimeframe
            loadReportData(currentTimeframe)
            true
        }

        popup.show()
    }

    private fun loadReportData(timeframe: String) {
        // Update data based on timeframe
        when (timeframe) {
            "Today" -> updateStats("$1,850", "22", "$84", "$520")
            "This Week" -> updateStats("$8,450", "98", "$86", "$2,380")
            "This Month" -> updateStats("$12,350", "159", "$78", "$3,680")
            "Last Month" -> updateStats("$11,200", "145", "$77", "$3,150")
            "This Quarter" -> updateStats("$35,800", "462", "$77", "$10,740")
            "This Year" -> updateStats("$142,500", "1845", "$77", "$42,750")
            else -> updateStats("$12,350", "159", "$78", "$3,680")
        }
    }

    private fun updateStats(
        totalSales: String,
        totalOrders: String,
        avgOrder: String,
        netProfit: String
    ) {
        tvTotalSales.text = totalSales
        tvTotalOrders.text = totalOrders
        tvAvgOrder.text = avgOrder
        tvNetProfit.text = netProfit
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReportsFragment()
    }
}