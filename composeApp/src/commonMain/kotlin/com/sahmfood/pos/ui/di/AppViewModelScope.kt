package com.sahmfood.pos.ui.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Survives tab/navigation changes; do not use [rememberCoroutineScope] for ViewModels on iOS. */
object AppViewModelScope {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
