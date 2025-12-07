package com.example.inventorymanagement.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.InventoryApp
import com.example.inventorymanagement.R
import com.example.inventorymanagement.activity.AddProductActivity
import com.example.inventorymanagement.adapter.InventoryAdapter
import com.example.inventorymanagement.dataclass.Product
import com.example.inventorymanagement.util.BaseURL
import com.example.inventorymanagement.util.NotificationHelper
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

class InventoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InventoryAdapter

    // We no longer rely solely on memory lists; we lean on the Database
    private var allProducts = mutableListOf<Product>()
    private var displayedProducts = mutableListOf<Product>()

    private val BASE_URL: String by lazy { BaseURL.getUrl(requireContext()) }

    // Database Reference
    private val inventoryDao by lazy { (requireActivity().application as InventoryApp).database.inventoryDao() }

    // Dashboard Views
    private lateinit var tvTotalProducts: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvTotalCategories: TextView
    private lateinit var cardLowStockAlert: LinearLayout
    private lateinit var tvLowStockMessage: TextView
    private lateinit var llCategoryContainer: LinearLayout
    private lateinit var etSearchBar: EditText

    private var selectedCategoryButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)
        initViews(view)

        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = InventoryAdapter(displayedProducts) { product ->
            showUpdateStockDialog(product)
        }
        recyclerView.adapter = adapter

        setupSearchBar()

        val btnAddProduct = view.findViewById<View>(R.id.btnAddProduct)
        btnAddProduct.setOnClickListener {
            val intent = Intent(context, AddProductActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun initViews(view: View) {
        tvTotalProducts = view.findViewById(R.id.tvTotalProducts)
        tvLowStock = view.findViewById(R.id.tvLowStock)
        tvTotalCategories = view.findViewById(R.id.tvTotalCategories)
        cardLowStockAlert = view.findViewById(R.id.cardLowStockAlert)
        tvLowStockMessage = view.findViewById(R.id.tvLowStockMessage)
        llCategoryContainer = view.findViewById(R.id.llCategoryContainer)
        recyclerView = view.findViewById(R.id.recyclerInventory)
        etSearchBar = view.findViewById(R.id.searchBar)
    }

    override fun onResume() {
        super.onResume()
        // 1. Load Local Data FIRST (Instant)
        loadFromLocalDatabase()
        // 2. Then try to Sync with Server (Background)
        fetchProductsFromServer()
    }

    // --- NEW: OFFLINE FIRST LOGIC ---
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

    // --- SEARCH LOGIC ---
    private fun setupSearchBar() {
        etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterListByQuery(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterListByQuery(query: String) {
        displayedProducts.clear()
        if (query.isEmpty()) {
            displayedProducts.addAll(allProducts)
        } else {
            val lowerCaseQuery = query.lowercase()
            val filteredList = allProducts.filter { product ->
                product.name.lowercase().contains(lowerCaseQuery) ||
                        (product.barcode != null && product.barcode.contains(lowerCaseQuery)) ||
                        product.category.lowercase().contains(lowerCaseQuery) ||
                        (product.supplier?.lowercase()?.contains(lowerCaseQuery) == true)
            }
            displayedProducts.addAll(filteredList)
        }
        adapter.updateData(displayedProducts)
    }

    // --- DIALOGS ---
    private fun showUpdateStockDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update Stock: ${product.name}")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter new quantity"
        input.setText(product.stock_qty.toString())

        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        builder.setPositiveButton("Update") { _, _ ->
            val newQtyStr = input.text.toString().trim()
            if (newQtyStr.isNotEmpty()) {
                updateStockInServer(product.id, newQtyStr.toInt())
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // --- API CALLS ---
    private fun updateStockInServer(productId: Int, newStock: Int) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "update_stock.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") +
                        "&product_id=" + productId +
                        "&new_stock=" + newStock

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData); writer.flush(); writer.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            Toast.makeText(context, "Stock Updated!", Toast.LENGTH_SHORT).show()
                            fetchProductsFromServer() // Refresh
                        } else {
                            Toast.makeText(context, json.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // OFFLINE LOGIC: We should ideally update local DB here too,
                    // but for simplicity, we just show error
                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchProductsFromServer() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        if (apiToken.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_products.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData); writer.flush(); writer.close()

                val response = conn.inputStream.bufferedReader().readText()

                // Parse and Save to DB
                parseAndSaveResponse(response)

            } catch (e: Exception) {
                e.printStackTrace()
                // If network fails, we do nothing because we already loaded local data in onResume
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

                    val supplier = if (obj.has("supplier") && !obj.isNull("supplier")) obj.getString("supplier") else "N/A"
                    val imgUrl = if (obj.has("image_url") && !obj.isNull("image_url")) obj.getString("image_url") else null
                    val barcode = if (obj.has("barcode") && !obj.isNull("barcode")) obj.getString("barcode") else null

                    newProducts.add(
                        Product(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            sku = obj.getString("sku"),
                            barcode = barcode,
                            category = obj.getString("category"),
                            stock_qty = obj.getInt("stock_qty"),
                            min_stock = obj.getInt("min_stock"),
                            cost_price = obj.getDouble("cost_price"),
                            sale_price = obj.getDouble("sale_price"),
                            supplier = supplier,
                            image_url = imgUrl,
                            is_synced = 1 // Coming from server, so it is synced
                        )
                    )
                }

                // SAVE TO LOCAL DATABASE
                inventoryDao.insertProducts(newProducts)

                // UPDATE UI
                withContext(Dispatchers.Main) {
                    updateUI(newProducts)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateUI(products: List<Product>) {
        allProducts.clear()
        allProducts.addAll(products)

        displayedProducts.clear()
        displayedProducts.addAll(allProducts)
        adapter.updateData(displayedProducts)

        updateDashboardStats()
        generateCategoryTabs()
    }

    private fun updateDashboardStats() {
        val totalCount = allProducts.size
        tvTotalProducts.text = totalCount.toString()

        val lowStockCount = allProducts.count { it.stock_qty <= it.min_stock }
        tvLowStock.text = lowStockCount.toString()

        val categoryCount = allProducts.map { it.category }.distinct().count()
        tvTotalCategories.text = categoryCount.toString()

        if (lowStockCount > 0) {
            cardLowStockAlert.visibility = View.VISIBLE
            tvLowStockMessage.text = "$lowStockCount product(s) are running low on stock"

            // Only alert if this is a fresh check (optional logic)
            // NotificationHelper.showLowStockNotification(requireContext(), lowStockCount)
        } else {
            cardLowStockAlert.visibility = View.GONE
        }
    }

    private fun generateCategoryTabs() {
        llCategoryContainer.removeAllViews()
        val categories = allProducts.map { it.category }.distinct().sorted().toMutableList()
        categories.add(0, "All Categories")

        for (cat in categories) {
            val btn = Button(context)
            btn.text = cat
            btn.textSize = 13f
            btn.isAllCaps = false

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                100
            )
            params.setMargins(0, 0, 24, 0)
            btn.layoutParams = params
            btn.setPadding(40, 0, 40, 0)

            if (cat == "All Categories") {
                styleSelectedButton(btn)
                // selectedCategoryButton = btn
            } else {
                styleUnselectedButton(btn)
            }

            btn.setOnClickListener {
                // styleSelectedButton(btn)
                filterProductsByCategory(cat)
            }
            llCategoryContainer.addView(btn)
        }
    }

    private fun filterProductsByCategory(category: String) {
        displayedProducts.clear()
        if (category == "All Categories") {
            displayedProducts.addAll(allProducts)
        } else {
            val filtered = allProducts.filter { it.category == category }
            displayedProducts.addAll(filtered)
        }
        adapter.updateData(displayedProducts)
    }

    private fun styleSelectedButton(btn: Button) {
        btn.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        btn.setTextColor(Color.WHITE)
    }

    private fun styleUnselectedButton(btn: Button) {
        btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
        btn.setTextColor(Color.BLACK)
    }

    companion object {
        @JvmStatic
        fun newInstance() = InventoryFragment()
    }
}