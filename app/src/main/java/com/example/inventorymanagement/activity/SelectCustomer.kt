package com.example.inventorymanagement.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
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

class SelectCustomer : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SelectAdapter
    private var allCustomers = mutableListOf<Customer>()
    private val BASE_URL: String by lazy { BaseURL.getUrl(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_customer)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerSelectCustomer)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SelectAdapter(mutableListOf()) { customer ->
            val resultIntent = Intent()
            resultIntent.putExtra("CUST_NAME", customer.name)
            resultIntent.putExtra("CUST_PHONE", customer.phone)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        recyclerView.adapter = adapter

        val searchBar = findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        fetchCustomers()
    }

    private fun filter(query: String) {
        val lower = query.lowercase().trim()
        val filtered = if (lower.isEmpty()) allCustomers else {
            allCustomers.filter {
                it.name.lowercase().contains(lower) || it.phone.contains(lower)
            }
        }
        adapter.updateList(filtered)
    }

    private fun fetchCustomers() {
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
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
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            allCustomers.clear()
                            val array = json.getJSONArray("customers")
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                allCustomers.add(Customer(
                                    obj.getInt("id"),
                                    obj.getString("name"),
                                    obj.getString("phone"),
                                    0.0, 0.0, null
                                ))
                            }
                            adapter.updateList(allCustomers)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- INTERNAL ADAPTER ---
    class SelectAdapter(
        private var list: List<Customer>,
        private val onClick: (Customer) -> Unit
    ) : RecyclerView.Adapter<SelectAdapter.Holder>() {

        class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val tvInitials: TextView = v.findViewById(R.id.tvInitials)
            val tvName: TextView = v.findViewById(R.id.tvName)
            val tvPhone: TextView = v.findViewById(R.id.tvPhone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_select_customer, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = list[position]
            holder.tvName.text = item.name
            holder.tvPhone.text = item.phone
            holder.tvInitials.text = if (item.name.isNotEmpty()) item.name.take(1).uppercase() else "?"

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = list.size
        fun updateList(l: List<Customer>) { list = l; notifyDataSetChanged() }
    }
}