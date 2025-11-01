package com.example.inventorymanagement.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.adaptor.CustomerAdapter
import com.example.inventorymanagement.dataclass.Customer

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CustomersFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CustomersFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customers, container, false)
        recyclerView = view.findViewById(R.id.recyclerCustomers)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val customers = listOf(
            Customer(
                "John Doe",
                "john@example.com",
                "+1 555 123-4567",
                "123 Main St, City",
                45.5,
                1250.75,
                "2024-12-29"
            ),
 Customer(
                "John Doe",
                "john@example.com",
                "+1 555 123-4567",
                "123 Main St, City",
                45.5,
                1250.75,
                "2024-12-29"
            ),
 Customer(
                "John Doe",
                "john@example.com",
                "+1 555 123-4567",
                "123 Main St, City",
                45.5,
                1250.75,
                "2024-12-29"
            ),
 Customer(
                "John Doe",
                "john@example.com",
                "+1 555 123-4567",
                "123 Main St, City",
                45.5,
                1250.75,
                "2024-12-29"
            ),
            Customer("Jane Smith", "jane@example.com", "+1 555 234-5678", "456 Oak Ave, City", 0.0, 2100.25, "2024-12-30")
        )

        adapter = CustomerAdapter(customers)
        recyclerView.adapter = adapter

        return view
    }
}
