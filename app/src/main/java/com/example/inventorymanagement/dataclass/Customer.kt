package com.example.inventorymanagement.dataclass

data class Customer(
    val id: Int,
    val name: String,
    val phone: String,
    val outstanding: Double,
    val total_purchase: Double,
    val last_purchase: String?
)