package com.sahmfood.pos.domain.usecase

import com.sahmfood.pos.data.DemoCatalog
import com.sahmfood.pos.domain.repository.ProductRepository

/** Inserts or updates the demo product catalogue in the local database. */
class SeedDemoCatalogUseCase(private val productRepository: ProductRepository) {
    suspend operator fun invoke() {
        productRepository.upsertAll(DemoCatalog.products)
    }
}
