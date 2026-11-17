package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DraftSaleItem
import com.example.data.LibardinoDatabase
import com.example.data.PaymentMethod
import com.example.data.SaleStatus
import com.example.data.StoreRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: LibardinoDatabase
    private lateinit var repository: StoreRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LibardinoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StoreRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name is Libardino`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Libardino", appName)
    }

    @Test
    fun `complete, edit to add 4th product, and cancel sale updates stock accurately`() = runTest {
        val shirtId = repository.saveGarment(
            garmentId = null,
            barcode = "770123456001",
            name = "Camisa Polo",
            size = "M",
            price = 80000.0,
            stock = 10,
            lowStockThreshold = 3
        ).getOrThrow()

        val jeanId = repository.saveGarment(
            garmentId = null,
            barcode = "770123456002",
            name = "Jean Slim",
            size = "32",
            price = 120000.0,
            stock = 3,
            lowStockThreshold = 3
        ).getOrThrow()

        val initialGarments = repository.observeGarments().first()
        assertTrue(initialGarments.first { it.id == jeanId }.needsRestockAlert)

        // 1. Complete initial sale with 3 items (2 shirts + 1 jean)
        val orderId = repository.saveSaleOrder(
            existingOrderId = null,
            targetStatus = SaleStatus.COMPLETADA,
            paymentMethod = PaymentMethod.EFECTIVO,
            draftItems = listOf(
                DraftSaleItem("garment_$shirtId", shirtId, "770123456001", "Camisa Polo", "M", 80000.0, 2),
                DraftSaleItem("garment_$jeanId", jeanId, "770123456002", "Jean Slim", "32", 120000.0, 1)
            )
        ).getOrThrow()

        var garmentsAfterSale = repository.observeGarments().first().associateBy { it.id }
        assertEquals(8, garmentsAfterSale[shirtId]!!.stock)
        assertEquals(2, garmentsAfterSale[jeanId]!!.stock)

        // 2. Edit the completed sale to add 1 more custom-priced product + 1 more shirt (total 5 units)
        repository.saveSaleOrder(
            existingOrderId = orderId,
            targetStatus = SaleStatus.COMPLETADA,
            paymentMethod = PaymentMethod.TARJETA,
            draftItems = listOf(
                DraftSaleItem("garment_$shirtId", shirtId, "770123456001", "Camisa Polo", "M", 80000.0, 3),
                DraftSaleItem("garment_$jeanId", jeanId, "770123456002", "Jean Slim", "32", 120000.0, 1),
                DraftSaleItem("custom_1", null, "", "Correa Cuero", "Única", 45000.0, 1)
            )
        ).getOrThrow()

        garmentsAfterSale = repository.observeGarments().first().associateBy { it.id }
        assertEquals(7, garmentsAfterSale[shirtId]!!.stock)
        assertEquals(2, garmentsAfterSale[jeanId]!!.stock)

        val editedOrder = repository.observeSales().first().first { it.order.id == orderId }
        assertEquals(405000.0, editedOrder.order.totalAmount, 0.01)
        assertEquals(5, editedOrder.order.totalUnits)

        // 3. Cancel the edited sale -> all inventory stock is restored
        repository.saveSaleOrder(
            existingOrderId = orderId,
            targetStatus = SaleStatus.CANCELADA,
            paymentMethod = PaymentMethod.TARJETA,
            draftItems = listOf(
                DraftSaleItem("garment_$shirtId", shirtId, "770123456001", "Camisa Polo", "M", 80000.0, 3),
                DraftSaleItem("garment_$jeanId", jeanId, "770123456002", "Jean Slim", "32", 120000.0, 1),
                DraftSaleItem("custom_1", null, "", "Correa Cuero", "Única", 45000.0, 1)
            )
        ).getOrThrow()

        garmentsAfterSale = repository.observeGarments().first().associateBy { it.id }
        assertEquals(10, garmentsAfterSale[shirtId]!!.stock)
        assertEquals(3, garmentsAfterSale[jeanId]!!.stock)
    }
}
