package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var waitingWebReturn by remember { mutableStateOf(false) }
    var webOpenedAtMs by remember { mutableLongStateOf(0L) }
    var earlyMsg by remember { mutableStateOf<String?>(null) }
    var claimingCoins by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, waitingWebReturn, webOpenedAtMs) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingWebReturn) {
                waitingWebReturn = false
                val elapsed = ((System.currentTimeMillis() - webOpenedAtMs) / 1000L).toInt()
                val req = config.requiredExploreSeconds.coerceAtLeast(1)
                if (elapsed >= req) {
                    val uid = profile?.uid
                    if (uid != null) {
                        scope.launch {
                            viewModel.repo.addGems(uid, "megaOfferGems", config.gemsPerWin)
                            viewModel.showToast("+${config.gemsPerWin} Mega Offer Gem earned!", ToastType.SUCCESS)
                            streak = 0
                            round = generateRound()
                        }
                    }
                } else {
                    earlyMsg = "Explored for ${elapsed}s only. Please stay on the website for at least ${req} seconds to earn your gem."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.size(190.dp),
                shape = RoundedCornerShape(28.dp),
                color = colors.lavenderSoft,
                border = BorderStroke(1.5.dp, colors.primaryIndigo.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = round.first.toString(),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Select the matching number", style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))

            val choices = round.second
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (rowIdx in 0..1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        for (colIdx in 0..1) {
                            val value = choices[rowIdx * 2 + colIdx]
                            PRCard(
                                modifier = Modifier.weight(1f),
                                cornerRadius = 24.dp,
                                contentPadding = PaddingValues(vertical = 16.dp),
                                onClick = {
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
                                        viewModel.showToast("Wrong number! Try matching ${round.first}.", ToastType.ERROR)
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

            Spacer(Modifier.height(28.dp))
            PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mega Offer Goal", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("$currentGems / $goal Gems", fontWeight = FontWeight.Bold, color = colors.primaryIndigo)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (currentGems.toFloat() / goal.toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                    color = colors.amberAccent,
                    trackColor = colors.amberSoft
                )
                Spacer(Modifier.height(14.dp))
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
                Text("Visit the partner website and explore for at least ${config.requiredExploreSeconds} seconds, then return to collect +${config.gemsPerWin} gem.", color = colors.textSecondary)
            },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Claim Gem",
                    height = 44.dp,
                    onClick = {
                        showClaimPopup = false
                        val url = config.websiteUrl.ifBlank { "https://playreward.app" }
                        webOpenedAtMs = System.currentTimeMillis()
                        waitingWebReturn = true
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) { waitingWebReturn = false }
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
            title = { Text("Explore Time Not Met", color = colors.textPrimary) },
            text = { Text(m, color = colors.textSecondary) },
            confirmButton = { TextButton(onClick = { earlyMsg = null }) { Text("OK") } }
        )
    }
}

// ============================================================================
// SUPER OFFER — SCREEN 1: DEDICATED ARENA LOBBY
// ============================================================================

private fun defaultTicTacToeTiers(): List<TicTacToeTier> = listOf(
    TicTacToeTier("low_10", "LOW", 10, 12, "Starter"),
    TicTacToeTier("med_10", "MEDIUM", 10, 15, "Pro"),
    TicTacToeTier("high_10", "HIGH", 10, 18, "Master"),
    TicTacToeTier("high_100", "HIGH", 100, 180, "Champion"),
    TicTacToeTier("high_500", "HIGH", 500, 950, "Legend")
)

@Composable
fun SuperOfferTicTacToeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEnterMatch: (String) -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.superOfferConfig.collectAsState()

    val allTiers = remember(config.tiers) {
        if (config.tiers.isNotEmpty()) config.tiers else defaultTicTacToeTiers()
    }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var pendingTier by remember { mutableStateOf<TicTacToeTier?>(null) }
    var starting by remember { mutableStateOf(false) }

    val visibleTiers = remember(allTiers, selectedFilter) {
        if (selectedFilter == "ALL") allTiers else allTiers.filter { it.difficulty == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Text("Super Offer", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Surface(shape = CircleShape, color = colors.amberSoft, border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.3f))) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${profile?.coins ?: 0}", color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Game-Vibe Hero Arena Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.superGradient)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.22f)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.SportsEsports, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("1v1 AI ARENA", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Grid3x3, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(config.title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Choose your battle table below. Defeat the AI in a full-screen 1v1 match to multiply your coins instantly!",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArenaRuleChip("Win = Instant Coins")
                        ArenaRuleChip("Draw = Full Refund")
                    }
                }
            }
        }

        // Difficulty Filter Chips
        item {
            Column {
                Text("Choose Difficulty", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("ALL" to "All", "LOW" to "Low", "MEDIUM" to "Medium", "HIGH" to "High").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Arena Stake Cards
        items(visibleTiers, key = { it.id }) { tier ->
            val (diffBg, diffTint) = when (tier.difficulty) {
                "LOW" -> colors.mintSoft to colors.mintAccent
                "MEDIUM" -> colors.amberSoft to colors.amberAccent
                else -> colors.lavenderSoft to colors.primaryIndigo
            }
            PRCard(
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(16.dp),
                onClick = { pendingTier = tier }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconBadge(
                        icon = when (tier.difficulty) {
                            "LOW" -> Icons.Rounded.Bolt
                            "MEDIUM" -> Icons.Rounded.WorkspacePremium
                            else -> Icons.Rounded.LocalFireDepartment
                        },
                        containerColor = diffBg,
                        iconTint = diffTint,
                        size = 48.dp,
                        cornerRadius = 15.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tier.label, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Surface(shape = CircleShape, color = diffBg) {
                                Text(
                                    text = tier.difficulty,
                                    color = diffTint,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Entry: ${tier.entryCoins} Coins  •  Win: ${tier.winCoins} Coins", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.heroGradient)
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text("Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Terms & Conditions Card (Admin-managed)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = colors.amberSoft,
                border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Gavel, null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Arena Rules & Terms", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = config.termsAndConditions.ifBlank {
                            "1. Entry coins are deducted when you start a match.\n2. Winning credits the prize instantly to your wallet.\n3. Draws refund 100% of your entry stake.\n4. Leaving a live match early forfeits the entry stake."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }

    // Match Entry Confirmation Dialog
    pendingTier?.let { tier ->
        AlertDialog(
            onDismissRequest = { if (!starting) pendingTier = null },
            containerColor = colors.card,
            icon = { IconBadge(Icons.Rounded.SportsEsports, colors.lavenderSoft, colors.primaryIndigo, size = 48.dp) },
            title = { Text("Enter ${tier.label} Match?", fontWeight = FontWeight.Bold, color = colors.textPrimary, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Difficulty", color = colors.textSecondary)
                        Text(tier.difficulty, fontWeight = FontWeight.Bold, color = colors.primaryIndigo)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Entry Stake", color = colors.textSecondary)
                        Text("${tier.entryCoins} Coins", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Winner Prize", color = colors.textSecondary)
                        Text("${tier.winCoins} Coins", fontWeight = FontWeight.ExtraBold, color = colors.mintAccent)
                    }
                }
            },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Start Match",
                    isLoading = starting,
                    height = 44.dp,
                    onClick = {
                        val uid = profile?.uid ?: return@PrimaryGradientButton
                        if ((profile?.coins ?: 0L) < tier.entryCoins) {
                            pendingTier = null
                            viewModel.showToast("Not enough coins! You need ${tier.entryCoins} coins to join.", ToastType.ERROR)
                            return@PrimaryGradientButton
                        }
                        scope.launch {
                            starting = true
                            val res = viewModel.repo.adjustCoins(
                                uid = uid,
                                deltaCoins = -tier.entryCoins,
                                countAsEarned = false,
                                txType = "TIC_TAC_TOE_ENTRY",
                                txTitle = "Tic-Tac-Toe Entry (${tier.label})"
                            )
                            starting = false
                            if (res.isSuccess) {
                                val id = tier.id
                                pendingTier = null
                                onEnterMatch(id)
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Could not start match.", ToastType.ERROR)
                            }
                        }
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
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
        Text(text, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

// ============================================================================
// SUPER OFFER — SCREEN 2: FULL-SCREEN DEDICATED TIC-TAC-TOE GAME ARENA
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
        val list = if (config.tiers.isNotEmpty()) config.tiers else defaultTicTacToeTiers()
        list.find { it.id == tierId } ?: list.first()
    }

    var board by remember { mutableStateOf(List(9) { "" }) }
    var aiThinking by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<String?>(null) } // null = LIVE, WIN, LOSS, DRAW
    var winningLine by remember { mutableStateOf<List<Int>>(emptyList()) }
    var celebrate by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var restarting by remember { mutableStateOf(false) }

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
        if (difficulty == "LOW" && Random.nextFloat() < 0.65f) return empty.random()
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
        val uid = profile?.uid ?: return
        scope.launch {
            when (result) {
                "WIN" -> {
                    viewModel.repo.adjustCoins(uid, tier.winCoins, true, "TIC_TAC_TOE_WIN", "Tic-Tac-Toe Win (${tier.label})")
                    celebrate = true
                    viewModel.showToast("Victory! +${tier.winCoins} coins credited!", ToastType.SUCCESS)
                }
                "DRAW" -> {
                    viewModel.repo.adjustCoins(uid, tier.entryCoins, false, "TIC_TAC_TOE_WIN", "Tic-Tac-Toe Draw Refund")
                    viewModel.showToast("Draw! Entry ${tier.entryCoins} coins refunded.", ToastType.INFO)
                }
                "LOSS" -> {
                    viewModel.showToast("AI won this round. Rematch anytime!", ToastType.ERROR)
                }
            }
        }
    }

    BackHandler(enabled = matchResult == null) {
        showLeaveDialog = true
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Match Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { if (matchResult == null) showLeaveDialog = true else onExitToLobby() }) {
                    Icon(Icons.Rounded.ArrowBackIosNew, "Leave", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
                Surface(shape = CircleShape, color = colors.lavenderSoft) {
                    Text(
                        text = "${tier.label} • ${tier.difficulty}",
                        color = colors.primaryIndigo,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Surface(shape = CircleShape, color = colors.mintSoft) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Win ${tier.winCoins}", color = colors.mintAccent, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // VS Scoreboard Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    Text("VS", fontWeight = FontWeight.ExtraBold, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
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

            Spacer(Modifier.height(18.dp))

            // Live Turn Status Pill
            Surface(
                shape = CircleShape,
                color = when (matchResult) {
                    "WIN" -> colors.mintSoft
                    "LOSS" -> colors.roseSoft
                    "DRAW" -> colors.amberSoft
                    else -> if (aiThinking) colors.amberSoft else colors.lavenderSoft
                }
            ) {
                Text(
                    text = when {
                        matchResult == "WIN" -> "You Won +${tier.winCoins} Coins!"
                        matchResult == "LOSS" -> "AI Won This Round"
                        matchResult == "DRAW" -> "Match Draw — ${tier.entryCoins} Coins Refunded"
                        aiThinking -> "AI is making a move..."
                        else -> "Your Turn — Tap any square"
                    },
                    color = when (matchResult) {
                        "WIN" -> colors.mintAccent
                        "LOSS" -> colors.roseAccent
                        "DRAW" -> Color(0xFFB45309)
                        else -> if (aiThinking) Color(0xFFB45309) else colors.primaryIndigo
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Dedicated Full-Width Animated 3x3 Board
            PRCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (r in 0..2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            for (c in 0..2) {
                                val idx = r * 3 + c
                                val cell = board[idx]
                                val isWinCell = idx in winningLine
                                AnimatedTicTacToeCell(
                                    symbol = cell,
                                    isWinningCell = isWinCell,
                                    enabled = matchResult == null && !aiThinking && cell.isEmpty(),
                                    onClick = {
                                        val updated = board.toMutableList().apply { this[idx] = "X" }
                                        board = updated
                                        val playerWin = checkWinner(updated)
                                        if (playerWin != null) {
                                            finishMatch("WIN", playerWin.second)
                                        } else if (updated.none { it.isEmpty() }) {
                                            finishMatch("DRAW", emptyList())
                                        } else {
                                            aiThinking = true
                                            scope.launch {
                                                delay(460)
                                                val aiMove = pickAiMove(updated, tier.difficulty)
                                                if (aiMove != -1) {
                                                    val afterAi = updated.toMutableList().apply { this[aiMove] = "O" }
                                                    board = afterAi
                                                    val aiWin = checkWinner(afterAi)
                                                    if (aiWin != null) finishMatch("LOSS", aiWin.second)
                                                    else if (afterAi.none { it.isEmpty() }) finishMatch("DRAW", emptyList())
                                                }
                                                aiThinking = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom Match Controls (Post-Match Rematch / Back to Lobby)
            if (matchResult != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SecondaryOutlineButton(
                        text = "Change Stake",
                        onClick = onExitToLobby,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryGradientButton(
                        text = "Play Again (${tier.entryCoins})",
                        isLoading = restarting,
                        onClick = {
                            val uid = profile?.uid ?: return@PrimaryGradientButton
                            if ((profile?.coins ?: 0L) < tier.entryCoins) {
                                viewModel.showToast("Not enough coins for rematch!", ToastType.ERROR)
                                return@PrimaryGradientButton
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
                                    celebrate = false
                                    board = List(9) { "" }
                                    winningLine = emptyList()
                                    matchResult = null
                                } else {
                                    viewModel.showToast(res.exceptionOrNull()?.message ?: "Rematch failed.", ToastType.ERROR)
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            } else {
                Text(
                    text = "Stake locked: ${tier.entryCoins} Coins • Balance: ${profile?.coins ?: 0} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        CelebrationBurstOverlay(visible = celebrate)
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.WarningAmber, null, tint = colors.amberAccent) },
            title = { Text("Leave Live Match?", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("Leaving mid-match forfeits your ${tier.entryCoins} coins entry stake. Are you sure?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showLeaveDialog = false; onExitToLobby() }) {
                    Text("Forfeit & Leave", color = colors.roseAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Keep Playing") }
            }
        )
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
    val scale by animateFloatAsState(if (active) 1.03f else 0.98f, spring(stiffness = Spring.StiffnessMediumLow), label = "turnScale")
    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = if (active) softBg else colors.card,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) accent else colors.cardBorder),
        shadowElevation = if (active) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(symbol, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, color = colors.textPrimary, maxLines = 1, fontSize = 14.sp)
                Text(if (active) "Playing..." else "Waiting", style = MaterialTheme.typography.labelSmall, color = if (active) accent else colors.textSecondary)
            }
        }
    }
}

@Composable
private fun AnimatedTicTacToeCell(
    symbol: String,
    isWinningCell: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    val drawProgress = remember(symbol) { Animatable(0f) }
    LaunchedEffect(symbol) {
        if (symbol.isNotEmpty()) {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        }
    }

    Surface(
        modifier = modifier.bounceClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = when {
            isWinningCell -> colors.mintSoft
            symbol == "X" -> colors.lavenderSoft
            symbol == "O" -> colors.amberSoft
            else -> colors.inputBg
        },
        border = BorderStroke(
            width = if (isWinningCell) 2.5.dp else 1.2.dp,
            color = when {
                isWinningCell -> colors.mintAccent
                symbol == "X" -> colors.primaryIndigo.copy(alpha = 0.4f)
                symbol == "O" -> colors.amberAccent.copy(alpha = 0.4f)
                else -> colors.cardBorder
            }
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(20.dp)) {
            if (symbol == "X") {
                val p = drawProgress.value
                val xBrush = Brush.linearGradient(listOf(colors.primaryIndigo, colors.primaryViolet))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    val p1 = (p * 2f).coerceIn(0f, 1f)
                    val p2 = ((p - 0.5f) * 2f).coerceIn(0f, 1f)
                    if (p1 > 0f) {
                        drawLine(
                            brush = xBrush,
                            start = Offset(0f, 0f),
                            end = Offset(size.width * p1, size.height * p1),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round
                        )
                    }
                    if (p2 > 0f) {
                        drawLine(
                            brush = xBrush,
                            start = Offset(size.width, 0f),
                            end = Offset(size.width * (1f - p2), size.height * p2),
                            strokeWidth = stroke.width,
                            cap = StrokeCap.Round
                        )
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
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
