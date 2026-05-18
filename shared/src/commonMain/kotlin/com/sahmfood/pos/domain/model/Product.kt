package com.sahmfood.pos.domain.model

/**
 * Represents a sellable item in the POS catalogue.
 * Immutable value object — all state changes produce a new instance.
 */
data class Product(
    val id: String,
    val barcode: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String,
    val stockQuantity: Int,
    val imageUrl: String = "",
    val isActive: Boolean = true,
) {
    init {
        require(price >= 0.0) { "Product price must be non-negative" }
        require(name.isNotBlank()) { "Product name must not be blank" }
        require(barcode.isNotBlank()) { "Product barcode must not be blank" }
    }
}
