package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.playreward.app.util.WebExploreHelper
import kotlinx.coroutines.Job
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.playreward.app.R
import com.playreward.app.data.model.TicTacToeTier
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MegaOfferScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.megaOfferConfig.collectAsState()

    fun generateRound(): Pair<Int, List<Int>> {
        val target = Random.nextInt(1000, 9999)
        val set = mutableSetOf(target)
        while (set.size < 4) set.add(Random.nextInt(1000, 9999))
        return target to set.shuffled()
    }

    var round by remember { mutableStateOf(generateRound()) }
    var streak by remember { mutableIntStateOf(0) }
    var showClaimPopup by remember { mutableStateOf(false) }

    // Bulletproof Chrome explore state (never triggers prematurely on initial addObserver!)
    var awaitingBrowser by remember { mutableStateOf(false) }
    var leftToBrowser by remember { mutableStateOf(false) }
    var browserStartElapsedMs by remember { mutableLongStateOf(0L) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    var earlyMsg by remember { mutableStateOf<String?>(null) }
    var gemEarnedMsg by remember { mutableStateOf<String?>(null) }
    var claimingCoins by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    fun launchPartnerWebsite() {
        val req = config.requiredExploreSeconds.coerceAtLeast(1)
        val opened = WebExploreHelper.openInBrowser(context, config.websiteUrl)
        if (!opened) {
            viewModel.showToast("Could not open browser. Check website URL in Admin Panel.", ToastType.ERROR)
            return
        }
        awaitingBrowser = true
        leftToBrowser = false
        browserStartElapsedMs = SystemClock.elapsedRealtime()
        countdownJob?.cancel()
        countdownJob = WebExploreHelper.startLiveCountdownNotification(
            context = context,
            scope = scope,
            requiredSeconds = req,
            onTimeReached = { viewModel.soundFx.playSuccess() }
        )
    }

    // Single stable observer — ONLY checks elapsed time AFTER ON_PAUSE confirms user actually left to Chrome!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && awaitingBrowser) {
                leftToBrowser = true
                browserStartElapsedMs = SystemClock.elapsedRealtime()
            } else if (event == Lifecycle.Event.ON_RESUME && awaitingBrowser && leftToBrowser) {
                awaitingBrowser = false
                leftToBrowser = false
                countdownJob?.cancel()
                countdownJob = null

                val elapsed = ((SystemClock.elapsedRealtime() - browserStartElapsedMs) / 1000L).toInt().coerceAtLeast(0)
                val req = config.requiredExploreSeconds.coerceAtLeast(1)
                if (elapsed >= req) {
                    val uid = profile?.uid
                    if (uid != null) {
                        scope.launch {
                            val res = viewModel.repo.addGems(uid, "megaOfferGems", config.gemsPerWin)
                            if (res.isSuccess) {
                                viewModel.soundFx.playCelebration()
                                celebrate = true
                                gemEarnedMsg = "You explored the website for ${WebExploreHelper.formatDuration(elapsed)} (Required: ${WebExploreHelper.formatDuration(req)}). +${config.gemsPerWin} Gem added!"
                                streak = 0
                                round = generateRound()
                            }
                        }
                    }
                } else {
                    viewModel.soundFx.playError()
                    earlyMsg = "You stayed on the website for ${WebExploreHelper.formatDuration(elapsed)} only. Please explore for at least ${WebExploreHelper.formatDuration(req)} to claim your gem."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            countdownJob?.cancel()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentGems = profile?.megaOfferGems ?: 0
    val goal = config.gemGoal.coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text(config.title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = colors.lavenderSoft) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_gem), null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("$currentGems / $goal", color = colors.primaryIndigo, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(config.correctAnswersNeeded) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i < streak) 12.dp else 10.dp)
                            .clip(CircleShape)
                            .background(if (i < streak) colors.primaryIndigo else colors.cardBorder)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("$streak / ${config.correctAnswersNeeded}", color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier.size(180.dp),
                shape = RoundedCornerShape(26.dp),
                color = colors.lavenderSoft,
                border = BorderStroke(1.5.dp, colors.primaryIndigo.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = round.first.toString(),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("Select the matching number", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            Spacer(Modifier.height(14.dp))

            val choices = round.second
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (rowIdx in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        for (colIdx in 0..1) {
                            val value = choices[rowIdx * 2 + colIdx]
                            PRCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 20.dp,
                                contentPadding = PaddingValues(vertical = 15.dp),
                                onClick = {
                                    viewModel.soundFx.playClick()
                                    if (value == round.first) {
                                        val next = streak + 1
                                        if (next >= config.correctAnswersNeeded) {
                                            streak = next
                                            showClaimPopup = true
                                        } else {
                                            streak = next
                                            round = generateRound()
                                        }
                                    } else {
                                        viewModel.showToast("Wrong number! Match ${round.first}.", ToastType.ERROR)
                                        round = generateRound()
                                    }
                                }
                            ) {
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mega Offer Goal", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("$currentGems / $goal Gems", fontWeight = FontWeight.Bold, color = colors.primaryIndigo)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (currentGems.toFloat() / goal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                    color = colors.amberAccent,
                    trackColor = colors.amberSoft
                )
                Spacer(Modifier.height(12.dp))
                PrimaryGradientButton(
                    text = if (currentGems >= goal) "Claim ${config.rewardCoins} Coins" else "Reach $goal Gems for ${config.rewardCoins} Coins",
                    enabled = currentGems >= goal,
                    isLoading = claimingCoins,
                    gradient = colors.megaGradient,
                    onClick = {
                        val uid = profile?.uid ?: return@PrimaryGradientButton
                        scope.launch {
                            claimingCoins = true
                            val res = viewModel.repo.claimGemGoal(
                                uid = uid,
                                gemField = "megaOfferGems",
                                goal = goal,
                                rewardCoins = config.rewardCoins,
                                txType = "MEGA_GEM",
                                txTitle = "Mega Offer Goal Reward"
                            )
                            claimingCoins = false
                            if (res.isSuccess) {
                                viewModel.soundFx.playCelebration()
                                celebrate = true
                                viewModel.showToast("+${config.rewardCoins} coins added to your wallet!", ToastType.SUCCESS)
                                delay(2500)
                                celebrate = false
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Failed to claim.", ToastType.ERROR)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        CelebrationBurstOverlay(visible = celebrate)
    }

    if (showClaimPopup) {
        AlertDialog(
            onDismissRequest = { showClaimPopup = false },
            containerColor = colors.card,
            title = { Text("Challenge Completed!", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    "Tap Claim Gem to open the website in Chrome. Stay on it for at least ${WebExploreHelper.formatDuration(config.requiredExploreSeconds)} (live countdown shows in your notification bar), then return to collect +${config.gemsPerWin} Gem!",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Claim Gem",
                    height = 44.dp,
                    onClick = {
                        showClaimPopup = false
                        launchPartnerWebsite()
                    }
                )
            },
            dismissButton = { TextButton(onClick = { showClaimPopup = false }) { Text("Cancel") } }
        )
    }

    earlyMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { earlyMsg = null },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.TimerOff, null, tint = colors.roseAccent) },
            title = { Text("Explore Time Not Met", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text(m, color = colors.textSecondary) },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Re-open Website",
                    height = 42.dp,
                    onClick = {
                        earlyMsg = null
                        launchPartnerWebsite()
                    }
                )
            },
            dismissButton = { TextButton(onClick = { earlyMsg = null }) { Text("Cancel") } }
        )
    }

    gemEarnedMsg?.let { m ->
        AlertDialog(
            onDismissRequest = { gemEarnedMsg = null; celebrate = false },
            containerColor = colors.card,
            icon = { Image(painterResource(R.drawable.ic_gem), null, modifier = Modifier.size(44.dp)) },
            title = { Text("Gem Claimed!", fontWeight = FontWeight.ExtraBold, color = colors.mintAccent) },
            text = { Text(m, color = colors.textPrimary, textAlign = TextAlign.Center) },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Awesome! OK",
                    height = 42.dp,
                    onClick = { gemEarnedMsg = null; celebrate = false }
                )
            }
        )
    }
}

// ============================================================================
// SUPER OFFER — SCREEN 1: COMPACT ARENA LOBBY + FREE PRACTICE (0 -> +1 COIN)
// ============================================================================

private val FREE_PRACTICE_TIER = TicTacToeTier(
    id = "free_practice",
    difficulty = "FREE",
    entryCoins = 0L,
    winCoins = 1L,
    label = "Free Practice"
)

private fun defaultTicTacToeTiers(): List<TicTacToeTier> = listOf(
    FREE_PRACTICE_TIER,
    TicTacToeTier("low_10", "LOW", 10, 12, "Starter"),
    TicTacToeTier("med_10", "MEDIUM", 10, 15, "Pro"),
    TicTacToeTier("high_10", "HIGH", 10, 18, "Master"),
    TicTacToeTier("high_100", "HIGH", 100, 180, "Champion"),
    TicTacToeTier("high_500", "HIGH", 500, 950, "Legend")
)

private fun buildArenaTiers(adminTiers: List<TicTacToeTier>): List<TicTacToeTier> {
    val base = if (adminTiers.isNotEmpty()) adminTiers else defaultTicTacToeTiers()
    return if (base.any { it.entryCoins == 0L }) base else listOf(FREE_PRACTICE_TIER) + base
}

@Composable
fun SuperOfferTicTacToeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEnterFreePractice: (String) -> Unit,
    onEnterPvpMatchmaking: (String) -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.superOfferConfig.collectAsState()

    val allTiers = remember(config.tiers) { buildArenaTiers(config.tiers) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var pendingTier by remember { mutableStateOf<TicTacToeTier?>(null) }
    var starting by remember { mutableStateOf(false) }

    val visibleTiers = remember(allTiers, selectedFilter) {
        if (selectedFilter == "ALL") allTiers else allTiers.filter { it.difficulty == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Compact Top Bar
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Super Offer", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = colors.amberSoft, border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.35f))) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("${profile?.coins ?: 0}", color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Sleek Compact Hero Banner (No Oversized Height!)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.superGradient)
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                            Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("REAL 1v1 PVP ARENA", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                            }
                        }
                        Box(
                            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Grid3x3, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = config.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "Free Practice vs AI (+1 coin) • Paid Tables are 100% Real 1v1 Players (Zero Bots)!",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArenaRuleChip("Win = Instant Coins")
                        ArenaRuleChip("Draw = Full Refund")
                    }
                }
            }
        }

        // Single-Line Scrollable Difficulty Pills (Zero Text Wrapping!)
        item {
            Column {
                Text("Choose Difficulty", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL" to "All", "FREE" to "Free", "LOW" to "Low", "MEDIUM" to "Medium", "HIGH" to "High")) { (key, label) ->
                        val active = selectedFilter == key
                        Surface(
                            modifier = Modifier.bounceClick {
                                viewModel.soundFx.playClick()
                                selectedFilter = key
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) colors.primaryIndigo else colors.card,
                            border = BorderStroke(1.dp, if (active) colors.primaryIndigo else colors.cardBorder)
                        ) {
                            Text(
                                text = label,
                                color = if (active) Color.White else colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        // Compact, Single-Line Arena Cards
        items(visibleTiers, key = { it.id }) { tier ->
            val isFree = tier.entryCoins == 0L
            val (diffBg, diffTint) = when (tier.difficulty) {
                "FREE" -> colors.mintSoft to colors.mintAccent
                "LOW" -> colors.mintSoft to colors.mintAccent
                "MEDIUM" -> colors.amberSoft to colors.amberAccent
                else -> colors.lavenderSoft to colors.primaryIndigo
            }
            PRCard(
                cornerRadius = 15.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
                onClick = {
                    viewModel.soundFx.playClick()
                    pendingTier = tier
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconBadge(
                        icon = when (tier.difficulty) {
                            "FREE" -> Icons.Rounded.School
                            "LOW" -> Icons.Rounded.Bolt
                            "MEDIUM" -> Icons.Rounded.WorkspacePremium
                            else -> Icons.Rounded.LocalFireDepartment
                        },
                        containerColor = diffBg,
                        iconTint = diffTint,
                        size = 40.dp,
                        cornerRadius = 12.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tier.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.5.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(shape = CircleShape, color = diffBg) {
                                Text(
                                    text = if (isFree) "AI PRACTICE" else "1v1 REAL • ${tier.difficulty}",
                                    color = diffTint,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        val infoText = if (isFree) {
                            "Entry: FREE  •  Win: +${tier.winCoins} Coin"
                        } else {
                            "Entry: ${tier.entryCoins}  •  Win: ${tier.winCoins} Coins"
                        }
                        Text(
                            text = infoText,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isFree) colors.megaGradient else colors.heroGradient)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        // Compact Terms & Conditions Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.amberSoft,
                border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Gavel, null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Arena Rules & Terms", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB45309))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = config.termsAndConditions.ifBlank {
                            "• Free Practice costs 0 coins and awards +1 coin on win.\n• Paid tables deduct entry coins on start and credit win coins instantly.\n• Draws refund 100% of paid entry stakes."
                        },
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }

    // Match Entry Confirmation Dialog
    pendingTier?.let { tier ->
        val isFree = tier.entryCoins == 0L
        AlertDialog(
            onDismissRequest = { if (!starting) pendingTier = null },
            containerColor = colors.card,
            icon = { IconBadge(Icons.Rounded.SportsEsports, colors.lavenderSoft, colors.primaryIndigo, size = 44.dp) },
            title = { Text("Enter ${tier.label}?", fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Difficulty", color = colors.textSecondary)
                        Text(tier.difficulty, fontWeight = FontWeight.Bold, color = colors.primaryIndigo)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Entry Stake", color = colors.textSecondary)
                        Text(if (isFree) "FREE (0 Coins)" else "${tier.entryCoins} Coins", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Winner Prize", color = colors.textSecondary)
                        Text("+${tier.winCoins} ${if (tier.winCoins == 1L) "Coin" else "Coins"}", fontWeight = FontWeight.ExtraBold, color = colors.mintAccent)
                    }
                }
            },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Start Match",
                    isLoading = starting,
                    height = 42.dp,
                    onClick = {
                        if (isFree) {
                            val id = tier.id
                            pendingTier = null
                            onEnterFreePractice(id)
                            return@PrimaryGradientButton
                        }
                        if ((profile?.coins ?: 0L) < tier.entryCoins) {
                            pendingTier = null
                            viewModel.showToast("Not enough coins! Play Free Practice or earn ${tier.entryCoins} coins.", ToastType.ERROR)
                            return@PrimaryGradientButton
                        }
                        val id = tier.id
                        pendingTier = null
                        onEnterPvpMatchmaking(id)
                    }
                )
            },
            dismissButton = {
                TextButton(enabled = !starting, onClick = { pendingTier = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ArenaRuleChip(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f)) {
        Text(text, color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp))
    }
}

// ============================================================================
// SUPER OFFER — SCREEN 2: PRO FULL-SCREEN 1v1 TIC-TAC-TOE ARENA + POPUPS
// ============================================================================

@Composable
fun TicTacToeMatchScreen(
    tierId: String,
    viewModel: MainViewModel,
    onExitToLobby: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.superOfferConfig.collectAsState()

    val tier = remember(config.tiers, tierId) {
        buildArenaTiers(config.tiers).find { it.id == tierId } ?: FREE_PRACTICE_TIER
    }
    val isFree = tier.entryCoins == 0L

    var board by remember { mutableStateOf(List(9) { "" }) }
    var aiThinking by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<String?>(null) } // null = LIVE, WIN, LOSS, DRAW
    var showResultPopup by remember { mutableStateOf(false) }
    var winningLine by remember { mutableStateOf<List<Int>>(emptyList()) }
    var lastAiCell by remember { mutableIntStateOf(-1) }
    var celebrate by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }

    // Session Scoreboard
    var playerWins by remember { mutableIntStateOf(0) }
    var aiWins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }

    // Per-turn 15-second arcade countdown
    var turnSecondsLeft by remember { mutableIntStateOf(15) }
    val strikeProgress = remember { Animatable(0f) }

    val winLines = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
        listOf(0, 4, 8), listOf(2, 4, 6)
    )

    fun checkWinner(b: List<String>): Pair<String, List<Int>>? {
        for (line in winLines) {
            val a = b[line[0]]
            if (a.isNotEmpty() && a == b[line[1]] && a == b[line[2]]) return a to line
        }
        return null
    }

    fun minimax(b: MutableList<String>, isAi: Boolean): Int {
        val w = checkWinner(b)?.first
        if (w == "O") return 10
        if (w == "X") return -10
        if (b.none { it.isEmpty() }) return 0
        var best = if (isAi) Int.MIN_VALUE else Int.MAX_VALUE
        for (i in 0..8) {
            if (b[i].isEmpty()) {
                b[i] = if (isAi) "O" else "X"
                val score = minimax(b, !isAi)
                b[i] = ""
                best = if (isAi) maxOf(best, score) else minOf(best, score)
            }
        }
        return best
    }

    fun pickAiMove(b: List<String>, difficulty: String): Int {
        val empty = b.indices.filter { b[it].isEmpty() }
        if (empty.isEmpty()) return -1
        if ((difficulty == "FREE" || difficulty == "LOW") && Random.nextFloat() < 0.72f) return empty.random()
        if (difficulty == "MEDIUM" && Random.nextFloat() < 0.28f) return empty.random()
        val copy = b.toMutableList()
        var bestMove = empty.first()
        var bestScore = Int.MIN_VALUE
        for (idx in empty) {
            copy[idx] = "O"
            val score = minimax(copy, false)
            copy[idx] = ""
            if (score > bestScore) {
                bestScore = score
                bestMove = idx
            }
        }
        return bestMove
    }

    fun finishMatch(result: String, line: List<Int>) {
        matchResult = result
        winningLine = line
        when (result) {
            "WIN" -> playerWins++
            "LOSS" -> aiWins++
            "DRAW" -> draws++
        }
        val uid = profile?.uid
        scope.launch {
            if (line.isNotEmpty()) {
                strikeProgress.snapTo(0f)
                strikeProgress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            }
            if (uid != null) {
                when (result) {
                    "WIN" -> {
                        viewModel.soundFx.playCelebration()
                        celebrate = true
                        viewModel.repo.adjustCoins(uid, tier.winCoins, true, "TIC_TAC_TOE_WIN", "Tic-Tac-Toe Win (${tier.label})")
                    }
                    "DRAW" -> {
                        viewModel.soundFx.playClick()
                        if (!isFree) {
                            viewModel.repo.adjustCoins(uid, tier.entryCoins, false, "TIC_TAC_TOE_WIN", "Tic-Tac-Toe Draw Refund")
                        }
                    }
                    "LOSS" -> {
                        viewModel.soundFx.playError()
                    }
                }
            }
            delay(320)
            showResultPopup = true
        }
    }

    fun triggerAiTurn(currentBoard: List<String>) {
        aiThinking = true
        scope.launch {
            delay(460)
            val aiMove = pickAiMove(currentBoard, tier.difficulty)
            if (aiMove != -1) {
                lastAiCell = aiMove
                viewModel.soundFx.playClick()
                val afterAi = currentBoard.toMutableList().apply { this[aiMove] = "O" }
                board = afterAi
                val aiWin = checkWinner(afterAi)
                if (aiWin != null) finishMatch("LOSS", aiWin.second)
                else if (afterAi.none { it.isEmpty() }) finishMatch("DRAW", emptyList())
                else turnSecondsLeft = 15
            }
            aiThinking = false
        }
    }

    fun startRematch() {
        if (isFree) {
            showResultPopup = false
            celebrate = false
            board = List(9) { "" }
            winningLine = emptyList()
            lastAiCell = -1
            matchResult = null
            turnSecondsLeft = 15
            return
        }
        val uid = profile?.uid ?: return
        if ((profile?.coins ?: 0L) < tier.entryCoins) {
            viewModel.showToast("Not enough coins for rematch! Need ${tier.entryCoins} coins.", ToastType.ERROR)
            return
        }
        scope.launch {
            restarting = true
            val res = viewModel.repo.adjustCoins(
                uid = uid,
                deltaCoins = -tier.entryCoins,
                countAsEarned = false,
                txType = "TIC_TAC_TOE_ENTRY",
                txTitle = "Tic-Tac-Toe Rematch (${tier.label})"
            )
            restarting = false
            if (res.isSuccess) {
                showResultPopup = false
                celebrate = false
                board = List(9) { "" }
                winningLine = emptyList()
                lastAiCell = -1
                matchResult = null
                turnSecondsLeft = 15
            } else {
                viewModel.showToast(res.exceptionOrNull()?.message ?: "Rematch failed.", ToastType.ERROR)
            }
        }
    }

    // 15-second turn timer during player's turn
    LaunchedEffect(matchResult, aiThinking, board) {
        if (matchResult == null && !aiThinking) {
            turnSecondsLeft = 15
            while (turnSecondsLeft > 0 && matchResult == null && !aiThinking) {
                delay(1000)
                turnSecondsLeft--
            }
            if (turnSecondsLeft == 0 && matchResult == null && !aiThinking) {
                // Auto-pass to AI if player times out
                triggerAiTurn(board)
            }
        }
    }

    BackHandler(enabled = matchResult == null) { showLeaveDialog = true }

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Match Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.size(36.dp).bounceClick { if (matchResult == null) showLeaveDialog = true else onExitToLobby() },
                    shape = RoundedCornerShape(11.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.cardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Leave", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    }
                }
                Surface(shape = CircleShape, color = colors.lavenderSoft) {
                    Text(
                        text = "${tier.label} • ${tier.difficulty}",
                        color = colors.primaryIndigo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Surface(shape = CircleShape, color = colors.mintSoft, border = BorderStroke(1.dp, colors.mintAccent.copy(alpha = 0.35f))) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Win +${tier.winCoins}", color = colors.mintAccent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Session Scoreboard Strip (You | Draws | AI)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.cardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScorePill("You", playerWins, colors.primaryIndigo)
                    ScorePill("Draws", draws, colors.textSecondary)
                    ScorePill("AI Bot", aiWins, colors.amberAccent)
                }
            }

            Spacer(Modifier.height(12.dp))

            // VS Player Turn Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val playerTurn = matchResult == null && !aiThinking
                PlayerTurnCard(
                    name = profile?.fullName?.substringBefore(" ")?.ifBlank { "You" } ?: "You",
                    symbol = "X",
                    active = playerTurn,
                    accent = colors.primaryIndigo,
                    softBg = colors.lavenderSoft,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = CircleShape, color = colors.card, border = BorderStroke(1.dp, colors.cardBorder)) {
                    Text("VS", fontWeight = FontWeight.ExtraBold, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
                PlayerTurnCard(
                    name = "AI Bot",
                    symbol = "O",
                    active = matchResult == null && aiThinking,
                    accent = colors.amberAccent,
                    softBg = colors.amberSoft,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Turn Status + 15s Timer Bar
            if (matchResult == null) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (aiThinking) "AI is thinking..." else "Your Turn — Tap a square",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aiThinking) colors.amberAccent else colors.primaryIndigo
                        )
                        if (!aiThinking) {
                            Text(
                                text = "${turnSecondsLeft}s",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (turnSecondsLeft <= 5) colors.roseAccent else colors.primaryIndigo
                            )
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { if (aiThinking) 1f else (turnSecondsLeft / 15f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = if (aiThinking) colors.amberAccent else if (turnSecondsLeft <= 5) colors.roseAccent else colors.primaryIndigo,
                        trackColor = colors.inputBg
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Dedicated 3x3 Game Board WITH Animated Winning Strike-Through Line Overlay!
            PRCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(14.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (r in 0..2) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            ) {
                                for (c in 0..2) {
                                    val idx = r * 3 + c
                                    val cell = board[idx]
                                    val isWinCell = idx in winningLine
                                    AnimatedTicTacToeCell(
                                        symbol = cell,
                                        isWinningCell = isWinCell,
                                        isRecentMove = idx == lastAiCell && matchResult == null,
                                        enabled = matchResult == null && !aiThinking && cell.isEmpty(),
                                        onClick = {
                                            viewModel.soundFx.playClick()
                                            val updated = board.toMutableList().apply { this[idx] = "X" }
                                            board = updated
                                            val playerWin = checkWinner(updated)
                                            if (playerWin != null) {
                                                finishMatch("WIN", playerWin.second)
                                            } else if (updated.none { it.isEmpty() }) {
                                                finishMatch("DRAW", emptyList())
                                            } else {
                                                triggerAiTurn(updated)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }

                    // Animated Glowing Winning Strike-Through Line across the 3 winning cells
                    if (winningLine.size == 3 && strikeProgress.value > 0f) {
                        val lineBrush = if (matchResult == "WIN") {
                            Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cellW = size.width / 3f
                            val cellH = size.height / 3f
                            val startIdx = winningLine.first()
                            val endIdx = winningLine.last()
                            val sx = (startIdx % 3) * cellW + cellW / 2f
                            val sy = (startIdx / 3) * cellH + cellH / 2f
                            val ex = (endIdx % 3) * cellW + cellW / 2f
                            val ey = (endIdx / 3) * cellH + cellH / 2f
                            val p = strikeProgress.value
                            drawLine(
                                brush = lineBrush,
                                start = Offset(sx, sy),
                                end = Offset(sx + (ex - sx) * p, sy + (ey - sy) * p),
                                strokeWidth = 9.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (matchResult != null && !showResultPopup) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryOutlineButton(text = "Change Table", onClick = onExitToLobby, modifier = Modifier.weight(1f))
                    PrimaryGradientButton(
                        text = if (isFree) "Play Again (Free)" else "Play Again (${tier.entryCoins})",
                        isLoading = restarting,
                        onClick = { startRematch() },
                        modifier = Modifier.weight(1.25f)
                    )
                }
            } else {
                Text(
                    text = if (isFree) "Free Practice Mode • Win +1 Coin" else "Stake locked: ${tier.entryCoins} Coins • Balance: ${profile?.coins ?: 0} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(6.dp))
        }

        // Full-Screen Confetti Burst on Win!
        CelebrationBurstOverlay(visible = celebrate)
    }

    // ========================================================================
    // CELEBRATION VICTORY / DRAW / DEFEAT RESULT POPUP DIALOG
    // ========================================================================
    if (showResultPopup && matchResult != null) {
        val isWin = matchResult == "WIN"
        val isDraw = matchResult == "DRAW"
        Dialog(
            onDismissRequest = { showResultPopup = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            Box(contentAlignment = Alignment.Center) {
                PRCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 26.dp,
                    contentPadding = PaddingValues(22.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        // Animated Bouncing Emblem Badge
                        val trophyAnim = rememberInfiniteTransition(label = "popupTrophy")
                        val trophyScale by trophyAnim.animateFloat(
                            initialValue = 0.93f,
                            targetValue = 1.08f,
                            animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
                            label = "trophyBounce"
                        )
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .scale(if (isWin) trophyScale else 1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isWin -> colors.amberSoft
                                        isDraw -> colors.lavenderSoft
                                        else -> colors.roseSoft
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isWin) {
                                Image(painterResource(R.drawable.ic_pr_trophy), contentDescription = "Victory", modifier = Modifier.size(48.dp))
                            } else if (isDraw) {
                                Icon(Icons.Rounded.Handshake, null, tint = colors.primaryIndigo, modifier = Modifier.size(42.dp))
                            } else {
                                Icon(Icons.Rounded.SentimentDissatisfied, null, tint = colors.roseAccent, modifier = Modifier.size(42.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = when {
                                isWin -> "VICTORY!"
                                isDraw -> "MATCH DRAW!"
                                else -> "AI WON!"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                isWin -> colors.mintAccent
                                isDraw -> colors.primaryIndigo
                                else -> colors.roseAccent
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = when {
                                isWin -> "Congratulations! You defeated the AI in ${tier.label} mode."
                                isDraw -> if (isFree) "Close battle! Play another round to win +1 coin." else "Nobody lost — your ${tier.entryCoins} coins stake has been refunded."
                                else -> "AI outplayed this round. Ready for a quick rematch?"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(16.dp))

                        // Coin Reward Highlight Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isWin -> colors.mintSoft
                                isDraw -> colors.lavenderSoft
                                else -> colors.inputBg
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isWin -> colors.mintAccent.copy(alpha = 0.4f)
                                    isDraw -> colors.primaryIndigo.copy(alpha = 0.3f)
                                    else -> colors.cardBorder
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = when {
                                        isWin -> "+${tier.winCoins} ${if (tier.winCoins == 1L) "Coin" else "Coins"} Won!"
                                        isDraw -> if (isFree) "0 Coins — Try Again" else "+${tier.entryCoins} Coins Refunded"
                                        else -> "0 Coins Won"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        isWin -> colors.mintAccent
                                        isDraw -> colors.primaryIndigo
                                        else -> colors.textSecondary
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Primary Play Again CTA
                        PrimaryGradientButton(
                            text = if (isFree) "Play Again (Free)" else "Play Again (${tier.entryCoins} Coins)",
                            isLoading = restarting,
                            leadingIcon = Icons.Rounded.Replay,
                            gradient = if (isWin) colors.megaGradient else colors.heroGradient,
                            onClick = { startRematch() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        // OK / Back to Lobby CTA
                        SecondaryOutlineButton(
                            text = if (isWin) "OK, Collect & Back" else "OK, Back to Arena",
                            onClick = {
                                showResultPopup = false
                                onExitToLobby()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.WarningAmber, null, tint = colors.amberAccent) },
            title = { Text("Leave Match?", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    text = if (isFree) "Are you sure you want to leave this practice match?"
                    else "Leaving mid-match forfeits your ${tier.entryCoins} coins entry stake. Are you sure?",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false; onExitToLobby() }) {
                    Text(if (isFree) "Leave" else "Forfeit & Leave", color = colors.roseAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Keep Playing") } }
        )
    }
}

@Composable
private fun ScorePill(label: String, score: Int, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = PRTheme.colors.textSecondary, fontWeight = FontWeight.Medium)
        Text("$score", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accent)
    }
}

@Composable
private fun PlayerTurnCard(
    name: String,
    symbol: String,
    active: Boolean,
    accent: Color,
    softBg: Color,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    val activePulse = rememberInfiniteTransition(label = "activeTurnPulse")
    val pulseVal by activePulse.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseVal"
    )
    val scale by animateFloatAsState(if (active) pulseVal else 0.97f, spring(stiffness = Spring.StiffnessMediumLow), label = "turnScale")
    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(15.dp),
        color = if (active) softBg else colors.card,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) accent else colors.cardBorder),
        shadowElevation = if (active) 4.dp else 1.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(accent), contentAlignment = Alignment.Center) {
                Text(symbol, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, fontSize = 13.sp)
                Text(if (active) "Playing..." else "Waiting", style = MaterialTheme.typography.labelSmall, color = if (active) accent else colors.textSecondary)
            }
        }
    }
}

@Composable
private fun AnimatedTicTacToeCell(
    symbol: String,
    isWinningCell: Boolean,
    isRecentMove: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    val drawProgress = remember(symbol) { Animatable(0f) }
    val burstProgress = remember(symbol) { Animatable(0f) }
    val popScale = remember(symbol) { Animatable(1f) }

    LaunchedEffect(symbol) {
        if (symbol.isNotEmpty()) {
            drawProgress.snapTo(0f)
            burstProgress.snapTo(0f)
            popScale.snapTo(0.84f)
            launch {
                popScale.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
            launch {
                drawProgress.animateTo(1f, tween(270, easing = FastOutSlowInEasing))
            }
            launch {
                burstProgress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            }
        }
    }

    val winPulse = rememberInfiniteTransition(label = "winCellPulse")
    val winScale by winPulse.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "winScale"
    )

    val effectiveScale = if (isWinningCell) winScale else popScale.value
    val accentColor = if (symbol == "X") colors.primaryIndigo else Color(0xFFF97316)

    Surface(
        modifier = modifier
            .scale(effectiveScale)
            .bounceClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = when {
            isWinningCell -> colors.mintSoft
            symbol == "X" -> colors.lavenderSoft
            symbol == "O" -> colors.amberSoft
            else -> colors.inputBg
        },
        border = BorderStroke(
            width = if (isWinningCell || isRecentMove) 2.5.dp else 1.2.dp,
            color = when {
                isWinningCell -> colors.mintAccent
                isRecentMove -> colors.amberAccent
                symbol == "X" -> colors.primaryIndigo.copy(alpha = 0.45f)
                symbol == "O" -> colors.amberAccent.copy(alpha = 0.45f)
                else -> colors.cardBorder
            }
        ),
        shadowElevation = if (isWinningCell) 6.dp else if (symbol.isNotEmpty()) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            // Shockwave ripple ring + 10-particle sparkle burst on every move!
            if (symbol.isNotEmpty() && burstProgress.value < 0.99f) {
                val bp = burstProgress.value
                val alpha = (1f - bp).coerceIn(0f, 1f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.minDimension * 0.48f
                    drawCircle(
                        color = accentColor.copy(alpha = alpha * 0.45f),
                        radius = maxR * bp,
                        center = Offset(cx, cy),
                        style = Stroke(width = (3.5f * alpha).dp.toPx())
                    )
                    for (i in 0 until 10) {
                        val angle = (i * 36.0) * (Math.PI / 180.0)
                        val dist = maxR * 0.3f + maxR * 0.7f * bp
                        val px = cx + (cos(angle) * dist).toFloat()
                        val py = cy + (sin(angle) * dist).toFloat()
                        drawCircle(
                            color = if (i % 2 == 0) accentColor.copy(alpha = alpha) else Color(0xFFFBBF24).copy(alpha = alpha),
                            radius = (3.2f * alpha + 1f).dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // Crisp animated X / O vector stroke
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(18.dp)) {
                if (symbol == "X") {
                    val p = drawProgress.value
                    val xBrush = Brush.linearGradient(listOf(colors.primaryIndigo, colors.primaryViolet))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
                        val p1 = (p * 2f).coerceIn(0f, 1f)
                        val p2 = ((p - 0.5f) * 2f).coerceIn(0f, 1f)
                        if (p1 > 0f) {
                            drawLine(brush = xBrush, start = Offset(0f, 0f), end = Offset(size.width * p1, size.height * p1), strokeWidth = stroke.width, cap = StrokeCap.Round)
                        }
                        if (p2 > 0f) {
                            drawLine(brush = xBrush, start = Offset(size.width, 0f), end = Offset(size.width * (1f - p2), size.height * p2), strokeWidth = stroke.width, cap = StrokeCap.Round)
                        }
                    }
                } else if (symbol == "O") {
                    val p = drawProgress.value
                    val oBrush = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316)))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            brush = oBrush,
                            startAngle = -90f,
                            sweepAngle = 360f * p,
                            useCenter = false,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height),
                            style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
