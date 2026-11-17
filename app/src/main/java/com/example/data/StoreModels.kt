package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class PaymentMethod(val label: String) {
    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod {
            return entries.firstOrNull { it.name == raw } ?: EFECTIVO
        }
    }
}

enum class SaleStatus(val label: String) {
    COMPLETADA("Completada"),
    CANCELADA("Cancelada");

    companion object {
        fun fromRaw(raw: String?): SaleStatus {
            return entries.firstOrNull { it.name == raw } ?: COMPLETADA
        }
    }
}

@Entity(
    tableName = "garments",
    indices = [Index(value = ["barcode"])]
)
data class GarmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val barcode: String = "",
    val name: String = "",
    val size: String = "M",
    val price: Double = 0.0,
    val stock: Int = 0,
    val lowStockThreshold: Int = 3,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean
        get() = stock <= 0

    val isLowStock: Boolean
        get() = stock in 1..lowStockThreshold

    val needsRestockAlert: Boolean
        get() = stock <= lowStockThreshold
}

@Entity(tableName = "sale_orders")
data class SaleOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val status: String = SaleStatus.COMPLETADA.name,
    val paymentMethod: String = PaymentMethod.EFECTIVO.name,
    val totalAmount: Double = 0.0,
    val totalUnits: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val statusEnum: SaleStatus
        get() = SaleStatus.fromRaw(status)

    val paymentMethodEnum: PaymentMethod
        get() = PaymentMethod.fromRaw(paymentMethod)

    val formattedNumber: String
        get() = "#${id.toString().padStart(4, '0')}"
}

@Entity(
    tableName = "sale_line_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class SaleLineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderId: Long = 0L,
    val garmentId: Long? = null,
    val barcode: String = "",
    val productName: String = "",
    val size: String = "M",
    val unitPrice: Double = 0.0,
    val quantity: Int = 1
) {
    val subtotal: Double
        get() = unitPrice * quantity
}

data class SaleOrderWithItems(
    @Embedded val order: SaleOrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<SaleLineItemEntity>
)

data class DraftSaleItem(
    val key: String,
    val garmentId: Long?,
    val barcode: String,
    val productName: String,
    val size: String,
    val unitPrice: Double,
    val quantity: Int,
    val maxInventoryStock: Int? = null
) {
    val subtotal: Double
        get() = unitPrice * quantity
}
