package com.example.inventorymanagement.dataclass

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0, // Auto-ID for phone
    val total_amount: Double,
    val tax_amount: Double,
    val customer_id: Int?, // Nullable for cash sales
    val created_at: String,

    // Sync Status: 0 = Pending Upload, 1 = Synced with Server
    var is_synced: Int = 0
)