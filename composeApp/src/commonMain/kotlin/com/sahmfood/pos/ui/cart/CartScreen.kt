package com.sahmfood.pos.ui.cart
import com.sahmfood.pos.util.toMoneyString

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sahmfood.pos.ui.di.rememberCartViewModel
import com.sahmfood.pos.ui.components.CartItemRow
import com.sahmfood.pos.ui.components.ErrorBanner
import com.sahmfood.pos.ui.components.LoadingOverlay
import com.sahmfood.pos.ui.components.SectionDivider
import com.sahmfood.pos.ui.components.TotalSummaryRow
import com.sahmfood.pos.data.DemoCatalog
import com.sahmfood.pos.domain.model.Product
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onProceedToPayment: (orderId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: com.sahmfood.pos.presentation.cart.CartViewModel = rememberCartViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope    = rememberCoroutineScope()
    var showProductPicker by remember { mutableStateOf(false) }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    // Show errors as snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            scope.launch { snackbar.showSnackbar(it) }
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Order", fontWeight = FontWeight.Bold)
                        state.order?.tableNumber?.takeIf { it.isNotBlank() }?.let {
                            Text("Table $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showBarcodeDialog = true }) {
                        Icon(Icons.Default.CameraAlt, "Scan Barcode")
                    }
                    IconButton(onClick = { viewModel.startNewOrder() }) {
                        Icon(Icons.Default.Delete, "Clear Cart")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick       = { showProductPicker = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Add Product", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                LoadingOverlay()
            }

            if (state.order?.items.isNullOrEmpty()) {
                EmptyCartPlaceholder(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.order!!.items, key = { it.product.id }) { item ->
                        CartItemRow(
                            item      = item,
                            onIncrease = {
                                viewModel.updateItemQuantity(item.product.id, item.quantity + 1)
                            },
                            onDecrease = {
                                viewModel.updateItemQuantity(item.product.id, item.quantity - 1)
                            },
                            onRemove   = { viewModel.removeProductFromCart(item.product.id) }
                        )
                    }
                }

                // Order summary panel
                SectionDivider()
                state.breakdown?.let { b ->
                    TotalSummaryRow("Subtotal", "SAR ${b.subtotal.toMoneyString()}")
                    TotalSummaryRow(
                        "Tax (${(b.taxRate * 100).toInt()}%)",
                        "SAR ${b.taxAmount.toMoneyString()}"
                    )
                    if (b.discountAmount > 0) {
                        TotalSummaryRow(
                            "Discount",
                            "-SAR ${b.discountAmount.toMoneyString()}",
                        )
                    }
                    SectionDivider()
                    TotalSummaryRow(
                        "TOTAL",
                        "SAR ${b.total.toMoneyString()}",
                        isBold = true
                    )
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick  = {
                        viewModel.confirmOrder { order ->
                            onProceedToPayment(order.id)
                        }
                    },
                    enabled  = state.order?.items?.isNotEmpty() == true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(56.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Proceed to Payment", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    // Product picker dialog
    if (showProductPicker) {
        ProductPickerDialog(
            onProductSelected = { product ->
                viewModel.addProductToCart(product)
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false }
        )
    }

    // Barcode scanner dialog
    if (showBarcodeDialog) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.onBarcodeScanned(barcode)
                showBarcodeDialog = false
            },
            onDismiss = { showBarcodeDialog = false }
        )
    }
}

@Composable
private fun EmptyCartPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Cart is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Tap + to add items or scan a barcode",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun ProductPickerDialog(
    onProductSelected: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val categories  = listOf("All") + DemoCatalog.products.map { it.category }.distinct()
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Add Product", fontWeight = FontWeight.Bold) },
        text    = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = { Text("Search products…") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = selectedTab) {
                    categories.forEachIndexed { idx, cat ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }) {
                            Text(cat, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    val filtered = DemoCatalog.products.filter { product ->
                        (categories[selectedTab] == "All" || product.category == categories[selectedTab]) &&
                        (searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true))
                    }
                    items(filtered) { product ->
                        Card(
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onProductSelected(product) },
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        product.category,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "SAR ${product.price.toMoneyString()}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BarcodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text("Scan Barcode") },
        text    = {
            Column {
                Text(
                    "Simulated scanner: enter barcode manually or pick from demo barcodes.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label         = { Text("Barcode") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Demo barcodes:", style = MaterialTheme.typography.labelMedium)
                DemoCatalog.products.forEach { p ->
                    TextButton(onClick = { onBarcodeScanned(p.barcode) }) {
                        Text("${p.barcode}  —  ${p.name}")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { if (manualBarcode.isNotBlank()) onBarcodeScanned(manualBarcode) },
                enabled  = manualBarcode.isNotBlank()
            ) { Text("Scan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

