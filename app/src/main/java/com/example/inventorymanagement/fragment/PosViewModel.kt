package com.example.inventorymanagement.viewmodel

import androidx.lifecycle.ViewModel
import com.example.inventorymanagement.dataclass.CartItem

class PosViewModel : ViewModel() {
    // This list lives independent of the UI lifecycle.
    // It survives when the fragment is destroyed/recreated by the Camera.
    val cartItems = mutableListOf<CartItem>()
}