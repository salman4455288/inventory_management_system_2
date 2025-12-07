package com.example.inventorymanagement.dataclass

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey val id: Int, // Remote ID from MySQL
    val name: String,
    val sku: String,
    val barcode: String?,
    val category: String,
    val stock_qty: Int,
    val min_stock: Int,
    val cost_price: Double,
    val sale_price: Double,
    val supplier: String?,
    val image_url: String?,

    // Sync Flag: 0 = Pending Upload, 1 = Synced
    val is_synced: Int = 1
)