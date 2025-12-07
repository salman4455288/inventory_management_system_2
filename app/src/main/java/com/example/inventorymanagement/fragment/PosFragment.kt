package com.example.inventorymanagement.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventorymanagement.R
import com.example.inventorymanagement.activity.ManualAddActivity
import com.example.inventorymanagement.activity.SelectCustomer
import com.example.inventorymanagement.adapter.CartItemAdapter
import com.example.inventorymanagement.dataclass.CartItem
import com.example.inventorymanagement.util.BaseURL
import com.example.inventorymanagement.viewmodel.PosViewModel
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.camera.CameraSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PosFragment : Fragment() {

    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var cartAdapter: CartItemAdapter
    private lateinit var viewModel: PosViewModel

    private lateinit var tvSubtotal: TextView
    private lateinit var tvTax: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button
    private lateinit var btnCreditSale: Button

    private lateinit var scannerView: DecoratedBarcodeView
    private lateinit var layoutCameraOff: LinearLayout
    private lateinit var btnToggleCamera: ImageButton
    private lateinit var tvScannerStatus: TextView

    private val beepSound = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private var isCameraOn = false
    private var isProcessingScan = false

    private val BASE_URL: String by lazy { BaseURL.getUrl(requireContext()) }

    // --- LAUNCHERS ---

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) enableCamera(true) else enableCamera(false)
    }

    private val manualAddLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val id = data.getIntExtra("PRODUCT_ID", -1)
                val name = data.getStringExtra("PRODUCT_NAME")
                val price = data.getDoubleExtra("PRODUCT_PRICE", 0.0)

                if (id != -1 && name != null) {
                    addToCart(id, name, price)
                    Toast.makeText(context, "$name Added", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // NEW: Handle Customer Selection
    private val selectCustomerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val name = data?.getStringExtra("CUST_NAME")
            val phone = data?.getStringExtra("CUST_PHONE")

            if (!name.isNullOrEmpty() && !phone.isNullOrEmpty()) {
                // Customer selected! Proceed to checkout immediately.
                performCheckout(name, phone)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pos, container, false)

        viewModel = ViewModelProvider(requireActivity())[PosViewModel::class.java]

        tvSubtotal = view.findViewById(R.id.tvSubtotal)
        tvTax = view.findViewById(R.id.tvTax)
        tvTotal = view.findViewById(R.id.tvTotal)
        btnCheckout = view.findViewById(R.id.btnCheckout)
        btnCreditSale = view.findViewById(R.id.btnCreditSale)

        scannerView = view.findViewById(R.id.scanner_view)
        layoutCameraOff = view.findViewById(R.id.layoutCameraOff)
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera)
        tvScannerStatus = view.findViewById(R.id.tvScannerStatus)

        cartRecyclerView = view.findViewById(R.id.cartRecyclerView)
        cartRecyclerView.layoutManager = LinearLayoutManager(context)

        cartAdapter = CartItemAdapter(viewModel.cartItems) {
            updateTotals()
        }
        cartRecyclerView.adapter = cartAdapter

        // Camera Settings
        val cameraSettings = CameraSettings()
        cameraSettings.isAutoFocusEnabled = true
        cameraSettings.isMeteringEnabled = true
        scannerView.barcodeView.cameraSettings = cameraSettings

        enableCamera(false)

        scannerView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    if (!isProcessingScan && isCameraOn && !it.text.isNullOrEmpty()) {
                        isProcessingScan = true
                        beepSound.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        scannerView.pause()
                        fetchProductByBarcode(it.text)
                    }
                }
            }
        })

        btnToggleCamera.setOnClickListener {
            if (isCameraOn) {
                enableCamera(false)
            } else {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                    enableCamera(true)
                } else {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
        }

        view.findViewById<LinearLayout>(R.id.btnAddManually).setOnClickListener {
            val intent = Intent(context, ManualAddActivity::class.java)
            manualAddLauncher.launch(intent)
        }

        // Cash Sale
        btnCheckout.setOnClickListener {
            if (viewModel.cartItems.isNotEmpty()) {
                performCheckout(null, null)
            } else {
                Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
            }
        }

        // Credit Sale
        btnCreditSale.setOnClickListener {
            if (viewModel.cartItems.isNotEmpty()) {
                showCustomerDialog()
            } else {
                Toast.makeText(context, "Cart is empty", Toast.LENGTH_SHORT).show()
            }
        }

        updateTotals()
        return view
    }

    // --- CUSTOMER DIALOG (Updated) ---
    private fun showCustomerDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add to Customer Account")

        val container = LinearLayout(requireContext())
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(50, 20, 50, 0)

        val inputName = EditText(requireContext())
        inputName.hint = "Customer Name"
        container.addView(inputName)

        val inputPhone = EditText(requireContext())
        inputPhone.hint = "Phone Number (Required)"
        inputPhone.inputType = InputType.TYPE_CLASS_PHONE
        container.addView(inputPhone)

        builder.setView(container)

        // 1. NEUTRAL BUTTON: Open List
        builder.setNeutralButton("Select Existing") { dialog, _ ->
            val intent = Intent(context, SelectCustomer::class.java)
            selectCustomerLauncher.launch(intent)
            dialog.dismiss()
        }

        // 2. POSITIVE BUTTON: Add New / Manual Entry
        builder.setPositiveButton("Confirm") { _, _ ->
            val name = inputName.text.toString().trim()
            val phone = inputPhone.text.toString().trim()

            if (phone.isNotEmpty()) {
                performCheckout(name, phone)
            } else {
                Toast.makeText(context, "Phone number is required", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun enableCamera(enable: Boolean) {
        isCameraOn = enable
        if (enable) {
            scannerView.resume()
            layoutCameraOff.visibility = View.GONE
            tvScannerStatus.visibility = View.VISIBLE
            btnToggleCamera.imageTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
        } else {
            scannerView.pause()
            layoutCameraOff.visibility = View.VISIBLE
            tvScannerStatus.visibility = View.GONE
            btnToggleCamera.imageTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
        }
    }

    override fun onResume() {
        super.onResume()
        if (isCameraOn) scannerView.resume()
    }

    override fun onPause() {
        super.onPause()
        scannerView.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        beepSound.release()
    }

    private fun fetchProductByBarcode(barcode: String) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "get_product_by_barcode.php")
                val postData = "api_token=" + URLEncoder.encode(apiToken, "UTF-8") +
                        "&barcode=" + URLEncoder.encode(barcode, "UTF-8")

                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(postData); writer.flush(); writer.close()
                val response = conn.inputStream.bufferedReader().readText()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    try {
                        val json = JSONObject(response)
                        if (!json.getBoolean("error")) {
                            val p = json.getJSONObject("product")
                            addToCart(
                                p.getInt("id"),
                                p.getString("name"),
                                p.getDouble("sale_price")
                            )
                            Toast.makeText(context, "Added: ${p.getString("name")}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Product Not Found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) { e.printStackTrace() }

                    delay(1500)
                    isProcessingScan = false
                    if (isCameraOn) scannerView.resume()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isProcessingScan = false
                    if (isCameraOn) scannerView.resume()
                }
            }
        }
    }

    private fun addToCart(id: Int, name: String, price: Double) {
        val existingItem = viewModel.cartItems.find { it.id == id }
        if (existingItem != null) {
            existingItem.quantity++
            existingItem.total = existingItem.price * existingItem.quantity
        } else {
            viewModel.cartItems.add(CartItem(id, name, price, 1, price))
        }
        cartAdapter.notifyDataSetChanged()
        updateTotals()
    }

    private fun updateTotals() {
        var subtotal = 0.0
        for (item in viewModel.cartItems) subtotal += item.total
        val taxRate = 0.085; val tax = subtotal * taxRate; val total = subtotal + tax
        tvSubtotal.text = String.format("Rs. %.2f", subtotal)
        tvTax.text = String.format("Rs. %.2f", tax)
        tvTotal.text = String.format("Rs. %.2f", total)
    }

    private fun performCheckout(custName: String?, custPhone: String?) {
        val sharedPref = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val apiToken = sharedPref.getString("api_token", "") ?: ""
        var subtotal = 0.0
        for (item in viewModel.cartItems) subtotal += item.total
        val tax = subtotal * 0.085; val total = subtotal + tax

        val jsonParams = JSONObject()
        jsonParams.put("api_token", apiToken)
        jsonParams.put("total", total)
        jsonParams.put("tax", tax)

        if (custPhone != null) {
            jsonParams.put("customer_name", custName)
            jsonParams.put("customer_phone", custPhone)
        }

        val itemsArray = JSONArray()
        for (item in viewModel.cartItems) {
            val itemObj = JSONObject()
            itemObj.put("id", item.id); itemObj.put("quantity", item.quantity); itemObj.put("price", item.price)
            itemsArray.put(itemObj)
        }
        jsonParams.put("cart_items", itemsArray)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(BASE_URL + "checkout.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                conn.doOutput = true
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonParams.toString()); writer.flush(); writer.close()

                val response = conn.inputStream.bufferedReader().readText()
                Log.d("CHECKOUT_DEBUG", "Response: $response")

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    try {
                        val jsonStartIndex = response.indexOf("{")
                        val jsonEndIndex = response.lastIndexOf("}")

                        if (jsonStartIndex != -1 && jsonEndIndex != -1) {
                            val cleanResponse = response.substring(jsonStartIndex, jsonEndIndex + 1)
                            val json = JSONObject(cleanResponse)

                            if (!json.getBoolean("error")) {
                                val msg = json.getString("message")
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                viewModel.cartItems.clear()
                                cartAdapter.notifyDataSetChanged()
                                updateTotals()
                            } else {
                                Toast.makeText(context, "Server Error: " + json.getString("message"), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Invalid Server Data", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Parse Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PosFragment()
    }
}