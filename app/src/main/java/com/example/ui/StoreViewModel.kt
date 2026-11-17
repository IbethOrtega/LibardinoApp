package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DraftSaleItem
import com.example.data.GarmentEntity
import com.example.data.PaymentMethod
import com.example.data.SaleOrderWithItems
import com.example.data.SaleStatus
import com.example.data.StoreRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ScannerMode(val label: String) {
    SELL("Para vender"),
    INVENTORY("Para inventario")
}

data class ChartPoint(
    val label: String,
    val revenue: Double,
    val units: Int
)

data class TopProductStat(
    val productName: String,
    val size: String,
    val units: Int,
    val revenue: Double
)

data class DashboardStats(
    val todayRevenue: Double = 0.0,
    val todayUnits: Int = 0,
    val todayCompletedCount: Int = 0,
    val todayCancelledCount: Int = 0,
    val lowStockCount: Int = 0,
    val totalInventoryUnits: Int = 0,
    val totalSkus: Int = 0,
    val hourlyPointsToday: List<ChartPoint> = emptyList(),
    val last7DaysPoints: List<ChartPoint> = emptyList(),
    val topProductsToday: List<TopProductStat> = emptyList()
)

data class ActiveSaleEditorState(
    val isOpen: Boolean = false,
    val editingOrderId: Long? = null,
    val editingOrderNumber: String? = null,
    val originalStatus: SaleStatus? = null,
    val items: List<DraftSaleItem> = emptyList(),
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO
) {
    val totalAmount: Double get() = items.sumOf { it.subtotal }
    val totalUnits: Int get() = items.sumOf { it.quantity }
    val isEditing: Boolean get() = editingOrderId != null
}

sealed interface ScanResolution {
    data class MatchedGarment(val barcode: String, val garment: GarmentEntity) : ScanResolution
    data class NewBarcode(val barcode: String) : ScanResolution
}

class StoreViewModel(
    private val repository: StoreRepository
) : ViewModel() {

    val garments: StateFlow<List<GarmentEntity>> = repository.observeGarments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val salesOrders: StateFlow<List<SaleOrderWithItems>> = repository.observeSales()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _lowStockOnly = MutableStateFlow(false)
    val lowStockOnly = _lowStockOnly.asStateFlow()

    val lowStockAlerts: StateFlow<List<GarmentEntity>> = garments.map { list ->
        list.filter { it.needsRestockAlert }.sortedBy { it.stock }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filteredGarments: StateFlow<List<GarmentEntity>> = combine(
        garments,
        _searchQuery,
        _lowStockOnly
    ) { list, query, lowOnly ->
        val q = query.trim().lowercase()
        list.filter { item ->
            val matchesQuery = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.barcode.lowercase().contains(q) ||
                item.size.lowercase().contains(q)
            val matchesLow = !lowOnly || item.needsRestockAlert
            matchesQuery && matchesLow
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(
        garments,
        salesOrders
    ) { garmentList, orders ->
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayOrders = orders.filter { it.order.createdAt >= startOfToday }
        val todayCompleted = todayOrders.filter { it.order.statusEnum == SaleStatus.COMPLETADA }
        val todayCancelled = todayOrders.filter { it.order.statusEnum == SaleStatus.CANCELADA }

        // Hourly blocks for today (8h, 10h, 12h, 14h, 16h, 18h, 20h)
        val hourBuckets = listOf(8, 10, 12, 14, 16, 18, 20)
        val cal = Calendar.getInstance()
        val hourlyPoints = hourBuckets.map { bucketStart ->
            val matching = todayCompleted.filter { orderWithItems ->
                cal.timeInMillis = orderWithItems.order.createdAt
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                when (bucketStart) {
                    8 -> hour < 10
                    20 -> hour >= 20
                    else -> hour in bucketStart until (bucketStart + 2)
                }
            }
            ChartPoint(
                label = "${bucketStart}h",
                revenue = matching.sumOf { it.order.totalAmount },
                units = matching.sumOf { it.order.totalUnits }
            )
        }

        // Last 7 days chart
        val dayFmt = SimpleDateFormat("EEE", Locale("es", "CO"))
        val allCompleted = orders.filter { it.order.statusEnum == SaleStatus.COMPLETADA }
        val last7DaysPoints = (6 downTo 0).map { daysAgo ->
            val dayStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = dayStart.timeInMillis
            val endMs = startMs + 24L * 60L * 60L * 1000L
            val dayOrders = allCompleted.filter { it.order.createdAt in startMs until endMs }
            val label = dayFmt.format(Date(startMs)).replace(".", "").take(3).uppercase()
            ChartPoint(
                label = label,
                revenue = dayOrders.sumOf { it.order.totalAmount },
                units = dayOrders.sumOf { it.order.totalUnits }
            )
        }

        // Top products sold today
        val topProductsToday = todayCompleted
            .flatMap { it.items }
            .groupBy { "${it.productName.trim().lowercase()}|${it.size.trim().lowercase()}" }
            .map { (_, lines) ->
                val sample = lines.first()
                TopProductStat(
                    productName = sample.productName,
                    size = sample.size,
                    units = lines.sumOf { it.quantity },
                    revenue = lines.sumOf { it.subtotal }
                )
            }
            .sortedByDescending { it.units }
            .take(5)

        DashboardStats(
            todayRevenue = todayCompleted.sumOf { it.order.totalAmount },
            todayUnits = todayCompleted.sumOf { it.order.totalUnits },
            todayCompletedCount = todayCompleted.size,
            todayCancelledCount = todayCancelled.size,
            lowStockCount = garmentList.count { it.needsRestockAlert },
            totalInventoryUnits = garmentList.sumOf { it.stock },
            totalSkus = garmentList.size,
            hourlyPointsToday = hourlyPoints,
            last7DaysPoints = last7DaysPoints,
            topProductsToday = topProductsToday
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardStats())

    private val _scannerMode = MutableStateFlow(ScannerMode.SELL)
    val scannerMode = _scannerMode.asStateFlow()

    private val _scanResolution = MutableStateFlow<ScanResolution?>(null)
    val scanResolution = _scanResolution.asStateFlow()

    private val _activeSale = MutableStateFlow(ActiveSaleEditorState())
    val activeSale = _activeSale.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy = _isBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLowStockOnly(enabled: Boolean) {
        _lowStockOnly.value = enabled
    }

    fun toggleLowStockOnly() {
        _lowStockOnly.update { !it }
    }

    fun setScannerMode(mode: ScannerMode) {
        _scannerMode.value = mode
    }

    fun resolveBarcode(rawCode: String) {
        val code = rawCode.trim()
        if (code.isEmpty()) return
        val match = garments.value.firstOrNull { it.barcode.equals(code, ignoreCase = true) }
        _scanResolution.value = if (match != null) {
            ScanResolution.MatchedGarment(code, match)
        } else {
            ScanResolution.NewBarcode(code)
        }
    }

    fun clearScanResolution() {
        _scanResolution.value = null
    }

    // --- Inventory CRUD ---

    fun saveGarment(
        garmentId: Long?,
        barcode: String,
        name: String,
        size: String,
        price: Double,
        stock: Int,
        lowStockThreshold: Int,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveGarment(
                garmentId = garmentId,
                barcode = barcode,
                name = name,
                size = size,
                price = price,
                stock = stock,
                lowStockThreshold = lowStockThreshold
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    emitMessage(if (garmentId == null || garmentId <= 0L) "Prenda registrada en Libardino." else "Prenda actualizada.")
                    onSuccess()
                },
                onFailure = { err ->
                    emitMessage(err.localizedMessage ?: "No se pudo guardar la prenda.")
                }
            )
        }
    }

    fun adjustStock(garmentId: Long, delta: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.adjustStock(garmentId, delta)
            _isBusy.value = false
            result.fold(
                onSuccess = { newStock ->
                    emitMessage("Stock actualizado: $newStock unidades.")
                    onSuccess()
                },
                onFailure = { err ->
                    emitMessage(err.localizedMessage ?: "No se pudo ajustar el stock.")
                }
            )
        }
    }

    fun deleteGarment(garmentId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteGarment(garmentId)
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    emitMessage("Prenda eliminada.")
                    onSuccess()
                },
                onFailure = { err ->
                    emitMessage(err.localizedMessage ?: "No se pudo eliminar la prenda.")
                }
            )
        }
    }

    // --- Sales Order Submenu (Create, Edit, Complete, Cancel) ---

    fun openNewSaleEditor() {
        _activeSale.update { current ->
            if (current.isEditing) {
                ActiveSaleEditorState(isOpen = true)
            } else {
                current.copy(isOpen = true)
            }
        }
    }

    fun openEditSaleEditor(orderWithItems: SaleOrderWithItems) {
        val currentGarmentsById = garments.value.associateBy { it.id }
        val wasCompleted = orderWithItems.order.statusEnum == SaleStatus.COMPLETADA

        val draftItems = orderWithItems.items.map { line ->
            val currentStock = line.garmentId?.let { currentGarmentsById[it]?.stock }
            val maxAllowed = if (currentStock != null) {
                currentStock + if (wasCompleted) line.quantity else 0
            } else {
                null
            }
            DraftSaleItem(
                key = if (line.garmentId != null) "garment_${line.garmentId}" else "custom_${line.id}",
                garmentId = line.garmentId,
                barcode = line.barcode,
                productName = line.productName,
                size = line.size,
                unitPrice = line.unitPrice,
                quantity = line.quantity,
                maxInventoryStock = maxAllowed
            )
        }

        _activeSale.value = ActiveSaleEditorState(
            isOpen = true,
            editingOrderId = orderWithItems.order.id,
            editingOrderNumber = orderWithItems.order.formattedNumber,
            originalStatus = orderWithItems.order.statusEnum,
            items = draftItems,
            paymentMethod = orderWithItems.order.paymentMethodEnum
        )
    }

    fun closeSaleEditor() {
        _activeSale.update { it.copy(isOpen = false) }
    }

    fun discardDraftAndClose() {
        _activeSale.value = ActiveSaleEditorState(isOpen = false)
    }

    fun setSalePaymentMethod(method: PaymentMethod) {
        _activeSale.update { it.copy(paymentMethod = method) }
    }

    fun addInventoryGarmentToSale(garment: GarmentEntity, quantity: Int = 1, openEditor: Boolean = false) {
        val key = "garment_${garment.id}"
        _activeSale.update { state ->
            val existing = state.items.firstOrNull { it.key == key }
            val baseOriginalQty = if (state.originalStatus == SaleStatus.COMPLETADA && state.editingOrderId != null) {
                existing?.maxInventoryStock ?: garment.stock
            } else {
                garment.stock
            }

            if (baseOriginalQty <= 0) {
                emitMessage("No hay stock disponible de '${garment.name}'.")
                return@update state
            }

            val updatedItems = if (existing != null) {
                val maxStock = existing.maxInventoryStock ?: garment.stock
                val nextQty = (existing.quantity + quantity).coerceAtMost(maxStock)
                if (nextQty == existing.quantity) {
                    emitMessage("Máximo stock disponible alcanzado ($maxStock).")
                }
                state.items.map { if (it.key == key) it.copy(quantity = nextQty) else it }
            } else {
                state.items + DraftSaleItem(
                    key = key,
                    garmentId = garment.id,
                    barcode = garment.barcode,
                    productName = garment.name,
                    size = garment.size,
                    unitPrice = garment.price,
                    quantity = quantity.coerceIn(1, garment.stock),
                    maxInventoryStock = garment.stock
                )
            }

            state.copy(
                isOpen = if (openEditor) true else state.isOpen,
                items = updatedItems
            )
        }
    }

    fun addCustomPriceItemToSale(
        productName: String,
        size: String,
        unitPrice: Double,
        quantity: Int
    ) {
        val cleanName = productName.trim().ifEmpty { "Prenda por precio" }
        val cleanSize = size.trim().ifEmpty { "Única" }
        if (unitPrice < 0.0 || quantity <= 0) return

        val key = "custom_${UUID.randomUUID()}"
        _activeSale.update { state ->
            state.copy(
                items = state.items + DraftSaleItem(
                    key = key,
                    garmentId = null,
                    barcode = "",
                    productName = cleanName,
                    size = cleanSize,
                    unitPrice = unitPrice,
                    quantity = quantity,
                    maxInventoryStock = null
                )
            )
        }
    }

    fun updateDraftItemQuantity(key: String, newQty: Int) {
        if (newQty <= 0) {
            removeDraftItem(key)
            return
        }
        _activeSale.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.key == key) {
                        val capped = item.maxInventoryStock?.let { newQty.coerceAtMost(it) } ?: newQty
                        item.copy(quantity = capped)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun removeDraftItem(key: String) {
        _activeSale.update { state ->
            state.copy(items = state.items.filterNot { it.key == key })
        }
    }

    fun saveActiveSale(targetStatus: SaleStatus) {
        val snapshot = _activeSale.value
        if (snapshot.items.isEmpty()) {
            if (targetStatus == SaleStatus.CANCELADA) {
                discardDraftAndClose()
            } else {
                emitMessage("Agrega al menos un producto para completar la venta.")
            }
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveSaleOrder(
                existingOrderId = snapshot.editingOrderId,
                targetStatus = targetStatus,
                paymentMethod = snapshot.paymentMethod,
                draftItems = snapshot.items
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    val msg = when {
                        targetStatus == SaleStatus.CANCELADA ->
                            if (snapshot.isEditing) "Venta cancelada y stock devuelto." else "Venta guardada como cancelada."
                        snapshot.isEditing -> "Venta ${snapshot.editingOrderNumber ?: ""} actualizada."
                        else -> "Venta completada correctamente."
                    }
                    _activeSale.value = ActiveSaleEditorState(isOpen = false)
                    emitMessage(msg)
                },
                onFailure = { err ->
                    emitMessage(err.localizedMessage ?: "No se pudo guardar la venta.")
                }
            )
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
