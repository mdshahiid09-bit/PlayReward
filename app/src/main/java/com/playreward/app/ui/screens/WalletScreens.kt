package com.playreward.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun WalletScreen(
    viewModel: MainViewModel,
    onOpenPayoutSettings: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var convertInput by remember { mutableStateOf("") }
    var withdrawCoinsInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var converting by remember { mutableStateOf(false) }
    var withdrawing by remember { mutableStateOf(false) }

    val coinsPerInr = appConfig.coinsPerInr.coerceAtLeast(1L)
    val convertCoins = convertInput.toLongOrNull() ?: 0L
    val convertedInrPreview = convertCoins.toDouble() / coinsPerInr.toDouble()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Payout Settings Gear
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Your Balance", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text("Manage earnings, convert coins & withdraw", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                Surface(
                    modifier = Modifier.size(44.dp).bounceClick(onClick = onOpenPayoutSettings),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Settings, "Payout Settings", tint = colors.primaryIndigo)
                    }
                }
            }
        }

        // Hero Indigo-Violet Balance Card (Reference Video 00:52)
        item {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(colors.heroGradient).padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Available Coins", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            Text("${profile?.coins ?: 0}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                            Text(
                                text = "₹${String.format("%.2f", profile?.inrBalance ?: 0.0)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Rate: $coinsPerInr Coins = ₹1 INR  •  Min Withdraw: ${appConfig.minWithdrawalCoins} Coins",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Convert Coins to INR Card (Min 10,000, Max Infinity)
        item {
            PRCard(cornerRadius = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.CurrencyExchange, colors.lavenderSoft, colors.primaryIndigo, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Convert Coins to INR", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Minimum ${appConfig.minConvertCoins} coins • No maximum limit", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                PRTextField(
                    value = convertInput,
                    onValueChange = { convertInput = it.filter { c -> c.isDigit() } },
                    label = "Coins to Convert",
                    placeholder = "e.g. 10000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (convertCoins > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "You will receive ₹${String.format("%.2f", convertedInrPreview)} INR",
                        color = colors.mintAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
                PrimaryGradientButton(
                    text = "Convert to INR",
                    isLoading = converting,
                    enabled = convertCoins >= appConfig.minConvertCoins,
                    onClick = {
                        val uid = profile?.uid ?: return@PrimaryGradientButton
                        scope.launch {
                            converting = true
                            val res = viewModel.repo.convertCoinsToInr(uid, convertCoins, appConfig)
                            converting = false
                            if (res.isSuccess) {
                                convertInput = ""
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

        // Withdraw Request Card
        item {
            PRCard(cornerRadius = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Withdraw Rewards", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    TextButton(onClick = onOpenPayoutSettings) {
                        Text("Edit Payout Info", color = colors.primaryIndigo, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("UPI" to Icons.Rounded.QrCode2, "BANK" to Icons.Rounded.AccountBalance).forEach { (m, ic) ->
                        val selected = selectedMethod == m
                        Surface(
                            modifier = Modifier.weight(1f).bounceClick { selectedMethod = m },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) colors.lavenderSoft else colors.inputBg,
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primaryIndigo else colors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(ic, null, tint = if (selected) colors.primaryIndigo else colors.textSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (m == "UPI") "UPI Transfer" else "Bank Transfer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PRTextField(
                    value = withdrawCoinsInput,
                    onValueChange = { withdrawCoinsInput = it.filter { c -> c.isDigit() } },
                    label = "Coins to Withdraw (${appConfig.minWithdrawalCoins} - ${appConfig.maxWithdrawalCoins})",
                    placeholder = "Enter coins",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                PrimaryGradientButton(
                    text = "Submit Withdrawal Request",
                    isLoading = withdrawing,
                    enabled = (withdrawCoinsInput.toLongOrNull() ?: 0L) >= appConfig.minWithdrawalCoins,
                    onClick = {
                        val p = profile ?: return@PrimaryGradientButton
                        val coins = withdrawCoinsInput.toLongOrNull() ?: 0L
                        scope.launch {
                            withdrawing = true
                            val res = viewModel.repo.requestWithdrawal(p.uid, p.fullName, coins, selectedMethod, p.payoutMethods, appConfig)
                            withdrawing = false
                            if (res.isSuccess) {
                                withdrawCoinsInput = ""
                                viewModel.showToast("Withdrawal request submitted for admin approval!", ToastType.SUCCESS)
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Withdrawal failed.", ToastType.ERROR)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Transactions History
        item {
            Text("Transactions History", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        if (transactions.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "No Transactions Yet",
                    subtitle = "Your task rewards, game wins, coin conversions, and withdrawals will appear here."
                )
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                TransactionRowCard(tx)
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
    PRCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconBadge(
                icon = if (isPositive) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                containerColor = if (isPositive) colors.mintSoft else colors.roseSoft,
                iconTint = if (isPositive) colors.mintAccent else colors.roseAccent,
                size = 42.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text("$dateStr • ${tx.status}", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            Text(
                text = "${if (isPositive) "+" else ""}${tx.amountCoins} coins",
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) colors.mintAccent else colors.roseAccent,
                fontSize = 14.sp
            )
        }
    }
}

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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                Spacer(Modifier.height(18.dp))
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
