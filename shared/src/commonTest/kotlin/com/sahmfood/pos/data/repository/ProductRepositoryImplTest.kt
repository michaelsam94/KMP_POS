package com.sahmfood.pos.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.sahmfood.pos.data.local.db.PosDatabase
import com.sahmfood.pos.domain.model.Product
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Integration tests that run against an in-memory SQLite database.
 * Tests are written before the implementation (TDD red-green-refactor).
 */
class ProductRepositoryImplTest {
    private lateinit var db: PosDatabase
    private lateinit var repository: ProductRepositoryImpl
    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PosDatabase.Schema.create(driver)
        db = PosDatabase(driver)
        repository = ProductRepositoryImpl(db)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun product(
        id: String = "p1",
        barcode: String = "111",
    ) = Product(
        id = id,
        barcode = barcode,
        name = "Espresso",
        description = "Double shot",
        price = 15.0,
        category = "Beverages",
        stockQuantity = 100,
    )

    @Test
    fun `upsert and getById returns product`() =
        runTest {
            repository.upsert(product())
            val found = repository.getById("p1")
            assertNotNull(found)
            assertEquals("Espresso", found.name)
            assertEquals(15.0, found.price)
        }

    @Test
    fun `getByBarcode returns correct product`() =
        runTest {
            repository.upsert(product(id = "p2", barcode = "999"))
            val found = repository.getByBarcode("999")
            assertNotNull(found)
            assertEquals("p2", found.id)
        }

    @Test
    fun `getById returns null for missing id`() =
        runTest {
            assertNull(repository.getById("missing"))
        }

    @Test
    fun `observeAll emits product after upsert`() =
        runTest {
            repository.observeAll().test {
                assertEquals(0, awaitItem().size) // initial empty emission

                repository.upsert(product("a", "A"))
                assertEquals(1, awaitItem().size)

                repository.upsert(product("b", "B"))
                assertEquals(2, awaitItem().size)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `upsert with same id updates the record`() =
        runTest {
            repository.upsert(product())
            repository.upsert(product().copy(name = "Americano", price = 18.0))

            val found = repository.getById("p1")
            assertEquals("Americano", found?.name)
            assertEquals(18.0, found?.price)
        }

    @Test
    fun `delete removes product from store`() =
        runTest {
            repository.upsert(product())
            repository.delete("p1")
            assertNull(repository.getById("p1"))
        }

    @Test
    fun `upsertAll inserts multiple products`() =
        runTest {
            val products = (1..5).map { product("p$it", "$it") }
            repository.upsertAll(products)

            repository.observeAll().test {
                assertEquals(5, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
