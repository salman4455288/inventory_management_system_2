package com.example.inventorymanagement.dataclass

data class CartItem(
    val id: Int,
    val name: String,
    val price: Double,
    var quantity: Int,
    var total: Double
)