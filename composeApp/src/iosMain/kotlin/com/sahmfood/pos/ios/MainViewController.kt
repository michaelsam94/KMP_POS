package com.sahmfood.pos.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.sahmfood.pos.ui.PosApp

@Suppress("FunctionName", "unused")
fun MainViewController() =
    ComposeUIViewController {
        initKoin()
        PosApp()
    }
