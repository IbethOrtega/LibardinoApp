package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.LibardinoDatabase
import com.example.data.SaleStatus
import com.example.data.StoreRepository
import com.example.ui.ScannerMode
import com.example.ui.StoreViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.SaleEditorSheet
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.LibardinoTheme
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute

@Serializable
data object InventoryRoute

@Serializable
data object ScannerRoute

@Serializable
data object SalesRoute

private data class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_DESTINATIONS = listOf(
    TopLevelDestination(
        route = DashboardRoute,
        label = "Inicio",
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_dashboard"
    ),
    TopLevelDestination(
        route = InventoryRoute,
        label = "Inventario",
        selectedIcon = Icons.Rounded.Checkroom,
        unselectedIcon = Icons.Outlined.Checkroom,
        testTag = "nav_inventory"
    ),
    TopLevelDestination(
        route = ScannerRoute,
        label = "Escáner",
        selectedIcon = Icons.Rounded.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_scanner"
    ),
    TopLevelDestination(
        route = SalesRoute,
        label = "Ventas",
        selectedIcon = Icons.Rounded.PointOfSale,
        unselectedIcon = Icons.Outlined.PointOfSale,
        testTag = "nav_sales"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LibardinoTheme {
                LibardinoApp()
            }
        }
    }
}

@Composable
fun LibardinoApp() {
    val viewModel: StoreViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = LibardinoDatabase.getInstance(app)
                StoreViewModel(StoreRepository(db))
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    val lowStockAlerts by viewModel.lowStockAlerts.collectAsStateWithLifecycle()
    val activeSale by viewModel.activeSale.collectAsStateWithLifecycle()
    val garments by viewModel.garments.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTo(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    DestinationIconWithBadge(
                                        dest = dest,
                                        selected = selected,
                                        lowStockCount = lowStockAlerts.size,
                                        activeSaleUnits = activeSale.totalUnits
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    DestinationIconWithBadge(
                                        dest = dest,
                                        selected = selected,
                                        lowStockCount = lowStockAlerts.size,
                                        activeSaleUnits = activeSale.totalUnits
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = DashboardRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 960.dp)
                    ) {
                        composable<DashboardRoute> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onStartNewSale = viewModel::openNewSaleEditor,
                                onOpenScanner = {
                                    viewModel.setScannerMode(ScannerMode.SELL)
                                    navigateTo(ScannerRoute)
                                },
                                onViewLowStockAlerts = {
                                    viewModel.setLowStockOnly(true)
                                    navigateTo(InventoryRoute)
                                }
                            )
                        }
                        composable<InventoryRoute> {
                            InventoryScreen(
                                viewModel = viewModel,
                                onOpenScannerForInventory = {
                                    viewModel.setScannerMode(ScannerMode.INVENTORY)
                                    navigateTo(ScannerRoute)
                                }
                            )
                        }
                        composable<ScannerRoute> {
                            ScannerScreen(viewModel = viewModel)
                        }
                        composable<SalesRoute> {
                            SalesScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    if (activeSale.isOpen) {
        SaleEditorSheet(
            saleState = activeSale,
            inventoryGarments = garments,
            isBusy = isBusy,
            onDismiss = viewModel::closeSaleEditor,
            onAddInventoryGarment = { garment ->
                viewModel.addInventoryGarmentToSale(garment, quantity = 1)
            },
            onAddCustomPriceItem = viewModel::addCustomPriceItemToSale,
            onUpdateItemQuantity = viewModel::updateDraftItemQuantity,
            onRemoveItem = viewModel::removeDraftItem,
            onSelectPaymentMethod = viewModel::setSalePaymentMethod,
            onCompleteSale = { viewModel.saveActiveSale(SaleStatus.COMPLETADA) },
            onCancelSale = { viewModel.saveActiveSale(SaleStatus.CANCELADA) },
            onDiscardDraft = viewModel::discardDraftAndClose
        )
    }
}

@Composable
private fun DestinationIconWithBadge(
    dest: TopLevelDestination,
    selected: Boolean,
    lowStockCount: Int,
    activeSaleUnits: Int
) {
    val badgeCount = when (dest.route) {
        InventoryRoute -> lowStockCount
        SalesRoute -> activeSaleUnits
        else -> 0
    }
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("$badgeCount")
                }
            }
        ) {
            Icon(
                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                contentDescription = dest.label
            )
        }
    } else {
        Icon(
            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.label
        )
    }
}
