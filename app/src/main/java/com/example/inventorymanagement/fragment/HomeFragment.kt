package com.example.inventorymanagement.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.activity.MainActivity
import com.example.inventorymanagement.activity.settingsActivity
import com.example.inventorymanagement.adapter.RecentTransactionsAdapter
import com.example.inventorymanagement.util.BaseURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTodaySales: TextView
    private lateinit var tvTotalStock: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvTodayExpenses: TextView
    private lateinit var recyclerTransactions: RecyclerView
    private lateinit var adapter: RecentTransactionsAdapter
    private var transactionList = mutableListOf<JSONObject>()

    private val BASE_URL: String by lazy { BaseURL.getUrl(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 1. Init Views
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvDate = view.findViewById(R.id.tvDate)
        tvTodaySales = view.findViewById(R.id.tvTodaySales)
        tvTotalStock = view.findViewById(R.id.tvTotalStock)
        tvLowStock = view.findViewById(R.id.tvLowStock)
        tvTodayExpenses = view.findViewById(R.id.tvTodayExpenses)

        // 2. Setup Recycler
        recyclerTransactions = view.findViewById(R.id.transactions_recycler)
        recyclerTransactions.layoutManager = LinearLayoutManager(context)
        adapter = RecentTransactionsAdapter(transactionList)
        recyclerTransactions.adapter = adapter

        // 3. Set Date
        val currentDate = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(Date())
        tvDate.text = currentDate

        // 4. Navigation
        setupNavigation(view)

        // 5. Header Settings
        view.findViewById<LinearLayout>(R.id.settings_btn).setOnClickListener {
            startActivity(Intent(context, settingsActivity::class.java))
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_home_stats.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData); writer.flush(); writer.close()

                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            // Update UI
                            tvGreeting.text = "Good Morning, ${json.getString("user_name")}"
                            tvTodaySales.text = "$" + json.getString("today_sales")
                            tvTotalStock.text = json.getString("total_items")
                            tvLowStock.text = json.getString("low_stock")
                            tvTodayExpenses.text = "$" + json.getString("today_expenses")

                            // Update Transactions
                            transactionList.clear()
                            val array = json.getJSONArray("transactions")
                            for(i in 0 until array.length()) {
                                transactionList.add(array.getJSONObject(i))
                            }
                            adapter.updateList(transactionList)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun setupNavigation(view: View) {
        val mainActivity = activity as? MainActivity ?: return

        view.findViewById<LinearLayout>(R.id.btn_pos).setOnClickListener {
            mainActivity.loadFragment(PosFragment())
            mainActivity.highlightTab(R.id.nav_pos)
        }
        view.findViewById<LinearLayout>(R.id.btn_inventory).setOnClickListener {
            mainActivity.loadFragment(InventoryFragment())
            mainActivity.highlightTab(R.id.nav_inventory)
        }
        view.findViewById<LinearLayout>(R.id.btn_customers).setOnClickListener {
            mainActivity.loadFragment(CustomersFragment())
            mainActivity.highlightTab(R.id.nav_customers)
        }
        view.findViewById<LinearLayout>(R.id.btn_reports).setOnClickListener {
            mainActivity.loadFragment(ReportsFragment())
            mainActivity.highlightTab(R.id.nav_reports)
        }
        // Add listeners for suppliers/expenses if you implement them
    }
}