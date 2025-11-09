package com.example.inventorymanagement.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.adapter.CartItemAdapter
import com.example.inventorymanagement.dataclass.CartItem

class PosFragment : Fragment() {

    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var cartAdapter: CartItemAdapter
    private val cartItems = mutableListOf<CartItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pos, container, false)

        cartRecyclerView = view.findViewById(R.id.cartRecyclerView)
        cartRecyclerView.layoutManager = LinearLayoutManager(context)

        // Sample data
        cartItems.add(CartItem("Coca Cola 500ml", 5.00, 2, 2.50))
        cartItems.add(CartItem("Lay's Original Chips", 3.99, 1, 3.99))
        cartItems.add(CartItem("Milk 1L", 4.25, 1, 4.25))

        cartAdapter = CartItemAdapter(cartItems)
        cartRecyclerView.adapter = cartAdapter

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = PosFragment()
    }
}