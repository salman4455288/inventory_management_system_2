package com.example.inventorymanagement.util

import android.content.Context
import com.example.inventorymanagement.R
import java.io.IOException

object BaseURL {

    // Reads the URL from res/raw/base_url.txt
    fun getUrl(context: Context): String {
        return try {
            // Access the file using the Resource ID (R.raw.base_url)
            // Note: The file must be named "base_url.txt" inside res/raw
            val inputStream = context.resources.openRawResource(R.raw.base_url)

            inputStream.bufferedReader().use { reader ->
                reader.readText().trim() // Removes extra lines or spaces
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback URL
            "http://10.0.2.2/inventory_api/"
        }
    }
}