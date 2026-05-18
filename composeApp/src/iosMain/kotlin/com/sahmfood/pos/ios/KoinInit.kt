package com.sahmfood.pos.ios

import com.sahmfood.pos.data.local.db.DatabaseDriverFactory
import com.sahmfood.pos.data.sync.SyncService
import com.sahmfood.pos.di.sharedModule
import com.sahmfood.pos.domain.usecase.SeedDemoCatalogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform.getKoin

private var isInitialized = false

fun initKoin() {
    if (isInitialized) return
    isInitialized = true

    startKoin {
        modules(
            sharedModule(),
            module {
                single { DatabaseDriverFactory() }
            }
        )
    }

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    scope.launch {
        getKoin().get<SeedDemoCatalogUseCase>().invoke()
    }
    getKoin().get<SyncService>().start()
}
