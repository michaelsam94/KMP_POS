package com.sahmfood.pos.ui.payment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sahmfood.pos.domain.model.PaymentMethod
import com.sahmfood.pos.presentation.payment.PaymentViewModel
import com.sahmfood.pos.ui.components.ErrorBanner
import com.sahmfood.pos.ui.components.LoadingOverlay
import com.sahmfood.pos.ui.components.SectionDivider
import com.sahmfood.pos.ui.components.TotalSummaryRow
import com.sahmfood.pos.ui.di.rememberCartViewModel
import com.sahmfood.pos.ui.di.rememberPaymentViewModel
import com.sahmfood.pos.util.toMoneyString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    orderId: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PaymentViewModel = rememberPaymentViewModel(),
    cartViewModel: com.sahmfood.pos.presentation.cart.CartViewModel = rememberCartViewModel(),
) {
    val payState by viewModel.state.collectAsState()

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
            change = payState.transaction?.change ?: 0.0,
            total = payState.transaction?.amount ?: 0.0,
            onDone = {
                viewModel.resetPayment()
                onComplete()
            },
        )
        return
    }

    val canPay = viewModel.canConfirmPayment() && payState.order != null

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Payment", fontWeight = FontWeight.Bold) })
        },
        bottomBar = {
            if (!payState.isLoading) {
                PaymentBottomBar(
                    canPay = canPay,
                    isProcessing = payState.isProcessing,
                    paymentMethod = payState.paymentMethod,
                    error = payState.error,
                    showHint = payState.order != null,
                    onConfirm = { viewModel.processPayment() },
                )
            }
        },
    ) { padding ->
        if (payState.isLoading) {
            LoadingOverlay()
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
        ) {
            payState.breakdown?.let { b ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Order Summary",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        TotalSummaryRow("Subtotal", "SAR ${b.subtotal.toMoneyString()}")
                        TotalSummaryRow(
                            "Tax (${(b.taxRate * 100).toInt()}%)",
                            "SAR ${b.taxAmount.toMoneyString()}",
                        )
                        if (b.discountAmount > 0) {
                            TotalSummaryRow(
                                "Discount",
                                "-SAR ${b.discountAmount.toMoneyString()}",
                            )
                        }
                        SectionDivider()
                        TotalSummaryRow(
                            "TOTAL DUE",
                            "SAR ${b.total.toMoneyString()}",
                            isBold = true,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Payment Method",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PaymentMethod.CASH to Icons.Default.Money,
                    PaymentMethod.CARD to Icons.Default.CreditCard,
                    PaymentMethod.MOBILE_WALLET to Icons.Default.PhoneAndroid,
                ).forEach { (method, icon) ->
                    FilterChip(
                        selected = payState.paymentMethod == method,
                        onClick = { viewModel.setPaymentMethod(method) },
                        label = { Text(method.name.replace("_", " ")) },
                        leadingIcon = { Icon(icon, null, Modifier.size(16.dp)) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            when (payState.paymentMethod) {
                PaymentMethod.CASH -> CashPaymentSection(total = payState.breakdown?.total ?: 0.0)
                PaymentMethod.CARD ->
                    CardPaymentSection(
                        cardNumber = payState.cardNumber,
                        cardExpiry = payState.cardExpiry,
                        cardCvv = payState.cardCvv,
                        onCardNumberChange = viewModel::setCardNumber,
                        onExpiryChange = viewModel::setCardExpiry,
                        onCvvChange = viewModel::setCardCvv,
                    )
                PaymentMethod.MOBILE_WALLET ->
                    WalletPaymentSection(
                        phone = payState.walletPhone,
                        onPhoneChange = viewModel::setWalletPhone,
                    )
                PaymentMethod.SPLIT -> Unit
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaymentBottomBar(
    canPay: Boolean,
    isProcessing: Boolean,
    paymentMethod: PaymentMethod,
    error: String?,
    showHint: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        error?.let {
            ErrorBanner(it)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = onConfirm,
            enabled = canPay && !isProcessing,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Confirm Payment", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (!canPay && showHint) {
            Spacer(Modifier.height(8.dp))
            Text(
                hintForMethod(paymentMethod),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CashPaymentSection(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Cash payment",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Amount to collect",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "SAR ${total.toMoneyString()}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Exact order total — no change required",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
    onCvvChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val expiryFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Card details", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(
                value = cardNumber,
                onValueChange = onCardNumberChange,
                label = { Text("Card number") },
                placeholder = { Text("0000 0000 0000 0000") },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                keyboardActions = KeyboardActions(onNext = { expiryFocus.requestFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = CardNumberVisualTransformation(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cardExpiry,
                    onValueChange = onExpiryChange,
                    label = { Text("Expiry (MMYY)") },
                    placeholder = { Text("1228") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions = KeyboardActions(onNext = { cvvFocus.requestFocus() }),
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(expiryFocus),
                )
                OutlinedTextField(
                    value = cardCvv,
                    onValueChange = onCvvChange,
                    label = { Text("CVV") },
                    placeholder = { Text("123") },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.weight(1f).focusRequester(cvvFocus),
                )
            }
        }
    }
}

@Composable
private fun WalletPaymentSection(
    phone: String,
    onPhoneChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Mobile wallet", fontWeight = FontWeight.SemiBold)
            }
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Phone number") },
                placeholder = { Text("05XXXXXXXX") },
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("+966 ") },
            )
        }
    }
}

/** Groups digits as "1234 5678 …" with correct cursor offset mapping. */
private class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted =
            buildString {
                digits.forEachIndexed { index, char ->
                    if (index > 0 && index % 4 == 0) append(' ')
                    append(char)
                }
            }

        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    val spacesBefore = (offset - 1) / 4
                    return (offset + spacesBefore).coerceAtMost(formatted.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0
                    var spaces = 0
                    for (i in 0 until offset.coerceAtMost(formatted.length)) {
                        if (formatted[i] == ' ') spaces++
                    }
                    return (offset - spaces).coerceIn(0, digits.length)
                }
            }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

private fun hintForMethod(method: PaymentMethod): String =
    when (method) {
        PaymentMethod.CARD -> "Fill in all card fields to continue"
        PaymentMethod.MOBILE_WALLET -> "Enter phone number to continue"
        else -> ""
    }

@Composable
private fun PaymentSuccessScreen(
    receiptText: String,
    change: Double,
    total: Double,
    onDone: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Success",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp),
        )
        Text(
            "Payment Successful!",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text("SAR ${total.toMoneyString()} paid", style = MaterialTheme.typography.titleLarge)
        if (change > 0) {
            Text(
                "Change: SAR ${change.toMoneyString()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
        }
        SectionDivider()
        if (receiptText.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Text(
                    text = receiptText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("New Order")
        }
    }
}
