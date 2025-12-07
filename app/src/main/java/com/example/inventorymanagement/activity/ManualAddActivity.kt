package com.example.inventorymanagement.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.InventoryApp // Import your App class
import com.example.inventorymanagement.R
import com.example.inventorymanagement.adapter.ManualProductAdapter
import com.example.inventorymanagement.dataclass.Product
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

class ManualAddActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ManualProductAdapter
    private lateinit var searchBar: EditText

    // Lists for Local Filtering
    private var allProducts = mutableListOf<Product>()
    private var displayedProducts = mutableListOf<Product>()

    private val BASE_URL: String by lazy { BaseURL.getUrl(this) }

    // 1. Get Database Reference
    private val inventoryDao by lazy { (application as InventoryApp).database.inventoryDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual_add)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.manualAddPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        recyclerView = findViewById(R.id.recyclerProducts)
        searchBar = findViewById(R.id.searchBar)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize Adapter with empty list
        adapter = ManualProductAdapter(displayedProducts) { product ->
            val resultIntent = Intent()
            resultIntent.putExtra("PRODUCT_ID", product.id)
            resultIntent.putExtra("PRODUCT_NAME", product.name)
            resultIntent.putExtra("PRODUCT_PRICE", product.sale_price)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        recyclerView.adapter = adapter

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. Load Local Data FIRST (Offline Support)
        loadFromLocalDatabase()

        // 3. Try to Fetch Fresh Data (Background Sync)
        fetchProductsFromServer()
    }

    // --- OFFLINE LOGIC ---
    private fun loadFromLocalDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            val localList = inventoryDao.getAllProducts()

            withContext(Dispatchers.Main) {
                if (localList.isNotEmpty()) {
                    updateUI(localList)
                }
            }
        }
    }

    private fun filter(query: String) {
        val lowerQuery = query.lowercase().trim()
        displayedProducts.clear()

        if (lowerQuery.isEmpty()) {
            displayedProducts.addAll(allProducts)
        } else {
            val filtered = allProducts.filter {
                it.name.lowercase().contains(lowerQuery) ||
                        (it.barcode != null && it.barcode.contains(lowerQuery))
            }
            displayedProducts.addAll(filtered)
        }
        adapter.updateList(displayedProducts)
    }

    private fun fetchProductsFromServer() {
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_products.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                // Parse & Save to DB
                parseAndSaveResponse(response)
            } catch (e: Exception) {
                e.printStackTrace()
                // If network fails, we rely on local data loaded in onCreate
            }
        }
    }

    private suspend fun parseAndSaveResponse(response: String) {
        try {
            val json = JSONObject(response)
            if (!json.getBoolean("error")) {
                val newProducts = mutableListOf<Product>()
                val array = json.getJSONArray("products")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    val barcode = if(obj.has("barcode") && !obj.isNull("barcode")) obj.getString("barcode") else ""
                    val supplier = if(obj.has("supplier") && !obj.isNull("supplier")) obj.getString("supplier") else ""
                    val imageUrl = if(obj.has("image_url") && !obj.isNull("image_url")) obj.getString("image_url") else null

                    newProducts.add(Product(
                        obj.getInt("id"),
                        obj.getString("name"),
                        obj.getString("sku"),
                        barcode,
                        obj.getString("category"),
                        obj.getInt("stock_qty"),
                        obj.getInt("min_stock"),
                        obj.getDouble("cost_price"),
                        obj.getDouble("sale_price"),
                        supplier,
                        imageUrl,
                        is_synced = 1
                    ))
                }

                // Update Local DB
                inventoryDao.insertProducts(newProducts)

                // Update UI
                withContext(Dispatchers.Main) {
                    updateUI(newProducts)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateUI(products: List<Product>) {
        allProducts.clear()
        allProducts.addAll(products)

        // Re-apply filter if user already typed something
        val currentQuery = searchBar.text.toString()
        filter(currentQuery)
    }
}