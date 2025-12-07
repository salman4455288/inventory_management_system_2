package com.example.inventorymanagement.dataclass

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["localId"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE // If Sale deleted, delete items too
        )
    ]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long, // Links to the local Sale
    val product_id: Int,
    val quantity: Int,
    val price: Double
)