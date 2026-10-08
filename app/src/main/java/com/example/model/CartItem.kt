package com.example.model

data class CartItem(
    val cartItemId: String = "",
    val productId: String? = null,
    val dealId: String? = null,
    val name: String = "",
    val price: Int = 0,
    val quantity: Int = 1,
    val selectedExtras: List<ExtraOption> = emptyList(),
    val specialInstructions: String = "",
    val drawableRes: Int = 0,
    val imageUrl: String = ""
) {
    val unitPriceWithExtras: Int
        get() = price + selectedExtras.sumOf { it.price }

    val totalPrice: Int
        get() = unitPriceWithExtras * quantity
}
