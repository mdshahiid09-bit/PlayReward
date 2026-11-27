package com.playreward.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playreward.app.R
import com.playreward.app.data.model.PayoutMethods
import com.playreward.app.data.model.TransactionItem
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// SCREEN 1: WALLET HUB (Compact Overview + Navigation to Dedicated Screens)
// ============================================================================

@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    onOpenWithdraw: () -> Unit,
    onOpenConvert: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPayoutSettings: () -> Unit
) {
    val colors = PRTheme.colors
    val profile by viewModel.userProfile.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val coinsPerInr = appConfig.coinsPerInr.coerceAtLeast(1L)
    val pm = profile?.payoutMethods ?: PayoutMethods()
    val payoutSummary = buildList {
        if (pm.upiId.isNotBlank()) add("UPI ready")
        if (pm.bankAccountNumber.isNotBlank()) add("Bank ready")
    }.joinToString(" • ").ifBlank { "Tap to add UPI or Bank details" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Settings Icon
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Your Wallet", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text("Manage coins, INR & withdrawals", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                Surface(
                    modifier = Modifier.size(40.dp).bounceClick(onClick = onOpenPayoutSettings),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Settings, "Payout Settings", tint = colors.primaryIndigo, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Hero Indigo-Violet Balance Card
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.heroGradient).padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available Coins", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                            Text("${profile?.coins ?: 0}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                            Text(
                                text = "₹${String.format("%.2f", profile?.inrBalance ?: 0.0)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "$coinsPerInr Coins = ₹1 INR  •  Min Withdraw: ${appConfig.minWithdrawalCoins} Coins",
                        color = Color.White.copy(alpha = 0.86f),
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // 2-Column Dedicated Action Cards: Withdraw & Convert Coins
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PRCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = onOpenWithdraw
                ) {
                    IconBadge(Icons.Rounded.AccountBalanceWallet, colors.amberSoft, colors.amberAccent, size = 38.dp, cornerRadius = 12.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Withdraw", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text("UPI & Bank Transfer", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }

                PRCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = onOpenConvert
                ) {
                    IconBadge(Icons.Rounded.CurrencyExchange, colors.lavenderSoft, colors.primaryIndigo, size = 38.dp, cornerRadius = 12.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Convert Coins", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text("Coins to INR", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
        }

        // Payout Methods & Full History Navigation Rows
        item {
            PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(13.dp), onClick = onOpenPayoutSettings) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconBadge(Icons.Rounded.AccountBalance, colors.mintSoft, colors.mintAccent, size = 38.dp, cornerRadius = 11.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Payout Methods", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text(payoutSummary, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Recent Transactions Preview + View All
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                TextButton(onClick = onOpenHistory) {
                    Text("View all", color = colors.primaryIndigo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.primaryIndigo, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.ReceiptLong, colors.lavenderSoft, colors.primaryIndigo, size = 38.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("No Transactions Yet", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Your earnings, conversions & withdrawals appear here.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        } else {
            items(transactions.take(4), key = { it.id }) { tx ->
                TransactionRowCard(tx)
            }
        }
    }
}

// ============================================================================
// SCREEN 2: DEDICATED WITHDRAWAL SCREEN
// ============================================================================

@Composable
fun WithdrawScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenPayoutSettings: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    val pm = profile?.payoutMethods ?: PayoutMethods()
    val coinsPerInr = appConfig.coinsPerInr.coerceAtLeast(1L)
    var selectedMethod by remember { mutableStateOf("UPI") }
    var withdrawInput by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val enteredCoins = withdrawInput.toLongOrNull() ?: 0L
    val estimatedInr = enteredCoins.toDouble() / coinsPerInr.toDouble()
    val presets = remember(appConfig.minWithdrawalCoins, appConfig.maxWithdrawalCoins) {
        listOf(10000L, 25000L, 50000L, 100000L)
            .filter { it in appConfig.minWithdrawalCoins..appConfig.maxWithdrawalCoins }
            .ifEmpty { listOf(appConfig.minWithdrawalCoins) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text("Withdraw Rewards", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = colors.amberSoft) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("${profile?.coins ?: 0}", color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Payout Method Selection Cards
        item {
            PRCard(cornerRadius = 16.dp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Select Payout Method", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    TextButton(onClick = onOpenPayoutSettings) {
                        Text("Edit", color = colors.primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PayoutOptionRow(
                        title = "UPI Transfer",
                        subtitle = pm.upiId.ifBlank { "Not added — tap Edit to save UPI ID" },
                        icon = Icons.Rounded.QrCode2,
                        selected = selectedMethod == "UPI",
                        configured = pm.upiId.isNotBlank(),
                        onClick = { selectedMethod = "UPI" }
                    )
                    val bankMask = if (pm.bankAccountNumber.length >= 4) "••••${pm.bankAccountNumber.takeLast(4)} (${pm.bankName.ifBlank { "Bank" }})" else "Not added — tap Edit to save Bank"
                    PayoutOptionRow(
                        title = "Bank Transfer",
                        subtitle = bankMask,
                        icon = Icons.Rounded.AccountBalance,
                        selected = selectedMethod == "BANK",
                        configured = pm.bankAccountNumber.isNotBlank() && pm.bankIfsc.isNotBlank(),
                        onClick = { selectedMethod = "BANK" }
                    )
                }
            }
        }

        // Amount Selection Card
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Withdrawal Amount", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text("Min ${appConfig.minWithdrawalCoins} • Max ${appConfig.maxWithdrawalCoins} coins", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Spacer(Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { preset ->
                        FilterChip(
                            selected = enteredCoins == preset,
                            onClick = { withdrawInput = preset.toString() },
                            label = { Text("$preset", fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                PRTextField(
                    value = withdrawInput,
                    onValueChange = { withdrawInput = it.filter { c -> c.isDigit() } },
                    label = "Enter Coins",
                    placeholder = "e.g. ${appConfig.minWithdrawalCoins}",
                    leadingIcon = Icons.Rounded.MonetizationOn,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Live Payout Summary Card
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Payout Summary", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                SummaryRow("Coins to Deduct", "$enteredCoins Coins")
                Spacer(Modifier.height(6.dp))
                SummaryRow("Payout Value (INR)", "₹${String.format("%.2f", estimatedInr)}", highlight = colors.mintAccent)
                Spacer(Modifier.height(6.dp))
                SummaryRow("Processing Time", "24 – 48 Hours")
                Spacer(Modifier.height(14.dp))
                PrimaryGradientButton(
                    text = "Withdraw ₹${String.format("%.2f", estimatedInr)}",
                    isLoading = submitting,
                    enabled = enteredCoins in appConfig.minWithdrawalCoins..appConfig.maxWithdrawalCoins,
                    leadingIcon = Icons.Rounded.Send,
                    onClick = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = colors.card,
            title = { Text("Confirm Withdrawal", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text("Withdraw $enteredCoins coins (₹${String.format("%.2f", estimatedInr)}) via $selectedMethod?", color = colors.textSecondary)
            },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Confirm",
                    height = 42.dp,
                    onClick = {
                        showConfirm = false
                        val p = profile ?: return@PrimaryGradientButton
                        scope.launch {
                            submitting = true
                            val res = viewModel.repo.requestWithdrawal(p.uid, p.fullName, enteredCoins, selectedMethod, p.payoutMethods, appConfig)
                            submitting = false
                            if (res.isSuccess) {
                                withdrawInput = ""
                                viewModel.showToast("Withdrawal submitted for admin approval!", ToastType.SUCCESS)
                                onBack()
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Withdrawal failed.", ToastType.ERROR)
                            }
                        }
                    }
                )
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun PayoutOptionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    configured: Boolean,
    onClick: () -> Unit
) {
    val colors = PRTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().bounceClick(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.lavenderSoft else colors.inputBg,
        border = BorderStroke(if (selected) 1.8.dp else 1.dp, if (selected) colors.primaryIndigo else colors.cardBorder)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, if (selected) colors.primaryIndigo.copy(alpha = 0.15f) else colors.card, if (selected) colors.primaryIndigo else colors.textSecondary, size = 36.dp, cornerRadius = 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (configured) colors.textSecondary else colors.amberAccent, maxLines = 1)
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, highlight: Color? = null) {
    val colors = PRTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = highlight ?: colors.textPrimary)
    }
}

// ============================================================================
// SCREEN 3: DEDICATED CONVERT COINS TO INR SCREEN
// ============================================================================

@Composable
fun ConvertCoinsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    val coinsPerInr = appConfig.coinsPerInr.coerceAtLeast(1L)
    var input by remember { mutableStateOf("") }
    var converting by remember { mutableStateOf(false) }

    val coinsVal = input.toLongOrNull() ?: 0L
    val inrPreview = coinsVal.toDouble() / coinsPerInr.toDouble()

    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Text("Convert Coins to INR", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }

        // Balance Bar
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PRCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
                Text("Coins", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Text("${profile?.coins ?: 0}", style = MaterialTheme.typography.titleLarge, color = colors.primaryIndigo)
            }
            PRCard(modifier = Modifier.weight(1f), cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
                Text("INR Balance", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Text("₹${String.format("%.2f", profile?.inrBalance ?: 0.0)}", style = MaterialTheme.typography.titleLarge, color = colors.mintAccent)
            }
        }

        PRCard(cornerRadius = 16.dp) {
            Text("Conversion Rate: $coinsPerInr Coins = ₹1 INR", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text("Minimum ${appConfig.minConvertCoins} coins • No maximum limit", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10000L, 25000L, 50000L).forEach { amt ->
                    FilterChip(
                        selected = coinsVal == amt,
                        onClick = { input = amt.toString() },
                        label = { Text("$amt") }
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { input = (profile?.coins ?: 0L).toString() },
                    label = { Text("MAX") }
                )
            }
            Spacer(Modifier.height(12.dp))
            PRTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = "Coins to Convert",
                placeholder = "Min ${appConfig.minConvertCoins}",
                leadingIcon = Icons.Rounded.CurrencyExchange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (coinsVal > 0) {
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = colors.mintSoft, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "You will receive ₹${String.format("%.2f", inrPreview)} INR instantly",
                        color = colors.mintAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryGradientButton(
                text = "Convert Now",
                isLoading = converting,
                enabled = coinsVal >= appConfig.minConvertCoins,
                onClick = {
                    val uid = profile?.uid ?: return@PrimaryGradientButton
                    scope.launch {
                        converting = true
                        val res = viewModel.repo.convertCoinsToInr(uid, coinsVal, appConfig)
                        converting = false
                        if (res.isSuccess) {
                            input = ""
                            viewModel.showToast("Converted to ₹${String.format("%.2f", res.getOrThrow())} INR!", ToastType.SUCCESS)
                        } else {
                            viewModel.showToast(res.exceptionOrNull()?.message ?: "Conversion failed.", ToastType.ERROR)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// SCREEN 4: DEDICATED TRANSACTIONS HISTORY SCREEN
// ============================================================================

@Composable
fun WalletHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val transactions by viewModel.transactions.collectAsState()
    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(transactions, filter) {
        when (filter) {
            "EARNED" -> transactions.filter { it.amountCoins > 0 }
            "WITHDRAW" -> transactions.filter { it.type == "WITHDRAWAL" }
            "CONVERT" -> transactions.filter { it.type == "CONVERT" }
            else -> transactions
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Text("Transactions History", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("ALL" to "All", "EARNED" to "Earned", "WITHDRAW" to "Withdrawals", "CONVERT" to "Conversions")) { (key, label) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.ReceiptLong,
                title = "No Transactions Found",
                subtitle = "Your coin rewards, conversions, and withdrawals will be listed here."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { tx ->
                    TransactionRowCard(tx)
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(tx: TransactionItem) {
    val colors = PRTheme.colors
    val isPositive = tx.amountCoins >= 0
    val dateStr = remember(tx.createdAt) {
        if (tx.createdAt > 0) SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.createdAt)) else ""
    }
    PRCard(cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconBadge(
                icon = if (isPositive) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                containerColor = if (isPositive) colors.mintSoft else colors.roseSoft,
                iconTint = if (isPositive) colors.mintAccent else colors.roseAccent,
                size = 38.dp,
                cornerRadius = 11.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Text("$dateStr • ${tx.status}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Text(
                text = "${if (isPositive) "+" else ""}${tx.amountCoins}",
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) colors.mintAccent else colors.roseAccent,
                fontSize = 14.sp
            )
        }
    }
}

// ============================================================================
// SCREEN 5: PAYOUT METHODS SETTINGS (UPI & BANK TRANSFER)
// ============================================================================

@Composable
fun PayoutSettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val saved = profile?.payoutMethods ?: PayoutMethods()

    var upiId by remember(saved.upiId) { mutableStateOf(saved.upiId) }
    var holder by remember(saved.bankAccountHolder) { mutableStateOf(saved.bankAccountHolder) }
    var bankName by remember(saved.bankName) { mutableStateOf(saved.bankName) }
    var accountNumber by remember(saved.bankAccountNumber) { mutableStateOf(saved.bankAccountNumber) }
    var ifsc by remember(saved.bankIfsc) { mutableStateOf(saved.bankIfsc) }
    var phone by remember(saved.bankPhone) { mutableStateOf(saved.bankPhone) }
    var saving by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text("Payout Methods", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }

        item {
            PRCard(cornerRadius = 16.dp) {
                Text("UPI Details", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                PRTextField(
                    value = upiId,
                    onValueChange = { upiId = it.trim() },
                    label = "Valid UPI ID",
                    placeholder = "example@okaxis",
                    leadingIcon = Icons.Rounded.QrCode2
                )
            }
        }

        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Bank Transfer Details", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                PRTextField(value = holder, onValueChange = { holder = it }, label = "Account Holder Name", leadingIcon = Icons.Rounded.Person)
                Spacer(Modifier.height(10.dp))
                PRTextField(value = bankName, onValueChange = { bankName = it }, label = "Bank Name", leadingIcon = Icons.Rounded.AccountBalance)
                Spacer(Modifier.height(10.dp))
                PRTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                    label = "Account Number (Required)",
                    leadingIcon = Icons.Rounded.Numbers,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(10.dp))
                PRTextField(
                    value = ifsc,
                    onValueChange = { ifsc = it.uppercase().take(11) },
                    label = "IFSC Code (Required)",
                    placeholder = "e.g. SBIN0001234",
                    leadingIcon = Icons.Rounded.AssuredWorkload
                )
                Spacer(Modifier.height(10.dp))
                PRTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() }.take(15) },
                    label = "Phone Number (Optional)",
                    leadingIcon = Icons.Rounded.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(Modifier.height(16.dp))
                PrimaryGradientButton(
                    text = "Save Changes",
                    isLoading = saving,
                    onClick = {
                        val uid = profile?.uid ?: return@PrimaryGradientButton
                        if (upiId.isNotBlank() && !upiId.contains("@")) {
                            viewModel.showToast("Please enter a valid UPI ID containing '@'.", ToastType.ERROR)
                            return@PrimaryGradientButton
                        }
                        if (accountNumber.isNotBlank() && ifsc.length < 11) {
                            viewModel.showToast("IFSC code must be 11 characters.", ToastType.ERROR)
                            return@PrimaryGradientButton
                        }
                        scope.launch {
                            saving = true
                            val res = viewModel.repo.savePayoutMethods(
                                uid,
                                PayoutMethods(upiId, holder.trim(), bankName.trim(), accountNumber, ifsc, phone)
                            )
                            saving = false
                            if (res.isSuccess) {
                                viewModel.showToast("Payout methods saved!", ToastType.SUCCESS)
                                onBack()
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Save failed.", ToastType.ERROR)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
