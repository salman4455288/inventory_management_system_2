package com.example.inventorymanagement.dataclass

data class CartItem(
    val name: String,
    val price: Double,
    var quantity: Int,
    val unitPrice: Double
)