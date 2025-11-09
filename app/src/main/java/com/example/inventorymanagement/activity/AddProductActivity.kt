package com.example.inventorymanagement.activity

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.inventorymanagement.R

class AddProductActivity : AppCompatActivity() {

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

        // Set click listeners
        btnChooseImage.setOnClickListener {
            // Handle image selection
        }

        btnGenerate.setOnClickListener {
            // Generate SKU
            val sku = "PRD${System.currentTimeMillis()}"
            etSku.setText(sku)
        }

        btnScanBarcode.setOnClickListener {
            // Handle barcode scanning
        }

        btnSaveProduct.setOnClickListener {
            // Save product
            finish()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }
}