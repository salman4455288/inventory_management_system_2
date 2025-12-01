package com.example.inventorymanagement.dataclass

data class roduct(
    val name: String,
    val sku: String,
    val category: String,
    val stock: Int,
    val minStock: Int,
    val cost: Double,
    val price: Double,
    val supplier: String,
    val status: String,
    val stockPercent: Int
)