package com.sahmfood.pos.data.local.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver = NativeSqliteDriver(PosDatabase.Schema, "pos_database.db")
}
