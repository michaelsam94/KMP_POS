package com.sahmfood.pos.data.local.db

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific factory — implemented in androidMain / iosMain. */
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
