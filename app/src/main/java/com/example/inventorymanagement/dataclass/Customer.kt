package com.example.inventorymanagement.dataclass

data class Customer(
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val outstanding: Double,
    val totalPurchases: Double,
    val lastPurchase: String
)
