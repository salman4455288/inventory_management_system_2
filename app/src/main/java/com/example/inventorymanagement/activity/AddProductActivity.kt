package com.example.inventorymanagement.activity

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.inventorymanagement.R

class AddProductActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var imgProduct: ImageView
    private lateinit var btnChooseImage: Button
    private lateinit var etProductName: EditText
    private lateinit var etSku: EditText
    private lateinit var btnGenerate: Button
    private lateinit var etBarcode: EditText
    private lateinit var btnScanBarcode: ImageView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etDescription: EditText
    private lateinit var etCostPrice: EditText
    private lateinit var etSellingPrice: EditText

    private lateinit var etCurrentStock: EditText
    private lateinit var etMinStock: EditText
    private lateinit var spinnerSupplier: Spinner
    private lateinit var btnSaveProduct: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Initialize views
        initViews()

        // Setup spinners
        setupSpinners()

        // Set click listeners
        setupClickListeners()
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
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etDescription = findViewById(R.id.etDescription)
        etCostPrice = findViewById(R.id.etCostPrice)
        etSellingPrice = findViewById(R.id.etSellingPrice)
        etCurrentStock = findViewById(R.id.etCurrentStock)
        etMinStock = findViewById(R.id.etMinStock)
        spinnerSupplier = findViewById(R.id.spinnerSupplier)
        btnSaveProduct = findViewById(R.id.btnSaveProduct)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupSpinners() {
        // Category spinner
        val categories = arrayOf("Select category", "Electronics", "Clothing", "Food", "Beverages", "Others")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Supplier spinner
        val suppliers = arrayOf("Select supplier", "Supplier 1", "Supplier 2", "Supplier 3")
        val supplierAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, suppliers)
        supplierAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSupplier.adapter = supplierAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnChooseImage.setOnClickListener {
            // TODO: Implement image picker
            Toast.makeText(this, "Image picker not implemented yet", Toast.LENGTH_SHORT).show()
        }

        btnGenerate.setOnClickListener {
            generateSKU()
        }

        btnScanBarcode.setOnClickListener {
            // TODO: Implement barcode scanner
            Toast.makeText(this, "Barcode scanner not implemented yet", Toast.LENGTH_SHORT).show()
        }

        btnSaveProduct.setOnClickListener {
            if (validateInputs()) {
                saveProduct()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun generateSKU() {
        val sku = "PRD${System.currentTimeMillis()}"
        etSku.setText(sku)
    }

    private fun validateInputs(): Boolean {
        // Product Name validation
        if (etProductName.text.toString().trim().isEmpty()) {
            etProductName.error = "Product name is required"
            etProductName.requestFocus()
            return false
        }

        // SKU validation
        if (etSku.text.toString().trim().isEmpty()) {
            etSku.error = "SKU is required"
            etSku.requestFocus()
            return false
        }

        // Category validation
        if (spinnerCategory.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return false
        }

        // Cost Price validation
        if (etCostPrice.text.toString().trim().isEmpty()) {
            etCostPrice.error = "Cost price is required"
            etCostPrice.requestFocus()
            return false
        }

        val costPrice = etCostPrice.text.toString().toDoubleOrNull()
        if (costPrice == null || costPrice < 0) {
            etCostPrice.error = "Enter valid cost price"
            etCostPrice.requestFocus()
            return false
        }

        // Selling Price validation
        if (etSellingPrice.text.toString().trim().isEmpty()) {
            etSellingPrice.error = "Selling price is required"
            etSellingPrice.requestFocus()
            return false
        }

        val sellingPrice = etSellingPrice.text.toString().toDoubleOrNull()
        if (sellingPrice == null || sellingPrice < 0) {
            etSellingPrice.error = "Enter valid selling price"
            etSellingPrice.requestFocus()
            return false
        }

        // Current Stock validation
        if (etCurrentStock.text.toString().trim().isEmpty()) {
            etCurrentStock.error = "Current stock is required"
            etCurrentStock.requestFocus()
            return false
        }

        val currentStock = etCurrentStock.text.toString().toIntOrNull()
        if (currentStock == null || currentStock < 0) {
            etCurrentStock.error = "Enter valid stock quantity"
            etCurrentStock.requestFocus()
            return false
        }

        // Minimum Stock validation
        if (etMinStock.text.toString().trim().isEmpty()) {
            etMinStock.error = "Minimum stock is required"
            etMinStock.requestFocus()
            return false
        }

        val minStock = etMinStock.text.toString().toIntOrNull()
        if (minStock == null || minStock < 0) {
            etMinStock.error = "Enter valid minimum stock"
            etMinStock.requestFocus()
            return false
        }

        return true
    }

    private fun saveProduct() {
        // Get all values
        val productName = etProductName.text.toString().trim()
        val sku = etSku.text.toString().trim()
        val barcode = etBarcode.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val description = etDescription.text.toString().trim()
        val costPrice = etCostPrice.text.toString().toDouble()
        val sellingPrice = etSellingPrice.text.toString().toDouble()
        val currentStock = etCurrentStock.text.toString().toInt()
        val minStock = etMinStock.text.toString().toInt()
        val supplier = spinnerSupplier.selectedItem.toString()

        // TODO: Save to database or send to API
        Toast.makeText(this, "Product saved successfully!", Toast.LENGTH_SHORT).show()

        finish()
    }
}