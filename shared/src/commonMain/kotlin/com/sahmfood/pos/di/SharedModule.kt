package com.sahmfood.pos.di

import com.sahmfood.pos.data.hardware.MockBarcodeScanner
import com.sahmfood.pos.data.hardware.MockPaymentTerminal
import com.sahmfood.pos.data.hardware.MockReceiptPrinter
import com.sahmfood.pos.data.local.db.DatabaseDriverFactory
import com.sahmfood.pos.data.local.db.PosDatabase
import com.sahmfood.pos.data.repository.OrderRepositoryImpl
import com.sahmfood.pos.data.repository.ProductRepositoryImpl
import com.sahmfood.pos.data.repository.SyncRepositoryImpl
import com.sahmfood.pos.data.repository.TransactionRepositoryImpl
import com.sahmfood.pos.data.sync.MockRemoteApi
import com.sahmfood.pos.data.sync.RemoteApi
import com.sahmfood.pos.data.sync.SyncService
import com.sahmfood.pos.domain.repository.OrderRepository
import com.sahmfood.pos.domain.repository.ProductRepository
import com.sahmfood.pos.domain.repository.SyncRepository
import com.sahmfood.pos.domain.repository.TransactionRepository
import com.sahmfood.pos.domain.usecase.AddItemToCartUseCase
import com.sahmfood.pos.domain.usecase.CalculateOrderTotalUseCase
import com.sahmfood.pos.domain.usecase.CreateOrderUseCase
import com.sahmfood.pos.domain.usecase.GetTransactionHistoryUseCase
import com.sahmfood.pos.domain.usecase.ObserveProductsUseCase
import com.sahmfood.pos.domain.usecase.ProcessPaymentUseCase
import com.sahmfood.pos.domain.usecase.RemoveItemFromCartUseCase
import com.sahmfood.pos.domain.usecase.ScanBarcodeUseCase
import com.sahmfood.pos.domain.usecase.SearchProductsUseCase
import com.sahmfood.pos.domain.usecase.SeedDemoCatalogUseCase
import com.sahmfood.pos.domain.usecase.UpdateCartItemQuantityUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

fun sharedModule(): Module = module {

    // ── Database ─────────────────────────────────────────────────────────
    single { get<DatabaseDriverFactory>().create() }
    single { PosDatabase(get()) }

    // ── Repositories ─────────────────────────────────────────────────────
    single<ProductRepository>     { ProductRepositoryImpl(get()) }
    single<OrderRepository>       { OrderRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<SyncRepository>        { SyncRepositoryImpl(get()) }

    // ── Hardware (mock) ───────────────────────────────────────────────────
    single { MockReceiptPrinter() }
    single { MockPaymentTerminal() }
    single { MockBarcodeScanner() }

    // ── Sync ──────────────────────────────────────────────────────────────
    single<RemoteApi> { MockRemoteApi() }
    single {
        SyncService(
            syncRepository = get(),
            remoteApi      = get(),
            scope          = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
    }

    // ── Use cases ─────────────────────────────────────────────────────────
    factory { CreateOrderUseCase(get()) }
    factory { AddItemToCartUseCase(get()) }
    factory { RemoveItemFromCartUseCase(get()) }
    factory { UpdateCartItemQuantityUseCase(get()) }
    factory { CalculateOrderTotalUseCase() }
    factory { ProcessPaymentUseCase(get(), get(), get()) }
    factory { GetTransactionHistoryUseCase(get()) }
    factory { ObserveProductsUseCase(get()) }
    factory { ScanBarcodeUseCase(get()) }
    factory { SearchProductsUseCase(get()) }
    factory { SeedDemoCatalogUseCase(get()) }

}
