package com.example.inventorymanagement.dataclass

data class Product(
    val id: Int,
    val name: String,
    val sku: String,
    val category: String,
    val stock_qty: Int,
    val min_stock: Int,
    val cost_price: Double,
    val sale_price: Double,
    val supplier: String?,
    val image_url: String?
)