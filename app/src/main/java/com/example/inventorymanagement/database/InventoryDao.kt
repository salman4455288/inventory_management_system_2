package com.example.inventorymanagement.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.inventorymanagement.dataclass.Customer
import com.example.inventorymanagement.dataclass.Product
import com.example.inventorymanagement.dataclass.Sale
import com.example.inventorymanagement.dataclass.SaleItem

@Dao
interface InventoryDao {
    // --- SYNC QUERIES ---
    @Query("SELECT * FROM sales WHERE is_synced = 0")
    suspend fun getUnsyncedSales(): List<Sale>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleLocalId")
    suspend fun getSaleItemsForSale(saleLocalId: Long): List<SaleItem>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): Customer?
    // ... (Keep existing Product/Customer methods) ...
    @Query("SELECT * FROM products ORDER BY id DESC")
    suspend fun getAllProducts(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY id DESC")
    suspend fun getAllCustomers(): List<Customer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<Customer>)

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?


    // --- NEW: SALES LOGIC ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long // Returns the new localId

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItem>)

    @Query("UPDATE sales SET is_synced = 1 WHERE localId = :id")
    suspend fun markSaleAsSynced(id: Long)
}