package com.example.inventorymanagement.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
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
    private lateinit var btnSaveProduct: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        // Initialize views
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
        btnSaveProduct = findViewById(R.id.btnSaveProduct)
        btnCancel = findViewById(R.id.btnCancel)

        // Set click listeners
        btnBack.setOnClickListener {
            finish()
        }

        btnChooseImage.setOnClickListener {
            // Handle image selection
            // TODO: Implement image picker
        }

        btnGenerate.setOnClickListener {
            // Generate SKU
            val sku = "PRD${System.currentTimeMillis()}"
            etSku.setText(sku)
        }

        btnScanBarcode.setOnClickListener {
            // Handle barcode scanning
            // TODO: Implement barcode scanner
        }

        btnSaveProduct.setOnClickListener {
            // Validate and save product
            if (validateInputs()) {
                saveProduct()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        if (etProductName.text.toString().trim().isEmpty()) {
            etProductName.error = "Product name is required"
            return false
        }

        if (etSku.text.toString().trim().isEmpty()) {
            etSku.error = "SKU is required"
            return false
        }

        if (spinnerCategory.selectedItemPosition == 0) {
            // Assuming first position is placeholder
            // Show error for category selection
            return false
        }

        return true
    }

    private fun saveProduct() {
        // TODO: Implement save logic
        // Get values from all fields
        val productName = etProductName.text.toString().trim()
        val sku = etSku.text.toString().trim()
        val barcode = etBarcode.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Save to database or send to API

        finish()
    }
}