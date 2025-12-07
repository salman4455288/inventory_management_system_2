package com.example.inventorymanagement.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.inventorymanagement.dataclass.Customer
import com.example.inventorymanagement.dataclass.Product
import com.example.inventorymanagement.dataclass.Sale
import com.example.inventorymanagement.dataclass.SaleItem

// UPDATED: Version 2, Added Sale & SaleItem entities
@Database(entities = [Product::class, Customer::class, Sale::class, SaleItem::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "inventory_db"
                )
                    .fallbackToDestructiveMigration() // Wipe DB to apply new schema
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}