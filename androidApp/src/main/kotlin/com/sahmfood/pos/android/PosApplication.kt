package com.sahmfood.pos.android

import android.app.Application
import com.sahmfood.pos.android.di.androidViewModelModule
import com.sahmfood.pos.data.local.db.DatabaseDriverFactory
import com.sahmfood.pos.data.sync.SyncService
import com.sahmfood.pos.di.sharedModule
import com.sahmfood.pos.domain.usecase.SeedDemoCatalogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class PosApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PosApplication)
            modules(
                sharedModule(),
                androidViewModelModule,
                module {
                    single { DatabaseDriverFactory(androidContext()) }
                }
            )
        }

        appScope.launch {
            get<SeedDemoCatalogUseCase>().invoke()
        }

        get<SyncService>().start()
    }
}
