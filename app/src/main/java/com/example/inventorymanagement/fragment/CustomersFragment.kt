package com.example.inventorymanagement.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.inventorymanagement.R
import com.example.inventorymanagement.adaptor.CustomerAdapter
import com.example.inventorymanagement.dataclass.Customer
import com.example.inventorymanagement.util.BaseURL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomerAdapter
    private var allCustomers = mutableListOf<Customer>()
    private var displayedCustomers = mutableListOf<Customer>()
    private val BASE_URL: String by lazy { BaseURL.getUrl(requireContext()) }

    // Views
    private lateinit var tvTotalCustomers: TextView
    private lateinit var tvActiveCustomers: TextView
    private lateinit var tvNewCustomers: TextView
    private lateinit var etSearchBar: EditText

    // UI State Views
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmptyState: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customers, container, false)

        // Init Views
        recyclerView = view.findViewById(R.id.recyclerCustomers)
        tvTotalCustomers = view.findViewById(R.id.tvTotalCustomers)
        tvActiveCustomers = view.findViewById(R.id.tvActiveCustomers)
        tvNewCustomers = view.findViewById(R.id.tvNewCustomers)
        etSearchBar = view.findViewById(R.id.searchBar)

        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // --- ADAPTER INITIALIZATION (With 3 Callbacks) ---
        adapter = CustomerAdapter(displayedCustomers,
            onCustomerClick = { customer -> showEditDialog(customer) },
            onCallClick = { phone -> dialPhoneNumber(phone) },
            onViewBillsClick = { customer -> fetchCustomerBills(customer) } // Added History Callback
        )
        recyclerView.adapter = adapter

        setupSearchBar()

        // Pull to Refresh
        swipeRefresh.setOnRefreshListener {
            fetchCustomers()
        }

        val btnAddCustomer = view.findViewById<View>(R.id.btnAddCustomer)
        btnAddCustomer.setOnClickListener { showAddCustomerDialog() }

        return view
    }

    override fun onResume() {
        super.onResume()
        if (allCustomers.isEmpty()) {
            progressBar.visibility = View.VISIBLE
        }
        fetchCustomers()
    }

    // --- 1. DATA FETCHING ---
    private fun fetchCustomers() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_customers.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    progressBar.visibility = View.GONE
                    parseCustomerResponse(response)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    swipeRefresh.isRefreshing = false
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun parseCustomerResponse(response: String) {
        try {
            val json = JSONObject(response)
            if (!json.getBoolean("error")) {
                allCustomers.clear()
                val array = json.getJSONArray("customers")
                val dbFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                var newCustomersCount = 0

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val createdAt = obj.getString("created_at")
                    try { if (SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(dbFormat.parse(createdAt)!!) == currentMonth) newCustomersCount++ } catch (e: Exception) { }

                    allCustomers.add(Customer(
                        id = obj.getInt("id"),
                        name = obj.getString("name"),
                        phone = obj.getString("phone"),
                        outstanding = obj.getDouble("outstanding_balance"),
                        total_purchase = obj.getDouble("total_spent"),
                        last_purchase = if(obj.has("last_purchase") && !obj.isNull("last_purchase")) obj.getString("last_purchase") else null
                    ))
                }

                // Update List and UI
                displayedCustomers.clear()
                displayedCustomers.addAll(allCustomers)
                adapter.updateList(displayedCustomers)

                updateEmptyState()

                tvTotalCustomers.text = allCustomers.size.toString()
                tvActiveCustomers.text = allCustomers.count { it.total_purchase > 0.0 }.toString()
                tvNewCustomers.text = newCustomersCount.toString()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // --- 2. SEARCH LOGIC ---
    private fun setupSearchBar() {
        etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        displayedCustomers.clear()
        if (query.isEmpty()) {
            displayedCustomers.addAll(allCustomers)
        } else {
            val lowerCaseQuery = query.lowercase()
            val filtered = allCustomers.filter { customer ->
                customer.name.lowercase().contains(lowerCaseQuery) ||
                        customer.phone.contains(lowerCaseQuery)
            }
            displayedCustomers.addAll(filtered)
        }
        adapter.updateList(displayedCustomers)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (displayedCustomers.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // --- 3. CUSTOMER ACTIONS (Add, Edit, Call) ---
    private fun showAddCustomerDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("New Customer")
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 20, 50, 0)

        val inputName = EditText(requireContext()); inputName.hint = "Name"
        val inputPhone = EditText(requireContext()); inputPhone.hint = "Phone"; inputPhone.inputType = InputType.TYPE_CLASS_PHONE
        val inputAmount = EditText(requireContext()); inputAmount.hint = "Outstanding"
        inputAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        inputAmount.setText("0")

        container.addView(inputName); container.addView(inputPhone); container.addView(inputAmount)
        builder.setView(container)

        builder.setPositiveButton("Add") { _, _ ->
            val name = inputName.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val amount = inputAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty() && phone.isNotEmpty()) addCustomerToBackend(name, phone, amount)
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun addCustomerToBackend(name: String, phone: String, amount: Double) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "add_customer.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") + "&name=" + URLEncoder.encode(name, "UTF-8") + "&phone=" + URLEncoder.encode(phone, "UTF-8") + "&amount=" + amount
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()
                withContext(Dispatchers.Main) {
                    if (!JSONObject(response).getBoolean("error")) { fetchCustomers() }
                }
            } catch (e: Exception) { }
        }
    }

    private fun dialPhoneNumber(phoneNumber: String) {
        startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phoneNumber") })
    }

    private fun showEditDialog(customer: Customer) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Customer")
        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 20, 50, 0)

        val inputName = EditText(requireContext()); inputName.setText(customer.name)
        val inputPhone = EditText(requireContext()); inputPhone.setText(customer.phone)
        val inputAmount = EditText(requireContext()); inputAmount.setText(customer.outstanding.toString())
        inputAmount.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED

        container.addView(inputName); container.addView(inputPhone); container.addView(inputAmount)
        builder.setView(container)

        builder.setPositiveButton("Update") { _, _ ->
            val name = inputName.text.toString().trim()
            val phone = inputPhone.text.toString().trim()
            val amount = inputAmount.text.toString().toDoubleOrNull() ?: 0.0
            if (name.isNotEmpty() && phone.isNotEmpty()) performUpdate(customer.id, name, phone, amount)
        }
        builder.setNeutralButton("Delete") { _, _ ->
            AlertDialog.Builder(requireContext()).setTitle("Delete?").setPositiveButton("Yes") { _, _ -> performDelete(customer.id) }.setNegativeButton("No", null).show()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun performUpdate(id: Int, name: String, phone: String, amount: Double) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "update_customer.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") + "&id=$id" + "&name=" + URLEncoder.encode(name, "UTF-8") + "&phone=" + URLEncoder.encode(phone, "UTF-8") + "&outstanding=" + amount
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()
                withContext(Dispatchers.Main) { if (!JSONObject(response).getBoolean("error")) fetchCustomers() }
            } catch (e: Exception) { }
        }
    }

    private fun performDelete(id: Int) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "delete_customer.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") + "&id=$id"
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()
                withContext(Dispatchers.Main) { if (!JSONObject(response).getBoolean("error")) fetchCustomers() }
            } catch (e: Exception) { }
        }
    }

    // --- 4. HISTORY LOGIC (Fetching Bills) ---
    private fun fetchCustomerBills(customer: Customer) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_customer_sales.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") + "&customer_id=" + customer.id
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            val sales = json.getJSONArray("sales")
                            showBillsListDialog(customer.name, sales)
                        } else {
                            Toast.makeText(context, "No history found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun showBillsListDialog(customerName: String, sales: org.json.JSONArray) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("History: $customerName")

        val items = Array(sales.length()) { i ->
            val obj = sales.getJSONObject(i)
            "Date: ${obj.getString("created_at")}\nTotal: $${obj.getString("total_amount")}"
        }

        builder.setItems(items) { _, which ->
            val saleId = sales.getJSONObject(which).getInt("id")
            fetchBillDetails(saleId)
        }
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    private fun fetchBillDetails(saleId: Int) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_sale_details.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") + "&sale_id=" + saleId
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream); writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            val items = json.getJSONArray("items")
                            showBillDetailsDialog(items)
                        }
                    } catch (e: Exception) { }
                }
            } catch (e: Exception) { }
        }
    }

    private fun showBillDetailsDialog(items: org.json.JSONArray) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Bill Details")
        val sb = StringBuilder()
        var grandTotal = 0.0

        for (i in 0 until items.length()) {
            val obj = items.getJSONObject(i)
            val name = obj.getString("name")
            val qty = obj.getInt("quantity")
            val price = obj.getDouble("price_at_sale")
            val total = qty * price
            grandTotal += total
            sb.append("$name (x$qty)\n$$price  =  $$total\n\n")
        }
        sb.append("----------------\nTotal: $$grandTotal")
        builder.setMessage(sb.toString())
        builder.setPositiveButton("Close", null)
        builder.show()
    }
}