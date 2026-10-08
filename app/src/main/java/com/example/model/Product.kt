package com.example.model

data class ExtraOption(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val description: String = "",
    val isAvailable: Boolean = true,
    val imageUrl: String = ""
)

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "", // Pasta, Crispy Chicken, Wings, Wraps & Rolls, Fries, Drinks, Extras
    val description: String = "",
    val ingredients: String = "",
    val price: Int = 0,
    val isAvailable: Boolean = true,
    val isPopular: Boolean = false,
    val drawableRes: Int = 0,
    val imageUrl: String = "",
    val availableExtras: List<ExtraOption> = emptyList()
)
