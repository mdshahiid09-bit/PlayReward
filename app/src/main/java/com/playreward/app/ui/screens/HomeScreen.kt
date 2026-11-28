package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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

    val targetCoins = (profile?.coins ?: 0L).toInt()
    val animatedCoins by animateIntAsState(targetValue = targetCoins, animationSpec = tween(750), label = "coinCount")
    val coinsPerInr = appConfig.coinsPerInr.coerceAtLeast(1L)
    val approxInr = (profile?.coins ?: 0L).toDouble() / coinsPerInr.toDouble()

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        // Breathing Ambient Aura Glow behind the top half of the screen
        AmbientAuraBackground(modifier = Modifier.align(Alignment.TopCenter))

        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Clean Floating Header (14dp gap between Notification & Profile)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp).bounceClick { viewModel.soundFx.playClick(); onOpenDrawer() },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shadowElevation = 3.dp
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
                        Text("Hi, $firstName 👋", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
                    }

                    Box(modifier = Modifier.padding(end = 14.dp)) {
                        Surface(
                            modifier = Modifier.size(38.dp).bounceClick { viewModel.soundFx.playClick(); onNavigate("notifications") },
                            shape = CircleShape,
                            color = colors.card.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, colors.cardBorder),
                            shadowElevation = 3.dp
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
                            PulsingLiveDot(color = colors.roseAccent, modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.heroGradient)
                            .padding(2.dp)
                            .bounceClick { viewModel.soundFx.playClick(); onNavigate("profile") }
                    ) {
                        Surface(modifier = Modifier.fillMaxSize(), shape = CircleShape, color = colors.card) {
                            if (!profile?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(model = profile?.avatarUrl, contentDescription = "Profile", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.background(colors.lavenderSoft)) {
                                    Text((profile?.fullName?.firstOrNull() ?: 'P').uppercase(), color = colors.primaryIndigo, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. NEXT-LEVEL GLOWING HERO WALLET CARD (Animated Coin Counter + Glass Action Pills)
            item {
                StaggeredAppear(index = 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 10.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFDB2777))
                                    )
                                )
                        ) {
                            DecorativeOrbsOverlay()
                            ShimmerSweepOverlay()
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
                                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("TOTAL BALANCE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                                            }
                                        }
                                    }
                                    Surface(shape = CircleShape, color = Color(0xFF10B981).copy(alpha = 0.25f), border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))) {
                                        Text(
                                            text = "₹${String.format("%.2f", profile?.inrBalance ?: 0.0)} INR",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$animatedCoins",
                                        color = Color.White,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-1).sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Coins",
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }

                                Spacer(Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        modifier = Modifier.weight(1f).bounceClick { viewModel.soundFx.playClick(); onNavigate("wallet") },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.AccountBalanceWallet, null, tint = Color(0xFF4F46E5), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Wallet & Payout", color = Color(0xFF4F46E5), fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f).bounceClick { viewModel.soundFx.playClick(); onNavigate("refer") },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.38f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Rounded.CardGiftcard, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Invite +${referralConfig.referrerRewardCoins}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quick-Access Colorful Icon Strip (Instant Navigation)
            item {
                StaggeredAppear(index = 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item { QuickActionChip(R.drawable.ic_pr_quiz, "Quizzes", colors.amberSoft) { viewModel.soundFx.playClick(); onNavigate("play") } }
                        item { QuickActionChip(R.drawable.ic_pr_refer, "Refer", colors.lavenderSoft) { viewModel.soundFx.playClick(); onNavigate("refer") } }
                        item { QuickActionChip(R.drawable.ic_pr_tasks, "Tasks", colors.mintSoft) { viewModel.soundFx.playClick(); onNavigate("tasks") } }
                        item { QuickActionChip(R.drawable.ic_pr_trophy, "Podium", colors.amberSoft) { viewModel.soundFx.playClick(); onNavigate("leaderboard") } }
                    }
                }
            }

            // 4. Auto-Scrolling Slider Banners
            item {
                StaggeredAppear(index = 2) {
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
                            delay(4200)
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
                                0 -> colors.superGradient
                                1 -> colors.megaGradient
                                else -> colors.heroGradient
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(112.dp).bounceClick {
                                    viewModel.soundFx.playClick()
                                    when (b.actionType.uppercase()) {
                                        "URL" -> if (b.actionValue.isNotBlank()) {
                                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.actionValue))) } catch (_: Exception) {}
                                        }
                                        "ROUTE" -> if (b.actionValue.isNotBlank()) onNavigate(b.actionValue)
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 5.dp
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
                                    if (b.imageUrl.isNotBlank()) {
                                        AsyncImage(model = b.imageUrl, contentDescription = b.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        DecorativeOrbsOverlay()
                                        Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(b.title, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Spacer(Modifier.height(4.dp))
                                                Text(b.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Surface(shape = CircleShape, color = Color.White) {
                                                Icon(Icons.Rounded.ArrowForward, null, tint = colors.primaryIndigo, modifier = Modifier.padding(8.dp).size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (effectiveBanners.size > 1) {
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                repeat(effectiveBanners.size) { i ->
                                    val active = pagerState.currentPage == i
                                    Box(modifier = Modifier.height(5.dp).width(if (active) 20.dp else 5.dp).clip(CircleShape).background(if (active) colors.primaryIndigo else colors.cardBorder))
                                }
                            }
                        }
                    }
                }
            }

            // 5. Mega Offer & Super Offer Side-by-Side Glowing Cards (with LIVE PVP dot!)
            if (megaConfig.enabled || superConfig.enabled) {
                item {
                    StaggeredAppear(index = 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            if (megaConfig.enabled) {
                                Surface(
                                    modifier = Modifier.weight(1f).bounceClick { viewModel.soundFx.playClick(); onNavigate("mega_offer") },
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFFFDE68A).copy(alpha = 0.55f)),
                                    shadowElevation = 7.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().background(colors.megaGradient)) {
                                        DecorativeOrbsOverlay()
                                        ShimmerSweepOverlay()
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Surface(shape = CircleShape, color = Color(0xFFFEF3C7)) {
                                                Text("MOST PAID", color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text(megaConfig.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Guaranteed", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                            Text("${megaConfig.rewardCoins}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                                            Text("Coins", color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp)
                                            Spacer(Modifier.height(10.dp))
                                            Surface(shape = CircleShape, color = Color.White) {
                                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Play Now", color = Color(0xFFEA580C), fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp)
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
                                    border = BorderStroke(1.2.dp, Color(0xFFC4B5FD).copy(alpha = 0.55f)),
                                    shadowElevation = 7.dp
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().background(colors.superGradient)) {
                                        DecorativeOrbsOverlay()
                                        ShimmerSweepOverlay()
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    PulsingLiveDot(color = Color(0xFF34D399))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("1v1 LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.5.sp)
                                                }
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Text("Super Offer", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Win Up To", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                                            Text("$topWin", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                                            Text("Coins", color = Color.White.copy(alpha = 0.9f), fontSize = 11.5.sp)
                                            Spacer(Modifier.height(10.dp))
                                            Surface(shape = CircleShape, color = Color.White) {
                                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Enter Arena", color = Color(0xFF4F46E5), fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp)
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
            }

            // 6. Animated Top Earners Leaderboard Banner Card
            item {
                StaggeredAppear(index = 4) {
                    val pulse = rememberInfiniteTransition(label = "trophyPulse")
                    val trophyScale by pulse.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(1050), RepeatMode.Reverse), label = "ts")
                    val topThree = remember(leaderboard) { leaderboard.take(3) }
                    val topScore = topThree.firstOrNull()?.totalCoinsEarned ?: 0L

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).bounceClick { viewModel.soundFx.playClick(); onNavigate("leaderboard") },
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.3.dp, colors.amberAccent.copy(alpha = 0.65f)),
                        shadowElevation = 5.dp
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
                                    Text(
                                        text = if (topScore > 0) "Top score: $topScore coins • Tap to view podium" else "Compete daily & climb to #1 podium",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = Color(0xFFB45309), modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // 7. Featured Tasks Section Header + List
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
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.lavenderSoft), contentAlignment = Alignment.Center) {
                                Image(painterResource(R.drawable.ic_pr_tasks), contentDescription = "Tasks", modifier = Modifier.size(24.dp))
                            }
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
                        onClick = { viewModel.soundFx.playClick(); onOpenTask(task.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(iconRes: Int, label: String, softBg: Color, onClick: () -> Unit) {
    val colors = PRTheme.colors
    Surface(
        modifier = Modifier.bounceClick(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(softBg), contentAlignment = Alignment.Center) {
                Image(painterResource(iconRes), contentDescription = label, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
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
    PRCard(modifier = modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (task.iconUrl.isNotBlank()) {
                AsyncImage(model = task.iconUrl, contentDescription = task.appName, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
            } else {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(colors.lavenderSoft), contentAlignment = Alignment.Center) {
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
