package com.example.data

import com.example.R
import com.example.model.Deal
import com.example.model.ExtraOption
import com.example.model.Product

object ZaviroMenuData {

    val standardExtras = listOf(
        ExtraOption(id = "extra_cheese", name = "Add Extra Cheese", price = 80),
        ExtraOption(id = "garlic_sauce", name = "Signature Garlic Sauce", price = 80),
        ExtraOption(id = "drink_345ml", name = "Soft Drink 345ml", price = 80),
        ExtraOption(id = "regular_fries", name = "Regular Fries Portion", price = 149)
    )

    val categories = listOf(
        "All",
        "Pasta",
        "Crispy Chicken",
        "Wings",
        "Wraps & Rolls",
        "Fries",
        "Drinks",
        "Extras"
    )

    val initialProducts: List<Product> = listOf(
        // Pasta
        Product(
            id = "prod_chicken_spaghetti",
            name = "Chicken Spaghetti",
            category = "Pasta",
            description = "Tender shredded chicken, rich Italian-style tomato herb spaghetti, topped with chilli flakes and aromatic seasoning.",
            price = 399,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_creamy_pasta,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_creamy_chicken_pasta",
            name = "Creamy Chicken Pasta",
            category = "Pasta",
            description = "Penne pasta smothered in rich velvet white cream sauce, tender chicken cubes, and freshly crushed herbs.",
            price = 449,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_creamy_pasta,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_spicy_chicken_pasta",
            name = "Spicy Chicken Pasta",
            category = "Pasta",
            description = "Spicy infused red & white sauce penne pasta loaded with fiery chicken pieces and crushed peppers.",
            price = 449,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_creamy_pasta,
            availableExtras = standardExtras
        ),

        // Wraps & Rolls
        Product(
            id = "prod_chicken_wrap_roll",
            name = "Chicken Wrap & Roll",
            category = "Wraps & Rolls",
            description = "Juicy flame-grilled chicken chunks wrapped in warm soft tortilla with house garlic mayo and crisp onions.",
            price = 299,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_wrap,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_zinger_wrap_roll",
            name = "Zinger Wrap & Roll",
            category = "Wraps & Rolls",
            description = "Ultra-crispy golden zinger fillet strips rolled in fresh tortilla with secret signature sauce.",
            price = 349,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_wrap,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_cheese_chicken_wrap_roll",
            name = "Cheese Chicken Wrap & Roll",
            category = "Wraps & Rolls",
            description = "Tender seasoned chicken with generous melted cheese slice and garlic sauce rolled in toasted wrap.",
            price = 349,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_wrap,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_cheese_zinger_wrap_roll",
            name = "Cheese Zinger Wrap & Roll",
            category = "Wraps & Rolls",
            description = "Golden crispy zinger chicken loaded with melted cheddar cheese slice and spicy garlic dressing.",
            price = 399,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_wrap,
            availableExtras = standardExtras
        ),

        // Crispy & Juicy Chicken
        Product(
            id = "prod_zinger_piece",
            name = "Zinger Piece",
            category = "Crispy Chicken",
            description = "Signature hand-breaded spicy zinger chicken piece fried to golden perfection with bold seasoning.",
            price = 299,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_crispy_chicken,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_crispy_chicken_piece",
            name = "Crispy Chicken Piece",
            category = "Crispy Chicken",
            description = "Classic crunchy outside, juicy inside fried chicken piece seasoned with Zaviro special spice blend.",
            price = 299,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_crispy_chicken,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_2_piece_combo",
            name = "2-Piece Combo",
            category = "Crispy Chicken",
            description = "2 golden crispy chicken pieces served with crunchy fries and dipping sauce.",
            price = 549,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_crispy_chicken,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_3_piece_combo",
            name = "3-Piece Combo",
            category = "Crispy Chicken",
            description = "3 big juicy crispy chicken pieces served with golden fries and garlic dip.",
            price = 799,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_crispy_chicken,
            availableExtras = standardExtras
        ),

        // Oven Baked / Fried Wings
        Product(
            id = "prod_6_piece_wings",
            name = "6-Piece Wings",
            category = "Wings",
            description = "6 succulent chicken wings tossed in smoky BBQ glaze, sprinkled with sesame seeds. Crispy, juicy, irresistible.",
            price = 699,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_wings,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_12_piece_wings",
            name = "12-Piece Wings",
            category = "Wings",
            description = "12 large succulent wings with irresistible smoky flavour and bigger bites for true wings lovers.",
            price = 1299,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_wings,
            availableExtras = standardExtras
        ),

        // Fries
        Product(
            id = "prod_regular_fries",
            name = "Regular Fries",
            category = "Fries",
            description = "Crispy golden salted potato fries, hot and crunchy.",
            price = 149,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_loaded_fries,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_masala_fries",
            name = "Masala Fries",
            category = "Fries",
            description = "Crispy fries dusted with special chatpata Pakistani masala spice blend.",
            price = 199,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_loaded_fries,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_cheese_fries",
            name = "Cheese Fries",
            category = "Fries",
            description = "Golden fries smothered in hot creamy liquid cheese sauce.",
            price = 249,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_loaded_fries,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_loaded_fries_regular",
            name = "Loaded Fries Regular",
            category = "Fries",
            description = "Cheesy, crunchy, full of flavour! Crispy fries loaded with cheese sauce, crispy fried chicken chunks, and signature sauces.",
            price = 499,
            isAvailable = true,
            isPopular = true,
            drawableRes = R.drawable.zaviro_loaded_fries,
            availableExtras = standardExtras
        ),
        Product(
            id = "prod_loaded_fries_large",
            name = "Loaded Fries Large",
            category = "Fries",
            description = "Large sharing platter of loaded fries drenched in double cheese sauce and crispy chicken bites.",
            price = 599,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_loaded_fries,
            availableExtras = standardExtras
        ),

        // Drinks
        Product(
            id = "prod_soft_drink_345ml",
            name = "Soft Drink 345ml",
            category = "Drinks",
            description = "Chilled 345ml beverage bottle (Pepsi / 7Up / Mirinda / Mountain Dew).",
            price = 80,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_hero_banner
        ),
        Product(
            id = "prod_soft_drink_1_5l",
            name = "Soft Drink 1.5L",
            category = "Drinks",
            description = "Chilled 1.5 Litre jumbo bottle for family meals.",
            price = 220,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_hero_banner
        ),

        // Extras
        Product(
            id = "prod_garlic_sauce",
            name = "Garlic Sauce",
            category = "Extras",
            description = "ZAVIRO special homemade velvety garlic dip.",
            price = 80,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_wrap
        ),
        Product(
            id = "prod_extra_cheese",
            name = "Extra Cheese",
            category = "Extras",
            description = "Rich melted cheese topping portion.",
            price = 80,
            isAvailable = true,
            isPopular = false,
            drawableRes = R.drawable.zaviro_loaded_fries
        )
    )

    val initialDeals: List<Deal> = listOf(
        Deal(
            id = "deal_1",
            dealNumber = 1,
            name = "ZAVIRO QUICK BITE",
            category = "Wraps & Rolls",
            items = listOf("Chicken Wrap & Roll", "Regular Fries", "345ml Drink"),
            price = 499,
            originalPrice = 528,
            saveAmount = 29,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wrap
        ),
        Deal(
            id = "deal_2",
            dealNumber = 2,
            name = "CHICKEN STREET BOX",
            category = "Wraps & Rolls",
            items = listOf("Chicken Wrap & Roll", "Masala Fries", "345ml Drink"),
            price = 549,
            originalPrice = 578,
            saveAmount = 29,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wrap
        ),
        Deal(
            id = "deal_3",
            dealNumber = 3,
            name = "CHEESY CHICKEN BOX",
            category = "Wraps & Rolls",
            items = listOf("Cheese Chicken Wrap & Roll", "Regular Fries", "345ml Drink"),
            price = 549,
            originalPrice = 578,
            saveAmount = 29,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wrap
        ),
        Deal(
            id = "deal_4",
            dealNumber = 4,
            name = "CREAMY PASTA COMBO",
            category = "Pasta Combos",
            items = listOf("Creamy Chicken Pasta", "Regular Fries", "345ml Drink"),
            price = 629,
            originalPrice = 678,
            saveAmount = 49,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_creamy_pasta
        ),
        Deal(
            id = "deal_5",
            dealNumber = 5,
            name = "SPICY PASTA FEAST",
            category = "Pasta Combos",
            items = listOf("Spicy Chicken Pasta", "Masala Fries", "345ml Drink"),
            price = 679,
            originalPrice = 728,
            saveAmount = 49,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_creamy_pasta
        ),
        Deal(
            id = "deal_6",
            dealNumber = 6,
            name = "PASTA & ROLL DUO",
            category = "Pasta Combos",
            items = listOf("Chicken Spaghetti", "Chicken Wrap & Roll", "345ml Drink"),
            price = 729,
            originalPrice = 778,
            saveAmount = 49,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_creamy_pasta
        ),
        Deal(
            id = "deal_7",
            dealNumber = 7,
            name = "CRISPY CHICKEN BOX",
            category = "Crispy Chicken",
            items = listOf("2 Crispy Chicken Pieces", "Regular Fries", "345ml Drink"),
            price = 729,
            originalPrice = 827,
            saveAmount = 98,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_crispy_chicken
        ),
        Deal(
            id = "deal_8",
            dealNumber = 8,
            name = "DOUBLE CRUNCH BOX",
            category = "Crispy Chicken",
            items = listOf("2 Crispy Chicken Pieces", "Masala Fries", "345ml Drink"),
            price = 749,
            originalPrice = 877,
            saveAmount = 128,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_crispy_chicken
        ),
        Deal(
            id = "deal_9",
            dealNumber = 9,
            name = "WINGS & CRUNCH",
            category = "Wings Specials",
            items = listOf("6-Piece Wings", "Regular Fries", "345ml Drink"),
            price = 849,
            originalPrice = 928,
            saveAmount = 79,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wings
        ),
        Deal(
            id = "deal_10",
            dealNumber = 10,
            name = "WINGS MASALA FEAST",
            category = "Wings Specials",
            items = listOf("6-Piece Wings", "Masala Fries", "345ml Drink"),
            price = 899,
            originalPrice = 978,
            saveAmount = 79,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wings
        ),
        Deal(
            id = "deal_11",
            dealNumber = 11,
            name = "LOADED WINGS BOX",
            category = "Wings Specials",
            items = listOf("6-Piece Wings", "Loaded Fries Regular", "345ml Drink"),
            price = 1099,
            originalPrice = 1278,
            saveAmount = 179,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wings
        ),
        Deal(
            id = "deal_12",
            dealNumber = 12,
            name = "DOUBLE WINGS FEAST",
            category = "Wings Specials",
            items = listOf("12-Piece Wings", "1.5L Drink"),
            price = 1399,
            originalPrice = 1519,
            saveAmount = 120,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_wings
        ),
        Deal(
            id = "deal_13",
            dealNumber = 13,
            name = "ZAVIRO FAMILY FEAST",
            category = "Family Deals",
            items = listOf("2 Chicken Wraps & Rolls", "2 Crispy Chicken Pieces", "Large Fries", "1.5L Drink"),
            price = 1899,
            originalPrice = 2015,
            saveAmount = 116,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_hero_banner
        ),
        Deal(
            id = "deal_14",
            dealNumber = 14,
            name = "ZAVIRO ULTIMATE FAMILY",
            category = "Family Deals",
            items = listOf("4 Chicken Wraps & Rolls", "4 Crispy Chicken Pieces", "Large Fries", "1.5L Drink"),
            price = 2599,
            originalPrice = 3211,
            saveAmount = 612,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_hero_banner
        ),
        Deal(
            id = "deal_15",
            dealNumber = 15,
            name = "ZAVIRO MEGA BOX",
            category = "Special Value",
            items = listOf("Chicken Wrap & Roll", "Loaded Fries Regular", "345ml Drink"),
            price = 849,
            originalPrice = 878,
            saveAmount = 29,
            isAvailable = true,
            drawableRes = R.drawable.zaviro_loaded_fries
        )
    )
}
