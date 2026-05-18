package com.sahmfood.pos.android.ui.payment

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sahmfood.pos.android.di.CartAndroidViewModel
import com.sahmfood.pos.android.di.PaymentAndroidViewModel
import com.sahmfood.pos.android.ui.components.ErrorBanner
import com.sahmfood.pos.android.ui.components.LoadingOverlay
import com.sahmfood.pos.android.ui.components.SectionDivider
import com.sahmfood.pos.android.ui.components.TotalSummaryRow
import com.sahmfood.pos.domain.model.PaymentMethod
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    orderId: String,
    onComplete: () -> Unit,
    viewModel: PaymentAndroidViewModel = koinViewModel()
) {
    val activity = LocalContext.current as ComponentActivity
    val cartViewModel: CartAndroidViewModel = koinViewModel(viewModelStoreOwner = activity)

    val payState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    // Empty the cart as soon as payment succeeds (before user taps "New Order")
    LaunchedEffect(payState.paymentComplete) {
        if (payState.paymentComplete) {
            cartViewModel.startNewOrder()
        }
    }

    if (payState.paymentComplete) {
        PaymentSuccessScreen(
            receiptText = payState.receiptText ?: "",
            change      = payState.transaction?.change ?: 0.0,
            total       = payState.transaction?.amount ?: 0.0,
            onDone      = {
                viewModel.resetPayment()
                onComplete()
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Payment", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        if (payState.isLoading) {
            LoadingOverlay()
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            payState.breakdown?.let { b ->
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Order Summary",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        TotalSummaryRow("Subtotal", "SAR ${String.format("%.2f", b.subtotal)}")
                        TotalSummaryRow(
                            "Tax (${(b.taxRate * 100).toInt()}%)",
                            "SAR ${String.format("%.2f", b.taxAmount)}"
                        )
                        if (b.discountAmount > 0) {
                            TotalSummaryRow(
                                "Discount",
                                "-SAR ${String.format("%.2f", b.discountAmount)}"
                            )
                        }
                        SectionDivider()
                        TotalSummaryRow(
                            "TOTAL DUE",
                            "SAR ${String.format("%.2f", b.total)}",
                            isBold = true
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Payment Method",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PaymentMethod.CASH to Icons.Default.Money,
                    PaymentMethod.CARD to Icons.Default.CreditCard,
                    PaymentMethod.MOBILE_WALLET to Icons.Default.PhoneAndroid
                ).forEach { (method, icon) ->
                    FilterChip(
                        selected = payState.paymentMethod == method,
                        onClick  = { viewModel.setPaymentMethod(method) },
                        label    = { Text(method.name.replace("_", " ")) },
                        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            when (payState.paymentMethod) {
                PaymentMethod.CASH -> CashPaymentSection(total = payState.breakdown?.total ?: 0.0)
                PaymentMethod.CARD -> CardPaymentSection(
                    cardNumber = payState.cardNumber,
                    cardExpiry = payState.cardExpiry,
                    cardCvv    = payState.cardCvv,
                    onCardNumberChange = viewModel::setCardNumber,
                    onExpiryChange     = viewModel::setCardExpiry,
                    onCvvChange        = viewModel::setCardCvv
                )
                PaymentMethod.MOBILE_WALLET -> WalletPaymentSection(
                    phone = payState.walletPhone,
                    onPhoneChange = viewModel::setWalletPhone
                )
                PaymentMethod.SPLIT -> Unit
            }

            payState.error?.let {
                Spacer(Modifier.height(12.dp))
                ErrorBanner(it)
            }

            Spacer(Modifier.height(32.dp))

            val canPay = viewModel.canConfirmPayment() && payState.order != null

            Button(
                onClick  = { viewModel.processPayment() },
                enabled  = canPay && !payState.isProcessing,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (payState.isProcessing) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Confirm Payment", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (!canPay && payState.order != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    hintForMethod(payState.paymentMethod),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CashPaymentSection(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Cash payment",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Amount to collect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "SAR ${String.format("%.2f", total)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Exact order total — no change required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun CardPaymentSection(
    cardNumber: String,
    cardExpiry: String,
    cardCvv: String,
    onCardNumberChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Card details", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(
                value         = cardNumber,
                onValueChange = onCardNumberChange,
                label         = { Text("Card number") },
                placeholder   = { Text("0000 0000 0000 0000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                visualTransformation = CardNumberVisualTransformation()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value         = cardExpiry,
                    onValueChange = onExpiryChange,
                    label         = { Text("Expiry (MMYY)") },
                    placeholder   = { Text("1228") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value         = cardCvv,
                    onValueChange = onCvvChange,
                    label         = { Text("CVV") },
                    placeholder   = { Text("123") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine    = true,
                    modifier      = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WalletPaymentSection(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Mobile wallet", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(
                value         = phone,
                onValueChange = onPhoneChange,
                label         = { Text("Phone number") },
                placeholder   = { Text("05XXXXXXXX") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                prefix        = { Text("+966 ") }
            )
        }
    }
}

private class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = text.text
        val grouped = digits.chunked(4).joinToString(" ")
        return androidx.compose.ui.text.input.TransformedText(
            androidx.compose.ui.text.AnnotatedString(grouped),
            androidx.compose.ui.text.input.OffsetMapping.Identity
        )
    }
}

private fun hintForMethod(method: PaymentMethod): String = when (method) {
    PaymentMethod.CARD -> "Fill in all card fields to continue"
    PaymentMethod.MOBILE_WALLET -> "Enter phone number to continue"
    else -> ""
}

@Composable
private fun PaymentSuccessScreen(
    receiptText: String,
    change: Double,
    total: Double,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            imageVector        = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(80.dp)
        )
        Text(
            "Payment Successful!",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("SAR ${String.format("%.2f", total)} paid", style = MaterialTheme.typography.titleLarge)
        if (change > 0) {
            Text(
                "Change: SAR ${String.format("%.2f", change)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        SectionDivider()
        if (receiptText.isNotBlank()) {
            Card(
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text     = receiptText,
                    style    = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick  = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text("New Order")
        }
    }
}
