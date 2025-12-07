package com.example.inventorymanagement.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.inventorymanagement.InventoryApp
import com.example.inventorymanagement.util.BaseURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dao = (applicationContext as InventoryApp).database.inventoryDao()
        val unsyncedSales = dao.getUnsyncedSales()

        if (unsyncedSales.isEmpty()) return Result.success()

        val sharedPref = applicationContext.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""
        val baseUrl = BaseURL.getUrl(applicationContext)

        for (sale in unsyncedSales) {
            val items = dao.getSaleItemsForSale(sale.localId)

            // Reconstruct JSON for checkout.php
            val jsonParams = JSONObject()
            jsonParams.put("api_token", apiToken)
            jsonParams.put("total", sale.total_amount)
            jsonParams.put("tax", sale.tax_amount)

            // --- FIX: Include Customer Data for Credit Sales ---
            if (sale.customer_id != null && sale.customer_id != 0) {
                // We need to fetch customer details to send to server
                // NOTE: You must ensure 'getCustomerById' exists in your InventoryDao
                val customer = dao.getCustomerById(sale.customer_id)
                if (customer != null) {
                    jsonParams.put("customer_name", customer.name)
                    jsonParams.put("customer_phone", customer.phone)
                }
            }

            val itemsArray = JSONArray()
            for (item in items) {
                val itemObj = JSONObject()
                itemObj.put("id", item.product_id)
                itemObj.put("quantity", item.quantity)
                itemObj.put("price", item.price)
                itemsArray.put(itemObj)
            }
            jsonParams.put("cart_items", itemsArray)

            // Upload
            val success = uploadSale(baseUrl, jsonParams.toString())
            if (success) {
                dao.markSaleAsSynced(sale.localId)
            }
        }
        return Result.success()
    }

    private suspend fun uploadSale(baseUrl: String, json: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(baseUrl + "checkout.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json)
                writer.flush()
                writer.close()

                // FIX: Check response code to prevent crash on errors
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().readText()
                    val jsonObj = JSONObject(response)
                    !jsonObj.getBoolean("error")
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}