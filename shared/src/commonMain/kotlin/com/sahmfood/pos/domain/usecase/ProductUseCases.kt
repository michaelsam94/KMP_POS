package com.sahmfood.pos.domain.usecase

import com.sahmfood.pos.data.DemoCatalog
import com.sahmfood.pos.domain.model.Product
import com.sahmfood.pos.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

/** Emits the full product catalogue whenever it changes. */
class ObserveProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(): Flow<List<Product>> = repository.observeAll()
}

/** Returns a product matched by barcode, or null if not found. */
class ScanBarcodeUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(barcode: String): Product? {
        repository.getByBarcode(barcode)?.let { return it }
        val demo = DemoCatalog.findByBarcode(barcode) ?: return null
        repository.upsert(demo)
        return demo
    }
}

/** Returns search results from the catalogue. */
class SearchProductsUseCase(private val repository: ProductRepository) {
    operator fun invoke(query: String): Flow<List<Product>> = repository.search(query)
}
