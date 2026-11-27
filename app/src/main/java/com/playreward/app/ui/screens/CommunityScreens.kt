package com.playreward.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playreward.app.R
import com.playreward.app.data.model.ReferralRecord
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun ReferAndEarnScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenReferralList: (String) -> Unit
) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.referralConfig.collectAsState()
    val referrals by viewModel.referrals.collectAsState()

    val myCode = profile?.referralCode.orEmpty()
    val referralLink = if (config.referralLinkEnabled && config.referralBaseUrl.isNotBlank()) {
        "${config.referralBaseUrl}&referrer=$myCode"
    } else ""

    val completedCount = referrals.count { it.status == "COMPLETED" }
    val pendingCount = referrals.count { it.status != "COMPLETED" }
    val totalEarnedInr = referrals.filter { it.status == "COMPLETED" }.sumOf { it.rewardInr }

    fun copyText(label: String, value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        viewModel.showToast("$label copied!", ToastType.SUCCESS)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text("Refer & Earn", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }

        if (!config.enabled) {
            item {
                EmptyStateView(
                    icon = Icons.Rounded.CardGiftcard,
                    title = "Referral Program Paused",
                    subtitle = "The referral program is currently paused by the administrator."
                )
            }
        } else {
            // Hero Card (Reference Video 00:44)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = colors.lavenderSoft,
                    border = BorderStroke(1.dp, colors.primaryIndigo.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.heroGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CardGiftcard, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Invite Friends & Earn", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Earn ${config.referrerRewardCoins} coins when your friend completes their first approved withdrawal. Your friend also gets ${config.signupBonusCoins} signup bonus coins!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Code & Share Card
            item {
                PRCard(cornerRadius = 20.dp) {
                    Text("Your Referral Code", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.inputBg,
                            border = BorderStroke(1.dp, colors.cardBorder)
                        ) {
                            Text(
                                text = myCode.ifBlank { "Generating..." },
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                        Surface(
                            modifier = Modifier.size(50.dp).bounceClick { if (myCode.isNotBlank()) copyText("Referral code", myCode) },
                            shape = RoundedCornerShape(14.dp),
                            color = colors.lavenderSoft
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ContentCopy, "Copy", tint = colors.primaryIndigo)
                            }
                        }
                    }

                    if (referralLink.isNotBlank()) {
                        Spacer(Modifier.height(14.dp))
                        SecondaryOutlineButton(
                            text = "Copy Referral Link",
                            leadingIcon = Icons.Rounded.Link,
                            onClick = { copyText("Referral link", referralLink) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    PrimaryGradientButton(
                        text = "Share with Friends",
                        leadingIcon = Icons.Rounded.Share,
                        onClick = {
                            val msg = config.shareGreetingMessage.replace("{CODE}", myCode) +
                                if (referralLink.isNotBlank()) "\n$referralLink" else ""
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Referral Code"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2x2 Referral Statistics Card (Total clickable -> ALL, Pending clickable -> PENDING)
            item {
                PRCard(cornerRadius = 20.dp) {
                    Text("Your Referral Stats", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatMiniCard(
                            icon = Icons.Rounded.Groups,
                            value = "${referrals.size}",
                            label = "Total Referrals",
                            tint = colors.primaryIndigo,
                            bg = colors.lavenderSoft,
                            onClick = { onOpenReferralList("ALL") },
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            icon = Icons.Rounded.CheckCircle,
                            value = "$completedCount",
                            label = "Completed",
                            tint = colors.mintAccent,
                            bg = colors.mintSoft,
                            onClick = { onOpenReferralList("COMPLETED") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatMiniCard(
                            icon = Icons.Rounded.Schedule,
                            value = "$pendingCount",
                            label = "Pending",
                            tint = colors.amberAccent,
                            bg = colors.amberSoft,
                            onClick = { onOpenReferralList("PENDING") },
                            modifier = Modifier.weight(1f)
                        )
                        StatMiniCard(
                            icon = Icons.Rounded.CurrencyRupee,
                            value = "₹${String.format("%.2f", totalEarnedInr)}",
                            label = "Total Earned",
                            tint = colors.primaryViolet,
                            bg = colors.lavenderSoft,
                            onClick = null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
    bg: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    Surface(
        modifier = if (onClick != null) modifier.bounceClick(onClick = onClick) else modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.inputBg,
        border = BorderStroke(1.dp, colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, bg, tint, size = 38.dp, cornerRadius = 12.dp)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ReferralListScreen(
    filter: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val allReferrals by viewModel.referrals.collectAsState()
    val filtered = remember(allReferrals, filter) {
        when (filter) {
            "PENDING" -> allReferrals.filter { it.status != "COMPLETED" }
            "COMPLETED" -> allReferrals.filter { it.status == "COMPLETED" }
            else -> allReferrals
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Text(
                text = when (filter) {
                    "PENDING" -> "Pending Referrals"
                    "COMPLETED" -> "Completed Referrals"
                    else -> "All Referrals"
                },
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
        }
        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.GroupAdd,
                title = "No Referrals Here",
                subtitle = "Invite friends with your code to see their live referral status and rewards here."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.referredUid }) { ref ->
                    ReferralRowCard(ref)
                }
            }
        }
    }
}

@Composable
private fun ReferralRowCard(ref: ReferralRecord) {
    val colors = PRTheme.colors
    val completed = ref.status == "COMPLETED"
    PRCard(contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconBadge(
                icon = if (completed) Icons.Rounded.Verified else Icons.Rounded.HourglassTop,
                containerColor = if (completed) colors.mintSoft else colors.amberSoft,
                iconTint = if (completed) colors.mintAccent else colors.amberAccent,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ref.referredName, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(
                    text = if (completed) "Reward credited" else "Waiting for 1st approved withdrawal",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Surface(shape = CircleShape, color = if (completed) colors.mintSoft else colors.lavenderSoft) {
                Text(
                    text = "₹${String.format("%.2f", ref.rewardInr)}",
                    color = if (completed) colors.mintAccent else colors.primaryIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun LeaderboardScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val entries by viewModel.leaderboard.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val top3 = remember(entries) { entries.take(3) }
    val rest = remember(entries) { entries.drop(3) }
    val myRank = remember(entries, profile?.uid) {
        val idx = entries.indexOfFirst { it.uid == profile?.uid }
        if (idx >= 0) idx + 1 else null
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp)) }
            Column(modifier = Modifier.weight(1f)) {
                Text("Top Earners", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text("Live rankings updated in real time", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
            if (myRank != null) {
                Surface(shape = CircleShape, color = colors.lavenderSoft) {
                    Text(
                        text = "Your Rank #$myRank",
                        color = colors.primaryIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.EmojiEvents,
                title = "Leaderboard Updating",
                subtitle = "Start earning coins through tasks and games to claim the #1 podium spot!"
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top 3 Visual Podium (#2 Left, #1 Center Elevated, #3 Right)
                item {
                    PRCard(cornerRadius = 20.dp, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            PodiumColumn(
                                entry = top3.getOrNull(1),
                                rank = 2,
                                pedestalHeight = 62.dp,
                                accent = Color(0xFF94A3B8),
                                bg = Color(0xFFF1F5F9),
                                modifier = Modifier.weight(1f)
                            )
                            PodiumColumn(
                                entry = top3.getOrNull(0),
                                rank = 1,
                                pedestalHeight = 84.dp,
                                accent = Color(0xFFF59E0B),
                                bg = Color(0xFFFFFBEB),
                                modifier = Modifier.weight(1.12f)
                            )
                            PodiumColumn(
                                entry = top3.getOrNull(2),
                                rank = 3,
                                pedestalHeight = 50.dp,
                                accent = Color(0xFFEA580C),
                                bg = Color(0xFFFFF7ED),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (rest.isNotEmpty()) {
                    item {
                        Text("All Rankings", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
                    }
                    itemsIndexed(rest, key = { _, e -> e.uid }) { idx, item ->
                        val rank = idx + 4
                        PRCard(cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(colors.inputBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("#$rank", fontWeight = FontWeight.Bold, color = colors.textSecondary, fontSize = 12.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.lavenderSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text((item.fullName.firstOrNull() ?: 'P').uppercase(), fontWeight = FontWeight.Bold, color = colors.primaryIndigo, fontSize = 13.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(item.fullName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, modifier = Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${item.totalCoinsEarned}", fontWeight = FontWeight.ExtraBold, color = colors.primaryIndigo, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: com.playreward.app.data.model.LeaderboardEntry?,
    rank: Int,
    pedestalHeight: androidx.compose.ui.unit.Dp,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (rank == 1) {
            Icon(Icons.Rounded.EmojiEvents, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
        }
        Box(
            modifier = Modifier
                .size(if (rank == 1) 48.dp else 40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (entry?.fullName?.firstOrNull() ?: '-').uppercase(),
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                fontSize = if (rank == 1) 18.sp else 15.sp
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = entry?.fullName?.substringBefore(" ") ?: "Open",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (entry != null) "${entry.totalCoinsEarned}" else "—",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.primaryIndigo
        )
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(pedestalHeight),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            color = if (colors.isDark) colors.inputBg else bg,
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.6f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("#$rank", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = accent)
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val items by viewModel.notifications.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Text("Notifications", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        if (items.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.NotificationsNone,
                title = "All Caught Up",
                subtitle = "Task approvals, rejections, reminders, and reward alerts appear here."
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { notif ->
                    PRCard(
                        onClick = {
                            val uid = profile?.uid ?: return@PRCard
                            if (!notif.read) scope.launch { viewModel.repo.markNotificationRead(uid, notif.id) }
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconBadge(
                                icon = if (notif.read) Icons.Rounded.NotificationsNone else Icons.Rounded.NotificationsActive,
                                containerColor = if (notif.read) colors.inputBg else colors.lavenderSoft,
                                iconTint = if (notif.read) colors.textSecondary else colors.primaryIndigo,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(notif.body, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
