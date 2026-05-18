package com.sahmfood.pos.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sahmfood.pos.ui.cart.CartScreen
import com.sahmfood.pos.ui.order.OrderListScreen
import com.sahmfood.pos.ui.payment.PaymentScreen
import com.sahmfood.pos.ui.theme.PosTheme
import com.sahmfood.pos.ui.transaction.TransactionScreen

private enum class MainTab { Cart, Orders, History }

@Composable
fun PosApp() {
    PosTheme {
        var selectedTab by rememberSaveable { mutableStateOf(MainTab.Cart) }
        var paymentOrderId by rememberSaveable { mutableStateOf<String?>(null) }

        if (paymentOrderId != null) {
            PaymentScreen(
                orderId = paymentOrderId!!,
                onComplete = {
                    paymentOrderId = null
                    selectedTab = MainTab.Cart
                },
            )
            return@PosTheme
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Cart") },
                        label = { Text("Cart") },
                        selected = selectedTab == MainTab.Cart,
                        onClick = { selectedTab = MainTab.Cart },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.List, contentDescription = "Orders") },
                        label = { Text("Orders") },
                        selected = selectedTab == MainTab.Orders,
                        onClick = { selectedTab = MainTab.Orders },
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        selected = selectedTab == MainTab.History,
                        onClick = { selectedTab = MainTab.History },
                    )
                }
            },
        ) { innerPadding ->
            when (selectedTab) {
                MainTab.Cart ->
                    CartScreen(
                        modifier = Modifier.padding(innerPadding),
                        onProceedToPayment = { orderId -> paymentOrderId = orderId },
                    )
                MainTab.Orders -> OrderListScreen(modifier = Modifier.padding(innerPadding))
                MainTab.History -> TransactionScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
