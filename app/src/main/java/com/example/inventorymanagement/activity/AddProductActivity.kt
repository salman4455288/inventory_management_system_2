package com.example.inventorymanagement.activity

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.inventorymanagement.R
import com.example.inventorymanagement.util.BaseURL
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.HashMap

class AddProductActivity : AppCompatActivity() {

    // --- VIEWS ---
    private lateinit var btnBack: ImageView
    private lateinit var imgProduct: ImageView
    private lateinit var btnChooseImage: Button
    private lateinit var etProductName: EditText
    private lateinit var etSku: EditText
    private lateinit var btnGenerate: Button
    private lateinit var etBarcode: EditText
    private lateinit var btnScanBarcode: ImageView
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var btnAddCategory: Button
    private lateinit var etDescription: EditText
    private lateinit var etCostPrice: EditText
    private lateinit var etSellingPrice: EditText
    private lateinit var etCurrentStock: EditText
    private lateinit var etMinStock: EditText
    private lateinit var actvSupplier: AutoCompleteTextView
    private lateinit var btnAddSupplier: Button
    private lateinit var btnSaveProduct: Button
    private lateinit var btnCancel: Button

    // --- DATA ---
    private var categoryList: ArrayList<String> = ArrayList()
    private var supplierList: ArrayList<String> = ArrayList()
    private var categoryAdapter: ArrayAdapter<String>? = null
    private var supplierAdapter: ArrayAdapter<String>? = null

    // --- IMAGE ---
    private var selectedBitmap: Bitmap? = null
    private val BASE_URL: String by lazy { BaseURL.getUrl(this) }

    // --- LAUNCHERS ---
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imgProduct.setImageURI(uri)
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) etBarcode.setText(result.contents)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_productPage)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupClickListeners()

        // Load data immediately
        fetchDropdowns()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        imgProduct = findViewById(R.id.imgProduct)
        btnChooseImage = findViewById(R.id.btnChooseImage)
        etProductName = findViewById(R.id.etProductName)
        etSku = findViewById(R.id.etSku)
        btnGenerate = findViewById(R.id.btnGenerate)
        etBarcode = findViewById(R.id.etBarcode)
        btnScanBarcode = findViewById(R.id.btnScanBarcode)
        actvCategory = findViewById(R.id.actvCategory)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        etDescription = findViewById(R.id.etDescription)
        etCostPrice = findViewById(R.id.etCostPrice)
        etSellingPrice = findViewById(R.id.etSellingPrice)
        etCurrentStock = findViewById(R.id.etCurrentStock)
        etMinStock = findViewById(R.id.etMinStock)
        actvSupplier = findViewById(R.id.actvSupplier)
        btnAddSupplier = findViewById(R.id.btnAddSupplier)
        btnSaveProduct = findViewById(R.id.btnSaveProduct)
        btnCancel = findViewById(R.id.btnCancel)
    }

    // --- HELPER: Set up adapter with a list ---
    private fun setCategoryAdapter(data: ArrayList<String>) {
        categoryList = data
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryList)
        actvCategory.setAdapter(categoryAdapter)
        actvCategory.threshold = 1 // Ensure filtering starts at 1 character

        // Show dropdown logic
        actvCategory.setOnClickListener { actvCategory.showDropDown() }
        actvCategory.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && actvCategory.text.isEmpty()) actvCategory.showDropDown()
        }
    }

    private fun setSupplierAdapter(data: ArrayList<String>) {
        supplierList = data
        supplierAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, supplierList)
        actvSupplier.setAdapter(supplierAdapter)
        actvSupplier.threshold = 1

        actvSupplier.setOnClickListener { actvSupplier.showDropDown() }
        actvSupplier.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && actvSupplier.text.isEmpty()) actvSupplier.showDropDown()
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }
        btnGenerate.setOnClickListener { generateSKU() }

        btnChooseImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnScanBarcode.setOnClickListener {
            val options = ScanOptions()
            options.setCaptureActivity(com.journeyapps.barcodescanner.CaptureActivity::class.java)
            barcodeLauncher.launch(options)
        }

        btnAddCategory.setOnClickListener {
            showAddItemDialog("Add New Category") { newItem ->
                if (!categoryList.contains(newItem)) {
                    categoryList.add(newItem)
                    // Re-set adapter to refresh filter capability
                    setCategoryAdapter(categoryList)
                    actvCategory.setText(newItem)
                    actvCategory.dismissDropDown()
                }
            }
        }

        btnAddSupplier.setOnClickListener {
            showAddItemDialog("Add New Supplier") { newItem ->
                if (!supplierList.contains(newItem)) {
                    supplierList.add(newItem)
                    setSupplierAdapter(supplierList)
                    actvSupplier.setText(newItem)
                    actvSupplier.dismissDropDown()
                }
            }
        }

        btnSaveProduct.setOnClickListener {
            if (validateInputs()) saveProduct()
        }
    }

    private fun fetchDropdowns() {
        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        if (apiToken.isEmpty()) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_dropdowns.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8")
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

                            val newCategories = ArrayList<String>()
                            val catArray = json.getJSONArray("categories")
                            for (i in 0 until catArray.length()) newCategories.add(catArray.getString(i))

                            val newSuppliers = ArrayList<String>()
                            val supArray = json.getJSONArray("suppliers")
                            for (i in 0 until supArray.length()) newSuppliers.add(supArray.getString(i))

                            // --- DEBUG TOAST: This will show you exactly what is loaded ---
                            Toast.makeText(applicationContext, "Loaded: ${newCategories.size} Categories, ${newSuppliers.size} Suppliers", Toast.LENGTH_LONG).show()

                            // Set Adapters with FRESH lists
                            setCategoryAdapter(newCategories)
                            setSupplierAdapter(newSuppliers)

                        } else {
                            Toast.makeText(applicationContext, "Server: " + json.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "JSON Error: $response", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Connection Failed", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun saveProduct() {
        val name = etProductName.text.toString().trim()
        val sku = etSku.text.toString().trim()
        val barcode = etBarcode.text.toString().trim()
        val category = actvCategory.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val costPrice = etCostPrice.text.toString().trim()
        val sellingPrice = etSellingPrice.text.toString().trim()
        val currentStock = etCurrentStock.text.toString().trim()
        val minStock = etMinStock.text.toString().trim()
        val supplier = actvSupplier.text.toString().trim()

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        val params = HashMap<String, String>()
        params["api_token"] = apiToken
        params["name"] = name
        params["sku"] = sku
        params["barcode"] = barcode
        params["category"] = category
        params["description"] = description
        params["cost_price"] = costPrice
        params["sale_price"] = sellingPrice
        params["stock_qty"] = currentStock
        params["min_stock"] = minStock
        params["supplier"] = supplier

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = multipartRequest(BASE_URL + "add_product.php", params, selectedBitmap, "image", "product_img.jpg")
                withContext(Dispatchers.Main) {
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            Toast.makeText(applicationContext, "Saved!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(applicationContext, json.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(applicationContext, "Response Error: $response", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "Net Error", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun multipartRequest(urlTo: String, params: Map<String, String>, bitmap: Bitmap?, fileField: String, fileName: String): String {
        val connection = URL(urlTo).openConnection() as HttpURLConnection
        val boundary = "*****" + System.currentTimeMillis() + "*****"
        connection.doInput = true; connection.doOutput = true; connection.useCaches = false
        connection.requestMethod = "POST"
        connection.setRequestProperty("Connection", "Keep-Alive")
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

        val outputStream = DataOutputStream(connection.outputStream)
        val lineEnd = "\r\n"; val twoHyphens = "--"

        for ((key, value) in params) {
            outputStream.writeBytes(twoHyphens + boundary + lineEnd)
            outputStream.writeBytes("Content-Disposition: form-data; name=\"$key\"$lineEnd")
            outputStream.writeBytes(lineEnd)
            outputStream.write(value.toByteArray(Charsets.UTF_8))
            outputStream.writeBytes(lineEnd)
        }

        if (bitmap != null) {
            outputStream.writeBytes(twoHyphens + boundary + lineEnd)
            outputStream.writeBytes("Content-Disposition: form-data; name=\"$fileField\";filename=\"$fileName\"$lineEnd")
            outputStream.writeBytes(lineEnd)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            outputStream.write(baos.toByteArray())
            outputStream.writeBytes(lineEnd)
        }

        outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
        outputStream.flush(); outputStream.close()

        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun showAddItemDialog(title: String, onAdd: (String) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT

        val container = android.widget.FrameLayout(this)
        val params = android.widget.FrameLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = 50; params.rightMargin = 50
        input.layoutParams = params
        container.addView(input)

        builder.setView(container)
        builder.setPositiveButton("Add") { _, _ ->
            if (input.text.toString().isNotEmpty()) onAdd(input.text.toString())
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun generateSKU() { etSku.setText("PRD${System.currentTimeMillis()}") }

    private fun validateInputs(): Boolean {
        if (etProductName.text.toString().isEmpty()) return false
        if (etSku.text.toString().isEmpty()) return false
        if (etCostPrice.text.toString().isEmpty()) return false
        if (etSellingPrice.text.toString().isEmpty()) return false
        if (etCurrentStock.text.toString().isEmpty()) return false
        if (etMinStock.text.toString().isEmpty()) return false
        return true
    }
}