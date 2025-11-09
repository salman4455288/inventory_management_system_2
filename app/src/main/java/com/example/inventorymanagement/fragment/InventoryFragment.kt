package com.example.inventorymanagement.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.activity.AddProductActivity
import com.example.inventorymanagement.adapter.InventoryAdapter
import com.example.inventorymanagement.dataclass.Product

class InventoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InventoryAdapter
    private val products = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inventory, container, false)
        val addproductact=view.findViewById<LinearLayout>(R.id.btnAddProduct)
        addproductact.setOnClickListener {
            val intent= Intent(context, AddProductActivity::class.java)
            startActivity(intent)
        }
        recyclerView = view.findViewById(R.id.recyclerInventory)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Sample data
        products.add(Product("Coca Cola 500ml", "CC500", "Beverages", 5, 30, 1.80, 2.50, "Coca Cola Co.", "Low Stock", 40))
        products.add(Product("Lay's Original Chips", "LC001", "Snacks", 8, 25, 2.00, 3.50, "PepsiCo", "Low Stock", 60))
        products.add(Product("Milk 1L", "MK100", "Dairy", 15, 50, 3.00, 4.25, "Dairy Farms", "In Stock", 75))

        adapter = InventoryAdapter(products)
        recyclerView.adapter = adapter

        return view
    }

    private fun findViewById(btnAddProduct: Int) {}

    companion object {
        @JvmStatic
        fun newInstance() = InventoryFragment()
    }
}