package com.sahmfood.pos.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sahmfood.pos.data.local.db.PosDatabase
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(db: PosDatabase) : ProductRepository {
    // SQLDelight generates "productQueries" from "Product.sq"
    private val queries = db.productQueries

    override fun observeAll(): Flow<List<Product>> =
        queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: String): Product? = queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun getByBarcode(barcode: String): Product? = queries.selectByBarcode(barcode).executeAsOneOrNull()?.toDomain()

    override fun search(query: String): Flow<List<Product>> =
        queries.search(query)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(product: Product) {
        queries.upsert(
            id = product.id,
            barcode = product.barcode,
            name = product.name,
            description = product.description,
            price = product.price,
            category = product.category,
            stockQuantity = product.stockQuantity.toLong(),
            imageUrl = product.imageUrl,
            isActive = if (product.isActive) 1L else 0L,
        )
    }

    override suspend fun upsertAll(products: List<Product>) {
        products.forEach { upsert(it) }
    }

    override suspend fun delete(id: String) {
        queries.delete(id)
    }

    // ── Mapper ────────────────────────────────────────────────────────────
    private fun com.sahmfood.pos.ProductEntity.toDomain() =
        Product(
            id = id,
            barcode = barcode,
            name = name,
            description = description,
            price = price,
            category = category,
            stockQuantity = stockQuantity.toInt(),
            imageUrl = imageUrl,
            isActive = isActive == 1L,
        )
}
