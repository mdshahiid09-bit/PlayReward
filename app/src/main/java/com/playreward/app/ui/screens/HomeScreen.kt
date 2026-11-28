package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.playreward.app.data.model.BannerItem
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
    val leaderboard by viewModel.leaderboard.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    val unreadCount = remember(notifications) { notifications.count { !it.read } }
    val activeTaskIds = remember(userTasks) { userTasks.map { it.taskId }.toSet() }
    val featuredTasks = remember(tasks, activeTaskIds) {
        val available = tasks.filter { it.id !in activeTaskIds }
        val flagged = available.filter { it.featured }
        (if (flagged.isNotEmpty()) flagged else available).take(10)
    }

    val coinsPerInr = appConfig.coinsPerInr.coerceAtLeast(1L)
    val approxInr = (profile?.coins ?: 0L).toDouble() / coinsPerInr.toDouble()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Top App Bar (Clean Spacing: 14dp gap between Notification & Profile)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(38.dp).bounceClick { viewModel.soundFx.playClick(); onOpenDrawer() },
                    shape = RoundedCornerShape(12.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Image(
                    painter = painterResource(R.drawable.playreward_logo),
                    contentDescription = "PlayReward",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("PlayReward", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    val firstName = profile?.fullName?.substringBefore(" ")?.ifBlank { "Player" } ?: "Player"
                    Text("Welcome back, $firstName", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
                }

                // Notification Bell with 14dp separation from Profile
                Box(modifier = Modifier.padding(end = 14.dp)) {
                    Surface(
                        modifier = Modifier.size(38.dp).bounceClick { viewModel.soundFx.playClick(); onNavigate("notifications") },
                        shape = CircleShape,
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (unreadCount > 0) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                                contentDescription = "Notifications",
                                tint = if (unreadCount > 0) colors.primaryIndigo else colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(colors.roseAccent)
                        )
                    }
                }

                // Glowing Gradient-Ring Profile Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.heroGradient)
                        .padding(2.dp)
                        .bounceClick { viewModel.soundFx.playClick(); onNavigate("profile") }
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = CircleShape,
                        color = colors.card
                    ) {
                        if (!profile?.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile?.avatarUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.background(colors.lavenderSoft)) {
                                Text(
                                    text = (profile?.fullName?.firstOrNull() ?: 'P').uppercase(),
                                    color = colors.primaryIndigo,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Luminous Auto-Scrolling Slider Banners with Decorative Orbs & Shimmer Sweep
        item {
            val effectiveBanners = remember(banners) {
                if (banners.isNotEmpty()) banners else listOf(
                    BannerItem("b1", "Play Quizzes & Collect Gems", "Answer daily quizzes and unlock guaranteed coin rewards", "", "ROUTE", "play", true, 0),
                    BannerItem("b2", "Real 1v1 Tic-Tac-Toe Arena", "Challenge real players live & multiply your coin rewards", "", "ROUTE", "super_offer", true, 1),
                    BannerItem("b3", "Refer Friends & Earn Together", "Share your invite code and earn bonus coins on withdrawal", "", "ROUTE", "refer", true, 2)
                )
            }
            val pagerState = rememberPagerState(pageCount = { effectiveBanners.size })
            LaunchedEffect(effectiveBanners.size) {
                while (effectiveBanners.size > 1) {
                    delay(4000)
                    pagerState.animateScrollToPage((pagerState.currentPage + 1) % effectiveBanners.size)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp
                ) { idx ->
                    val b = effectiveBanners[idx]
                    val bgBrush = when (idx % 3) {
                        0 -> colors.heroGradient
                        1 -> colors.megaGradient
                        else -> colors.superGradient
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .bounceClick {
                                viewModel.soundFx.playClick()
                                when (b.actionType.uppercase()) {
                                    "URL" -> if (b.actionValue.isNotBlank()) {
                                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.actionValue))) } catch (_: Exception) {}
                                    }
                                    "ROUTE" -> if (b.actionValue.isNotBlank()) onNavigate(b.actionValue)
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
                            if (b.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = b.imageUrl,
                                    contentDescription = b.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                DecorativeOrbsOverlay()
                                ShimmerSweepOverlay()
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                                            Text(
                                                text = "FEATURED",
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(b.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(3.dp))
                                        Text(b.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Surface(shape = CircleShape, color = Color.White) {
                                        Icon(Icons.Rounded.ArrowForward, null, tint = colors.primaryIndigo, modifier = Modifier.padding(9.dp).size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                if (effectiveBanners.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(effectiveBanners.size) { i ->
                            val active = pagerState.currentPage == i
                            Box(
                                modifier = Modifier
                                    .height(5.dp)
                                    .width(if (active) 20.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) colors.primaryIndigo else colors.cardBorder)
                            )
                        }
                    }
                }
            }
        }

        // 3. Flagship Balance Summary Card (Coins + INR Pill + Glowing Wallet CTA)
        item {
            PRCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(15.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(colors.amberSoft, colors.amberAccent.copy(alpha = 0.25f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(painterResource(R.drawable.ic_coin), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AVAILABLE BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary, letterSpacing = 0.6.sp)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${profile?.coins ?: 0}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "≈ ₹${String.format("%.2f", approxInr)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.mintAccent,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.heroGradient)
                            .bounceClick { viewModel.soundFx.playClick(); onNavigate("wallet") }
                            .padding(horizontal = 13.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Wallet", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 4. Sleek 2-Column Quick Action Cards (Refer & Earn + Play & Earn)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                PRCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = { viewModel.soundFx.playClick(); onNavigate("refer") }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(colors.lavenderSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_pr_refer), contentDescription = "Refer", modifier = Modifier.size(26.dp))
                        }
                        Icon(Icons.Rounded.ArrowOutward, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Refer & Earn", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (referralConfig.enabled) "+${referralConfig.referrerRewardCoins} coins / invite" else "Program paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryIndigo,
                        fontWeight = FontWeight.Bold
                    )
                }

                PRCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 18.dp,
                    contentPadding = PaddingValues(14.dp),
                    onClick = { viewModel.soundFx.playClick(); onNavigate("play") }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(colors.amberSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_pr_quiz), contentDescription = "Play & Earn", modifier = Modifier.size(26.dp))
                        }
                        Icon(Icons.Rounded.ArrowOutward, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Play & Earn", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Quizzes & Gems",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.amberAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Featured Tasks Section Header + List (Top 10)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Featured Tasks", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFF97316), modifier = Modifier.size(19.dp))
                }
                Surface(
                    modifier = Modifier.bounceClick { viewModel.soundFx.playClick(); onNavigate("tasks") },
                    shape = CircleShape,
                    color = colors.lavenderSoft
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("View all", color = colors.primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Icon(Icons.Rounded.ChevronRight, null, tint = colors.primaryIndigo, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        if (featuredTasks.isEmpty()) {
            item {
                PRCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), cornerRadius = 16.dp, contentPadding = PaddingValues(13.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.lavenderSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_pr_tasks), contentDescription = "Tasks", modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Featured Tasks Yet", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("New tasks published from the admin panel appear here live.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        } else {
            items(featuredTasks, key = { it.id }) { task ->
                FeaturedTaskRowCard(
                    task = task,
                    onClick = { viewModel.soundFx.playClick(); onOpenTask(task.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 6. Luminous Side-by-Side Gradient Cards: Mega Offer & Super Offer
        if (megaConfig.enabled || superConfig.enabled) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    if (megaConfig.enabled) {
                        Surface(
                            modifier = Modifier.weight(1f).bounceClick { viewModel.soundFx.playClick(); onNavigate("mega_offer") },
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 5.dp
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().background(colors.megaGradient)) {
                                DecorativeOrbsOverlay()
                                ShimmerSweepOverlay()
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Surface(shape = CircleShape, color = Color(0xFFFEF3C7)) {
                                        Text("MOST PAID", color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.height(9.dp))
                                    Text(megaConfig.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(3.dp))
                                    Text("Guaranteed", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                    Text("${megaConfig.rewardCoins}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                                    Text("Coins", color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp)
                                    Spacer(Modifier.height(10.dp))
                                    Surface(shape = CircleShape, color = Color.White) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Play Now", color = Color(0xFFEA580C), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFEA580C), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (superConfig.enabled) {
                        val topWin = superConfig.tiers.maxOfOrNull { it.winCoins } ?: 950L
                        Surface(
                            modifier = Modifier.weight(1f).bounceClick { viewModel.soundFx.playClick(); onNavigate("super_offer") },
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 5.dp
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().background(colors.superGradient)) {
                                DecorativeOrbsOverlay()
                                ShimmerSweepOverlay()
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.24f)) {
                                        Text("1v1 REAL PVP", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    Spacer(Modifier.height(9.dp))
                                    Text("Super Offer", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(3.dp))
                                    Text("Win Up To", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                    Text("$topWin", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp)
                                    Text("Coins", color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp)
                                    Spacer(Modifier.height(10.dp))
                                    Surface(shape = CircleShape, color = Color.White) {
                                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Enter Arena", color = Color(0xFF4F46E5), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                            Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Animated Top Earners Leaderboard Banner Card
        item {
            val pulse = rememberInfiniteTransition(label = "trophyPulse")
            val trophyScale by pulse.animateFloat(0.93f, 1.07f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "ts")
            val topThree = remember(leaderboard) { leaderboard.take(3) }
            val topScore = topThree.firstOrNull()?.totalCoinsEarned ?: 0L

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .bounceClick { viewModel.soundFx.playClick(); onNavigate("leaderboard") },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, colors.amberAccent.copy(alpha = 0.6f)),
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxWidth().background(colors.warmBannerGradient).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(46.dp).scale(trophyScale).clip(RoundedCornerShape(13.dp)).background(colors.amberSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_pr_trophy), contentDescription = "Leaderboard", modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Top Earners Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.AutoAwesome, null, tint = colors.amberAccent, modifier = Modifier.size(15.dp))
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (topScore > 0) "Top score: $topScore coins • Tap to view podium" else "Compete daily & climb to #1 podium",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            if (topThree.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    topThree.forEachIndexed { idx, entry ->
                                        val badgeColors = listOf(Color(0xFFF59E0B), Color(0xFF6366F1), Color(0xFFEC4899))
                                        Box(
                                            modifier = Modifier.offset(x = (-4 * idx).dp).size(22.dp).clip(CircleShape).background(badgeColors[idx % badgeColors.size]),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text((entry.fullName.firstOrNull() ?: 'P').uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    Spacer(Modifier.width(4.dp))
                                    Text("Top earners live", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB45309))
                                }
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFB45309), modifier = Modifier.size(22.dp))
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
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(12.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (task.iconUrl.isNotBlank()) {
                AsyncImage(
                    model = task.iconUrl,
                    contentDescription = task.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.lavenderSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painterResource(R.drawable.ic_pr_tasks), null, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title.ifBlank { task.appName }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(task.appName, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = colors.lavenderSoft) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    val rewardLabel = if (task.currency == "INR") "₹${task.prizeAmount.toInt()}" else "${task.prizeAmount.toInt()}"
                    Text(rewardLabel, color = colors.primaryIndigo, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                }
            }
        }
    }
}
