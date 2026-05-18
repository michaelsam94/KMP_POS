package com.sahmfood.pos.android.ui

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
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sahmfood.pos.android.ui.cart.CartScreen
import com.sahmfood.pos.android.ui.order.OrderListScreen
import com.sahmfood.pos.android.ui.payment.PaymentScreen
import com.sahmfood.pos.android.ui.theme.PosTheme
import com.sahmfood.pos.android.ui.transaction.TransactionScreen

sealed class Screen(val route: String, val label: String) {
    object Cart        : Screen("cart", "Cart")
    object Orders      : Screen("orders", "Orders")
    object Transactions: Screen("transactions", "History")
    object Payment     : Screen("payment/{orderId}", "Payment") {
        fun createRoute(orderId: String) = "payment/$orderId"
    }
}

@Composable
fun PosApp() {
    PosTheme {
        val navController = rememberNavController()

        val bottomItems = listOf(Screen.Cart, Screen.Orders, Screen.Transactions)

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDest = navBackStackEntry?.destination
                    bottomItems.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Cart         -> Icons.Default.ShoppingCart
                                        Screen.Orders       -> Icons.Default.List
                                        Screen.Transactions -> Icons.Default.History
                                        else                -> Icons.Default.ShoppingCart
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label     = { Text(screen.label) },
                            selected  = currentDest?.hierarchy?.any { it.route == screen.route } == true,
                            onClick   = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = Screen.Cart.route,
                modifier         = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Cart.route) {
                    CartScreen(
                        onProceedToPayment = { orderId ->
                            navController.navigate(Screen.Payment.createRoute(orderId))
                        }
                    )
                }
                composable(Screen.Orders.route) {
                    OrderListScreen()
                }
                composable(Screen.Transactions.route) {
                    TransactionScreen()
                }
                composable(Screen.Payment.route) { backStack ->
                    val orderId = backStack.arguments?.getString("orderId") ?: ""
                    PaymentScreen(
                        orderId    = orderId,
                        onComplete = { navController.popBackStack(Screen.Cart.route, false) }
                    )
                }
            }
        }
    }
}
