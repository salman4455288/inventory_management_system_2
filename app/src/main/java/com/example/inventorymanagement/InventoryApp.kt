package com.example.inventorymanagement

import android.app.Application
import com.example.inventorymanagement.database.AppDatabase

class InventoryApp : Application() {
    // Creates a single database instance for the whole app
    val database by lazy { AppDatabase.getDatabase(this) }
}