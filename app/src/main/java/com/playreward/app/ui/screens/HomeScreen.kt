package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playreward.app.R
import com.playreward.app.data.model.AppTask
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    onNavigate: (String) -> Unit,
    onOpenTask: (String) -> Unit
) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val banners by viewModel.banners.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val userTasks by viewModel.userTasks.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val megaConfig by viewModel.megaOfferConfig.collectAsState()
    val superConfig by viewModel.superOfferConfig.collectAsState()
    val referralConfig by viewModel.referralConfig.collectAsState()

    val unreadCount = remember(notifications) { notifications.count { !it.read } }
    val activeTaskIds = remember(userTasks) { userTasks.map { it.taskId }.toSet() }
    val featuredTasks = remember(tasks, activeTaskIds) {
        val available = tasks.filter { it.id !in activeTaskIds }
        val flagged = available.filter { it.featured }
        (if (flagged.isNotEmpty()) flagged else available).take(10)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: Left Menu Slider + App Name; Right Notifications, Leaderboard, Profile
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp).bounceClick(onClick = onOpenDrawer),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = colors.textPrimary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("PlayReward", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    val firstName = profile?.fullName?.substringBefore(" ")?.ifBlank { "Player" } ?: "Player"
                    Text("Hi, $firstName!", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }

                // Notifications Icon
                Box(modifier = Modifier.padding(end = 6.dp)) {
                    Surface(
                        modifier = Modifier.size(40.dp).bounceClick { onNavigate("notifications") },
                        shape = CircleShape,
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier.size(10.dp).align(Alignment.TopEnd).clip(CircleShape).background(colors.roseAccent)
                        )
                    }
                }

                // Leaderboard Icon
                Surface(
                    modifier = Modifier.padding(end = 6.dp).size(40.dp).bounceClick { onNavigate("leaderboard") },
                    shape = CircleShape,
                    color = colors.amberSoft,
                    border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.35f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = "Leaderboard", tint = colors.amberAccent, modifier = Modifier.size(20.dp))
                    }
                }

                // Profile Avatar Icon
                Surface(
                    modifier = Modifier.size(40.dp).bounceClick { onNavigate("profile") },
                    shape = CircleShape,
                    color = colors.lavenderSoft,
                    border = BorderStroke(1.5.dp, colors.primaryIndigo)
                ) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile?.avatarUrl,
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = colors.primaryIndigo, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Top Admin Slider Banners
        if (banners.isNotEmpty()) {
            item {
                val pagerState = rememberPagerState(pageCount = { banners.size })
                LaunchedEffect(banners.size) {
                    while (banners.size > 1) {
                        delay(4000)
                        pagerState.animateScrollToPage((pagerState.currentPage + 1) % banners.size)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 12.dp
                    ) { idx ->
                        val b = banners[idx]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colors.heroGradient)
                                .bounceClick {
                                    when (b.actionType.uppercase()) {
                                        "URL" -> if (b.actionValue.isNotBlank()) {
                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.actionValue))) } catch (_: Exception) {}
                                        }
                                        "ROUTE" -> if (b.actionValue.isNotBlank()) onNavigate(b.actionValue)
                                    }
                                }
                        ) {
                            if (b.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = b.imageUrl,
                                    contentDescription = b.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(20.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(b.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                                    if (b.subtitle.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(b.subtitle, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                    if (banners.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(banners.size) { i ->
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(if (pagerState.currentPage == i) 20.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(if (pagerState.currentPage == i) colors.primaryIndigo else colors.cardBorder)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hero Balance Summary Card (Reference Video Style)
        item {
            PRCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = 22.dp,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(colors.amberSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painterResource(R.drawable.ic_coin), contentDescription = null, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${profile?.coins ?: 0}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary
                        )
                        Text("Coins Available", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Surface(
                        modifier = Modifier.bounceClick { onNavigate("wallet") },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.canvas,
                        border = BorderStroke(1.dp, colors.cardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Go to Wallet", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        }
                    }
                }
            }
        }

        // 2-Column Quick Action Cards: Refer & Earn + Play & Earn
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PRCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp,
                    onClick = { onNavigate("refer") }
                ) {
                    IconBadge(Icons.Rounded.CardGiftcard, colors.lavenderSoft, colors.primaryViolet, size = 44.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Refer & Earn", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (referralConfig.enabled) "+${referralConfig.referrerRewardCoins} coins / friend" else "Program paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                PRCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp,
                    onClick = { onNavigate("play") }
                ) {
                    IconBadge(Icons.Rounded.Quiz, colors.amberSoft, colors.amberAccent, size = 44.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Play & Earn", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Quizzes & Gems",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.amberAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Featured App Tasks (Max 10) + View All
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Featured Tasks", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                }
                TextButton(onClick = { onNavigate("tasks") }) {
                    Text("View all", color = colors.primaryIndigo, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.primaryIndigo, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (featuredTasks.isEmpty()) {
            item {
                PRCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.TaskAlt, colors.lavenderSoft, colors.primaryIndigo, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Featured Tasks Yet", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Tasks published from the admin panel appear here live.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        } else {
            items(featuredTasks, key = { it.id }) { task ->
                FeaturedTaskRowCard(
                    task = task,
                    onClick = { onOpenTask(task.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Big Gradient Cards: Mega Offer (Find the Correct Number) + Super Offer (Tic Tac Toe)
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Mega Offer Card
                if (megaConfig.enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.megaGradient)
                            .bounceClick { onNavigate("mega_offer") }
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                                    Text(
                                        text = "MEGA OFFER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(shape = CircleShape, color = Color(0xFFFEF3C7)) {
                                    Text(
                                        text = "MOST PAID",
                                        color = Color(0xFFB45309),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(megaConfig.title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(megaConfig.subtitle, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Guaranteed Reward", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                    Text("${megaConfig.rewardCoins} Coins", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                }
                                Surface(shape = CircleShape, color = Color.White) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Play Now", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Super Offer Card (Tic Tac Toe 1v1 AI)
                if (superConfig.enabled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.superGradient)
                            .bounceClick { onNavigate("super_offer") }
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                                    Text(
                                        text = "SUPER OFFER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Icon(Icons.Rounded.Grid3x3, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(superConfig.title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "1v1 AI Tic-Tac-Toe Arena — pick your stake & difficulty and win multiplied coins!",
                                color = Color.White.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val topWin = superConfig.tiers.maxOfOrNull { it.winCoins } ?: 950L
                                Column {
                                    Text("Win Up To", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                                    Text("$topWin Coins", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                }
                                Surface(shape = CircleShape, color = Color.White) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Enter Arena", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(18.dp))
                                    }
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
fun FeaturedTaskRowCard(
    task: AppTask,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    PRCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (task.iconUrl.isNotBlank()) {
                AsyncImage(
                    model = task.iconUrl,
                    contentDescription = task.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                )
            } else {
                IconBadge(Icons.Rounded.Apps, colors.lavenderSoft, colors.primaryIndigo, size = 48.dp, cornerRadius = 14.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title.ifBlank { task.appName },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = task.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Surface(shape = CircleShape, color = colors.lavenderSoft) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    val rewardLabel = if (task.currency == "INR") "₹${task.prizeAmount.toInt()}" else "${task.prizeAmount.toInt()}"
                    Text(rewardLabel, color = colors.primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
