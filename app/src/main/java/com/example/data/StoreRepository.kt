package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class StoreRepository(
    private val database: LibardinoDatabase
) {
    private val garmentDao = database.garmentDao()
    private val saleDao = database.saleDao()

    fun observeGarments(): Flow<List<GarmentEntity>> = garmentDao.observeAll()

    fun observeSales(): Flow<List<SaleOrderWithItems>> = saleDao.observeAllOrders()

    suspend fun saveGarment(
        garmentId: Long?,
        barcode: String,
        name: String,
        size: String,
        price: Double,
        stock: Int,
        lowStockThreshold: Int
    ): Result<Long> {
        return try {
            val cleanName = name.trim()
            val cleanSize = size.trim()
            val cleanBarcode = barcode.trim()
            require(cleanName.isNotEmpty()) { "Ingresa el nombre de la prenda." }
            require(cleanSize.isNotEmpty()) { "Ingresa la talla de la prenda." }
            require(price >= 0.0) { "El precio debe ser 0 o mayor." }
            require(stock >= 0) { "El stock debe ser 0 o mayor." }
            require(lowStockThreshold >= 0) { "La alerta de bajo stock debe ser 0 o mayor." }

            val now = System.currentTimeMillis()
            val savedId = database.withTransaction {
                if (cleanBarcode.isNotEmpty()) {
                    val duplicate = garmentDao.getByBarcode(cleanBarcode)
                    if (duplicate != null && duplicate.id != (garmentId ?: 0L)) {
                        throw IllegalStateException("Ya existe una prenda con el código '$cleanBarcode' (${duplicate.name}).")
                    }
                }
                if (garmentId == null || garmentId <= 0L) {
                    garmentDao.insert(
                        GarmentEntity(
                            barcode = cleanBarcode,
                            name = cleanName,
                            size = cleanSize,
                            price = price,
                            stock = stock,
                            lowStockThreshold = lowStockThreshold,
                            updatedAt = now
                        )
                    )
                } else {
                    val existing = garmentDao.getById(garmentId)
                        ?: throw IllegalStateException("La prenda ya no existe en el inventario.")
                    garmentDao.update(
                        existing.copy(
                            barcode = cleanBarcode,
                            name = cleanName,
                            size = cleanSize,
                            price = price,
                            stock = stock,
                            lowStockThreshold = lowStockThreshold,
                            updatedAt = now
                        )
                    )
                    garmentId
                }
            }
            Result.success(savedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun adjustStock(garmentId: Long, delta: Int): Result<Int> {
        return try {
            val newStock = database.withTransaction {
                val existing = garmentDao.getById(garmentId)
                    ?: throw IllegalStateException("Prenda no encontrada.")
                val updatedStock = existing.stock + delta
                if (updatedStock < 0) {
                    throw IllegalStateException("Stock insuficiente (disponible: ${existing.stock}).")
                }
                garmentDao.update(
                    existing.copy(
                        stock = updatedStock,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                updatedStock
            }
            Result.success(newStock)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGarment(garmentId: Long): Result<Unit> {
        return try {
            garmentDao.deleteById(garmentId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSaleOrder(
        existingOrderId: Long?,
        targetStatus: SaleStatus,
        paymentMethod: PaymentMethod,
        draftItems: List<DraftSaleItem>
    ): Result<Long> {
        return try {
            require(draftItems.isNotEmpty()) { "Agrega al menos un producto a la venta." }
            draftItems.forEach { item ->
                require(item.productName.isNotBlank()) { "Todos los productos deben tener nombre." }
                require(item.size.isNotBlank()) { "Todos los productos deben tener talla." }
                require(item.quantity > 0) { "La cantidad debe ser al menos 1." }
                require(item.unitPrice >= 0.0) { "El precio no puede ser negativo." }
            }

            val now = System.currentTimeMillis()
            val totalAmount = draftItems.sumOf { it.subtotal }
            val totalUnits = draftItems.sumOf { it.quantity }

            val savedOrderId = database.withTransaction {
                val oldOrderWithItems = if (existingOrderId != null && existingOrderId > 0L) {
                    saleDao.getOrderWithItems(existingOrderId)
                        ?: throw IllegalStateException("No se encontró la venta a editar.")
                } else {
                    null
                }

                // Compute net stock changes across old vs new order state
                val oldDeductedByGarment = if (oldOrderWithItems?.order?.statusEnum == SaleStatus.COMPLETADA) {
                    oldOrderWithItems.items
                        .filter { it.garmentId != null }
                        .groupBy { it.garmentId!! }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }
                } else {
                    emptyMap()
                }

                val newDeductedByGarment = if (targetStatus == SaleStatus.COMPLETADA) {
                    draftItems
                        .filter { it.garmentId != null }
                        .groupBy { it.garmentId!! }
                        .mapValues { (_, list) -> list.sumOf { it.quantity } }
                } else {
                    emptyMap()
                }

                val affectedGarmentIds = (oldDeductedByGarment.keys + newDeductedByGarment.keys)
                for (gid in affectedGarmentIds) {
                    val garment = garmentDao.getById(gid) ?: continue
                    val oldQty = oldDeductedByGarment[gid] ?: 0
                    val newQty = newDeductedByGarment[gid] ?: 0
                    val stockDelta = oldQty - newQty
                    val nextStock = garment.stock + stockDelta
                    if (nextStock < 0) {
                        throw IllegalStateException(
                            "Stock insuficiente para '${garment.name}' (Talla ${garment.size}). Disponible: ${garment.stock}."
                        )
                    }
                    if (stockDelta != 0) {
                        garmentDao.update(
                            garment.copy(
                                stock = nextStock,
                                updatedAt = now
                            )
                        )
                    }
                }

                val orderId = if (oldOrderWithItems == null) {
                    saleDao.insertOrder(
                        SaleOrderEntity(
                            status = targetStatus.name,
                            paymentMethod = paymentMethod.name,
                            totalAmount = totalAmount,
                            totalUnits = totalUnits,
                            createdAt = now,
                            updatedAt = now
                        )
                    )
                } else {
                    saleDao.updateOrder(
                        oldOrderWithItems.order.copy(
                            status = targetStatus.name,
                            paymentMethod = paymentMethod.name,
                            totalAmount = totalAmount,
                            totalUnits = totalUnits,
                            updatedAt = now
                        )
                    )
                    saleDao.deleteLineItemsByOrderId(oldOrderWithItems.order.id)
                    oldOrderWithItems.order.id
                }

                saleDao.insertLineItems(
                    draftItems.map { item ->
                        SaleLineItemEntity(
                            orderId = orderId,
                            garmentId = item.garmentId,
                            barcode = item.barcode.trim(),
                            productName = item.productName.trim(),
                            size = item.size.trim(),
                            unitPrice = item.unitPrice,
                            quantity = item.quantity
                        )
                    }
                )
                orderId
            }
            Result.success(savedOrderId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
