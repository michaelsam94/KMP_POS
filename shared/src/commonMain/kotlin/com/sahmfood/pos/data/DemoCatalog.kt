package com.sahmfood.pos.data

import com.sahmfood.pos.domain.model.Product

/** Demo product catalogue — seeded into SQLite on first launch. */
object DemoCatalog {
    val products: List<Product> =
        listOf(
            Product("p1", "6281007010010", "Espresso", "Double shot", 15.0, "Beverages", 100),
            Product("p2", "6281007010027", "Cappuccino", "With milk foam", 20.0, "Beverages", 80),
            Product("p3", "6281007010034", "Club Sandwich", "Classic", 45.0, "Food", 50),
            Product("p4", "6281007010041", "Chicken Burger", "Crispy", 55.0, "Food", 40),
            Product("p5", "6281007010058", "Caesar Salad", "Fresh", 38.0, "Salads", 30),
            Product("p6", "6281007010065", "Chocolate Cake", "Rich", 25.0, "Desserts", 60),
            Product("p7", "6281007010072", "Mineral Water", "500 ml", 8.0, "Beverages", 200),
            Product("p8", "6281007010089", "Orange Juice", "Fresh squeezed", 22.0, "Beverages", 70),
        )

    fun findByBarcode(barcode: String): Product? = products.find { it.barcode == barcode }
}
