package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
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
            // Step Dots (Matching Reference Video 01:22)
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
            // Soft Lavender Target Number Box
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

            // 2x2 Choice Grid
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

// --- Super Offer: 1v1 AI Tic-Tac-Toe Arena ---

@Composable
fun SuperOfferTicTacToeScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.superOfferConfig.collectAsState()

    val tiers = remember(config.tiers) {
        if (config.tiers.isNotEmpty()) config.tiers else listOf(
            TicTacToeTier("low_10", "LOW", 10, 12, "Starter"),
            TicTacToeTier("med_10", "MEDIUM", 10, 15, "Pro"),
            TicTacToeTier("high_10", "HIGH", 10, 18, "Master"),
            TicTacToeTier("high_100", "HIGH", 100, 180, "Champion"),
            TicTacToeTier("high_500", "HIGH", 500, 950, "Legend")
        )
    }

    var selectedTier by remember(tiers) { mutableStateOf(tiers.first()) }
    var board by remember { mutableStateOf(List(9) { "" }) }
    var inMatch by remember { mutableStateOf(false) }
    var aiThinking by remember { mutableStateOf(false) }
    var startingMatch by remember { mutableStateOf(false) }
    var matchResult by remember { mutableStateOf<String?>(null) } // WIN, LOSS, DRAW
    var winningLine by remember { mutableStateOf<List<Int>>(emptyList()) }
    var celebrate by remember { mutableStateOf(false) }

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
        if (difficulty == "MEDIUM" && Random.nextFloat() < 0.30f) return empty.random()
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
        inMatch = false
        val uid = profile?.uid ?: return
        scope.launch {
            when (result) {
                "WIN" -> {
                    viewModel.repo.adjustCoins(uid, selectedTier.winCoins, true, "TIC_TAC_TOE_WIN", "Tic-Tac-Toe Win (${selectedTier.label})")
                    celebrate = true
                    viewModel.showToast("Victory! +${selectedTier.winCoins} coins credited!", ToastType.SUCCESS)
                    delay(2500)
                    celebrate = false
                }
                "DRAW" -> {
                    viewModel.repo.adjustCoins(uid, selectedTier.entryCoins, false, "TIC_TAC_TOE_WIN", "Tic-Tac-Toe Draw Refund")
                    viewModel.showToast("Draw! Entry ${selectedTier.entryCoins} coins refunded.", ToastType.INFO)
                }
                "LOSS" -> {
                    viewModel.showToast("AI won this round. Try again!", ToastType.ERROR)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                    Text("Super Offer", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Surface(shape = CircleShape, color = colors.amberSoft) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${profile?.coins ?: 0}", color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Game Header Banner
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(colors.superGradient).padding(18.dp)
                ) {
                    Column {
                        Text(config.title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = config.termsAndConditions.ifBlank {
                                "Pick a difficulty & coin stake. Beat the AI to win multiplied coins! Losing awards 0 coins; draws refund your entry."
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Tier Selection (Disabled while match active)
            item {
                Text("Select Stake & Difficulty", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tiers.forEach { tier ->
                        val isSelected = selectedTier.id == tier.id
                        Surface(
                            modifier = Modifier.fillMaxWidth().bounceClick(enabled = !inMatch) { selectedTier = tier },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colors.lavenderSoft else colors.card,
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) colors.primaryIndigo else colors.cardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${tier.label} • ${tier.difficulty}", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                    Text("Entry: ${tier.entryCoins} coins", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                                Surface(shape = CircleShape, color = colors.mintSoft) {
                                    Text(
                                        text = "Win ${tier.winCoins}",
                                        color = colors.mintAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive 3x3 Board
            item {
                PRCard(cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                !inMatch && matchResult == null -> "Ready to Play (You: X vs AI: O)"
                                aiThinking -> "AI is thinking..."
                                inMatch -> "Your Turn (X)"
                                matchResult == "WIN" -> "You Won +${selectedTier.winCoins} Coins!"
                                matchResult == "DRAW" -> "Match Draw — Stake Refunded"
                                else -> "AI Won This Round"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = when (matchResult) {
                                "WIN" -> colors.mintAccent
                                "LOSS" -> colors.roseAccent
                                else -> colors.primaryIndigo
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (r in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                for (c in 0..2) {
                                    val idx = r * 3 + c
                                    val cell = board[idx]
                                    val isWinningCell = idx in winningLine
                                    val cellScale by animateFloatAsState(if (cell.isNotEmpty()) 1f else 0.94f, label = "cellScale")
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .scale(cellScale)
                                            .bounceClick(enabled = inMatch && !aiThinking && cell.isEmpty()) {
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
                                                        delay(420)
                                                        val aiMove = pickAiMove(updated, selectedTier.difficulty)
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
                                        shape = RoundedCornerShape(18.dp),
                                        color = when {
                                            isWinningCell -> colors.mintSoft
                                            cell == "X" -> colors.lavenderSoft
                                            cell == "O" -> colors.amberSoft
                                            else -> colors.inputBg
                                        },
                                        border = BorderStroke(
                                            width = if (isWinningCell) 2.dp else 1.dp,
                                            color = if (isWinningCell) colors.mintAccent else colors.cardBorder
                                        )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = cell,
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (cell == "X") colors.primaryIndigo else colors.amberAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    PrimaryGradientButton(
                        text = if (inMatch) "Match in Progress..." else "Start Match (${selectedTier.entryCoins} Coins)",
                        enabled = !inMatch,
                        isLoading = startingMatch,
                        leadingIcon = Icons.Rounded.SportsEsports,
                        onClick = {
                            val uid = profile?.uid ?: return@PrimaryGradientButton
                            if ((profile?.coins ?: 0L) < selectedTier.entryCoins) {
                                viewModel.showToast("Not enough coins! Need ${selectedTier.entryCoins} coins.", ToastType.ERROR)
                                return@PrimaryGradientButton
                            }
                            scope.launch {
                                startingMatch = true
                                val res = viewModel.repo.adjustCoins(
                                    uid = uid,
                                    deltaCoins = -selectedTier.entryCoins,
                                    countAsEarned = false,
                                    txType = "TIC_TAC_TOE_ENTRY",
                                    txTitle = "Tic-Tac-Toe Entry (${selectedTier.label})"
                                )
                                startingMatch = false
                                if (res.isSuccess) {
                                    board = List(9) { "" }
                                    winningLine = emptyList()
                                    matchResult = null
                                    inMatch = true
                                } else {
                                    viewModel.showToast(res.exceptionOrNull()?.message ?: "Could not start match.", ToastType.ERROR)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        CelebrationBurstOverlay(visible = celebrate)
    }
}
