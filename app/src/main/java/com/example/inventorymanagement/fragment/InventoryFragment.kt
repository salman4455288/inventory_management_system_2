package com.example.inventorymanagement.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
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
import com.example.inventorymanagement.R
import com.example.inventorymanagement.activity.AddProductActivity
import com.example.inventorymanagement.adapter.InventoryAdapter
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

class InventoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InventoryAdapter

    // Two lists: Master list (all data) and Display list (filtered data)
    private var allProducts = mutableListOf<Product>()
    private var displayedProducts = mutableListOf<Product>()

    private val BASE_URL: String by lazy { BaseURL.getUrl(requireContext()) }

    // Dashboard Views
    private lateinit var tvTotalProducts: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvTotalCategories: TextView
    private lateinit var cardLowStockAlert: LinearLayout
    private lateinit var tvLowStockMessage: TextView
    private lateinit var llCategoryContainer: LinearLayout

    // Track selected filter button
    private var selectedCategoryButton: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)

        initViews(view)

        // Setup Recycler
        recyclerView.layoutManager = LinearLayoutManager(context)

        // --- INITIALIZE ADAPTER WITH EDIT CLICK LISTENER ---
        // When user clicks the "Pencil" icon, this block runs
        adapter = InventoryAdapter(displayedProducts) { product ->
            showUpdateStockDialog(product)
        }
        recyclerView.adapter = adapter

        // Add Product Button Logic
        val btnAddProduct = view.findViewById<LinearLayout>(R.id.btnAddProduct)
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
    }

    override fun onResume() {
        super.onResume()
        // Refresh data whenever the screen appears (e.g. coming back from Add Product)
        fetchProducts()
    }

    // --- 1. SHOW POPUP DIALOG ---
    private fun showUpdateStockDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Update Stock: ${product.name}")

        // Create Input Field
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter new quantity"
        input.setText(product.stock_qty.toString()) // Pre-fill with current stock

        // Add padding around the input
        val container = android.widget.FrameLayout(requireContext())
        val params = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(50, 20, 50, 20)
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)

        // Buttons
        builder.setPositiveButton("Update") { _, _ ->
            val newQtyStr = input.text.toString().trim()
            if (newQtyStr.isNotEmpty()) {
                val newQty = newQtyStr.toInt()
                // Call API to update database
                updateStockInServer(product.id, newQty)
                Toast.makeText(context, "Updated Successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Quantity cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    // --- 2. CALL PHP API TO UPDATE ---
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
                writer.write(postData)
                writer.flush()
                writer.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            Toast.makeText(context, "Stock Updated Successfully!", Toast.LENGTH_SHORT).show()
                            // Refresh list to update UI and Low Stock Stats
                            fetchProducts()
                        } else {
                            Toast.makeText(context, json.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- 3. FETCH ALL PRODUCTS ---
    private fun fetchProducts() {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        if (apiToken.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_products.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData)
                writer.flush()
                writer.close()

                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    parseProductResponse(response)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- 4. PARSE DATA & UPDATE DASHBOARD ---
    private fun parseProductResponse(response: String) {
        try {
            val json = JSONObject(response)
            if (!json.getBoolean("error")) {
                allProducts.clear()
                val array = json.getJSONArray("products")

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    val supplier = if (obj.has("supplier") && !obj.isNull("supplier")) obj.getString("supplier") else "N/A"
                    val imgUrl = if (obj.has("image_url") && !obj.isNull("image_url")) obj.getString("image_url") else null

                    allProducts.add(
                        Product(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            sku = obj.getString("sku"),
                            category = obj.getString("category"),
                            stock_qty = obj.getInt("stock_qty"),
                            min_stock = obj.getInt("min_stock"),
                            cost_price = obj.getDouble("cost_price"),
                            sale_price = obj.getDouble("sale_price"),
                            supplier = supplier,
                            image_url = imgUrl
                        )
                    )
                }

                // Show all products initially
                displayedProducts.clear()
                displayedProducts.addAll(allProducts)
                adapter.updateData(displayedProducts)

                // Update Stats
                updateDashboardStats()
                // Update Category Tabs
                generateCategoryTabs()

            } else {
                Toast.makeText(context, json.getString("message"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateDashboardStats() {
        val totalCount = allProducts.size
        tvTotalProducts.text = totalCount.toString()

        val lowStockCount = allProducts.count { it.stock_qty <= it.min_stock }
        tvLowStock.text = lowStockCount.toString()

        val categoryCount = allProducts.map { it.category }.distinct().count()
        tvTotalCategories.text = categoryCount.toString()

        // Hide/Show Alert Box
        if (lowStockCount > 0) {
            cardLowStockAlert.visibility = View.VISIBLE
            tvLowStockMessage.text = "$lowStockCount product(s) are running low on stock"
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
                selectedCategoryButton = btn
            } else {
                styleUnselectedButton(btn)
            }

            btn.setOnClickListener {
                selectedCategoryButton?.let { styleUnselectedButton(it) }
                styleSelectedButton(btn)
                selectedCategoryButton = btn
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