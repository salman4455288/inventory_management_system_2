package com.example.inventorymanagement.dataclass

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: Int,
    val name: String,
    val phone: String,
    val outstanding: Double,
    val total_purchase: Double,
    val last_purchase: String?,

    // Sync Flag: 0 = Pending Upload, 1 = Synced
    val is_synced: Int = 1
)