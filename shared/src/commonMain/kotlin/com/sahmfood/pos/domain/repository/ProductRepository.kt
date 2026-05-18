package com.sahmfood.pos.domain.repository

import com.sahmfood.pos.domain.model.Product
import kotlinx.coroutines.flow.Flow

/** Contract for product catalogue persistence. */
interface ProductRepository {
    /** Emits the full list whenever it changes. */
    fun observeAll(): Flow<List<Product>>

    suspend fun getById(id: String): Product?

    suspend fun getByBarcode(barcode: String): Product?

    /** Returns products whose name or category matches [query] (case-insensitive). */
    fun search(query: String): Flow<List<Product>>

    suspend fun upsert(product: Product)

    suspend fun upsertAll(products: List<Product>)

    suspend fun delete(id: String)
}
