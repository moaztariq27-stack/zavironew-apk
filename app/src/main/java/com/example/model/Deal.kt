package com.example.model

data class Deal(
    val id: String = "",
    val dealNumber: Int = 1,
    val name: String = "",
    val category: String = "", // Wraps & Rolls, Pasta Combos, Crispy Chicken, Wings Specials, Family Deals, Special Value
    val items: List<String> = emptyList(),
    val description: String = "",
    val price: Int = 0,
    val originalPrice: Int = 0,
    val saveAmount: Int = 0,
    val isAvailable: Boolean = true,
    val drawableRes: Int = 0,
    val imageUrl: String = ""
)
