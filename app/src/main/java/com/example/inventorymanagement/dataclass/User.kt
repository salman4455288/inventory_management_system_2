package com.example.inventorymanagement.dataclass

data class User(
    val id: Int = 0,
    val fullName: String,
    val businessName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val profilePicture: String? = null // Will store the filename
)