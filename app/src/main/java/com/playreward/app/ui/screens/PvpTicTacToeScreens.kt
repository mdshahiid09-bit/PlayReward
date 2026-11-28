package com.playreward.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.playreward.app.R
import com.playreward.app.data.model.TicTacToeTier
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// ============================================================================
// DEDICATED REAL-PLAYER 1v1 MATCHMAKING RADAR SCREEN (ZERO BOTS!)
// ============================================================================

@Composable
fun PvpMatchmakingScreen(
    tierId: String,
    viewModel: MainViewModel,
    onCancel: () -> Unit,
    onMatchFound: (String) -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val config by viewModel.superOfferConfig.collectAsState()

    val tier = remember(config.tiers, tierId) {
        val list = if (config.tiers.isNotEmpty()) config.tiers else listOf(
            TicTacToeTier("low_10", "LOW", 10, 12, "Starter"),
            TicTacToeTier("med_10", "MEDIUM", 10, 15, "Pro"),
            TicTacToeTier("high_10", "HIGH", 10, 18, "Master"),
            TicTacToeTier("high_100", "HIGH", 100, 180, "Champion"),
            TicTacToeTier("high_500", "HIGH", 500, 950, "Legend")
        )
        list.find { it.id == tierId } ?: list.first()
    }

    val uid = profile?.uid.orEmpty()
    val fullName = profile?.fullName.orEmpty()
    val matchedRoomFromQueue by remember(uid) {
        if (uid.isNotBlank()) viewModel.repo.observeQueueMatchRoom(uid) else kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)

    var searchSeconds by remember { mutableIntStateOf(0) }
    var timedOut by remember { mutableStateOf(false) }
    var matchTriggered by remember { mutableStateOf(false) }
    var searchAttempt by remember { mutableIntStateOf(0) }

    // Join or match real player queue
    LaunchedEffect(uid, tier.id, searchAttempt) {
        if (uid.isBlank()) return@LaunchedEffect
        timedOut = false
        searchSeconds = 0
        val res = viewModel.repo.findOrJoinPvpQueue(uid, fullName, tier)
        val instantRoom = res.getOrNull()
        if (!instantRoom.isNullOrBlank() && !matchTriggered) {
            matchTriggered = true
            viewModel.soundFx.playSuccess()
            onMatchFound(instantRoom)
        } else if (res.isFailure) {
            viewModel.showToast(res.exceptionOrNull()?.message ?: "Matchmaking error.", ToastType.ERROR)
        }
    }

    // Listen for another real player claiming us from the queue
    LaunchedEffect(matchedRoomFromQueue) {
        val rId = matchedRoomFromQueue
        if (!rId.isNullOrBlank() && !matchTriggered) {
            matchTriggered = true
            viewModel.soundFx.playSuccess()
            onMatchFound(rId)
        }
    }

    // Elapsed timer (60s search timeout — NEVER spawns a fake bot!)
    LaunchedEffect(searchAttempt, matchTriggered) {
        while (!matchTriggered && searchSeconds < 60) {
            delay(1000)
            searchSeconds++
        }
        if (!matchTriggered && searchSeconds >= 60) {
            timedOut = true
            if (uid.isNotBlank()) viewModel.repo.leavePvpQueue(uid, tier.id)
        }
    }

    DisposableEffect(uid, tier.id) {
        onDispose {
            if (!matchTriggered && uid.isNotBlank()) {
                scope.launch { viewModel.repo.leavePvpQueue(uid, tier.id) }
            }
        }
    }

    fun cancelAndExit() {
        scope.launch {
            if (uid.isNotBlank()) viewModel.repo.leavePvpQueue(uid, tier.id)
            onCancel()
        }
    }

    BackHandler { cancelAndExit() }

    val radar = rememberInfiniteTransition(label = "pvpRadar")
    val ring1 by radar.animateFloat(0.35f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart), label = "r1")
    val ring2 by radar.animateFloat(0.1f, 1f, infiniteRepeatable(tween(1500, 750, LinearEasing), RepeatMode.Restart), label = "r2")

    Box(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Surface(shape = CircleShape, color = colors.lavenderSoft) {
                Text(
                    text = "REAL 1v1 MULTIPLAYER • ${tier.label}",
                    color = colors.primaryIndigo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // Animated Radar Scanner
            Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                if (!timedOut) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val maxR = size.minDimension / 2f
                        drawCircle(colors.primaryIndigo.copy(alpha = (1f - ring1) * 0.45f), maxR * ring1, c, style = Stroke(3.dp.toPx()))
                        drawCircle(colors.primaryViolet.copy(alpha = (1f - ring2) * 0.45f), maxR * ring2, c, style = Stroke(3.dp.toPx()))
                    }
                }
                Box(
                    modifier = Modifier.size(84.dp).clip(CircleShape).background(colors.heroGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (timedOut) Icons.Rounded.PersonSearch else Icons.Rounded.Groups,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = if (timedOut) "No Opponent Found Yet" else "Searching for Real Player...",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (timedOut)
                    "No other real player joined the ${tier.label} table right now. Zero bots are used — tap Retry to search again."
                else
                    "Matching you live with a real player on the ${tier.label} table (Entry: ${tier.entryCoins} • Win: ${tier.winCoins} Coins). Entry coins are deducted only when the match starts.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))
            if (!timedOut) {
                Surface(shape = CircleShape, color = colors.card, border = BorderStroke(1.dp, colors.cardBorder)) {
                    Text(
                        text = "Elapsed: 00:${searchSeconds.toString().padStart(2, '0')}",
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryIndigo,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }

            Spacer(Modifier.height(30.dp))
            if (timedOut) {
                PrimaryGradientButton(
                    text = "Retry Search",
                    leadingIcon = Icons.Rounded.Refresh,
                    onClick = { searchAttempt++ },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
            }
            SecondaryOutlineButton(
                text = "Cancel & Back to Arena",
                onClick = { cancelAndExit() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// DEDICATED REAL-TIME 1v1 MULTIPLAYER MATCH SCREEN (ZERO BOTS!)
// ============================================================================

@Composable
fun PvpMatchScreen(
    roomId: String,
    viewModel: MainViewModel,
    onExitToLobby: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val room by remember(roomId) { viewModel.repo.observePvpRoom(roomId) }.collectAsState(initial = null)

    val myUid = profile?.uid.orEmpty()
    val mySymbol = remember(room, myUid) {
        when {
            room?.playerXUid == myUid -> "X"
            room?.playerOUid == myUid -> "O"
            else -> "X"
        }
    }
    val oppSymbol = if (mySymbol == "X") "O" else "X"
    val myName = if (mySymbol == "X") room?.playerXName ?: "You" else room?.playerOName ?: "You"
    val oppName = if (mySymbol == "X") room?.playerOName ?: "Opponent" else room?.playerXName ?: "Opponent"

    var entryDeducted by remember(roomId) { mutableStateOf(false) }
    var payoutHandled by remember(roomId) { mutableStateOf(false) }
    var showResultPopup by remember(roomId) { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }
    var turnSecondsLeft by remember { mutableIntStateOf(15) }
    val strikeProgress = remember { Animatable(0f) }

    // Deduct entry stake atomically once when the real match room loads
    LaunchedEffect(room?.roomId, myUid) {
        val r = room ?: return@LaunchedEffect
        if (!entryDeducted && myUid.isNotBlank() && r.entryCoins > 0L) {
            entryDeducted = true
            viewModel.repo.adjustCoins(
                uid = myUid,
                deltaCoins = -r.entryCoins,
                countAsEarned = false,
                txType = "TIC_TAC_TOE_ENTRY",
                txTitle = "1v1 PvP Entry (${r.tierLabel})"
            )
        }
    }

    val isLive = room?.status == "PLAYING"
    val isMyTurn = isLive && room?.turn == mySymbol

    val myOutcome: String? = remember(room?.status, mySymbol) {
        when (room?.status) {
            "X_WIN" -> if (mySymbol == "X") "WIN" else "LOSS"
            "O_WIN" -> if (mySymbol == "O") "WIN" else "LOSS"
            "FORFEIT_X" -> if (mySymbol == "X") "LOSS" else "WIN"
            "FORFEIT_O" -> if (mySymbol == "O") "LOSS" else "WIN"
            "DRAW" -> "DRAW"
            else -> null
        }
    }

    // Handle real-time synced 15s turn timer
    LaunchedEffect(room?.turn, room?.lastMoveAt, isLive) {
        if (isLive) {
            turnSecondsLeft = 15
            while (turnSecondsLeft > 0 && isLive) {
                delay(1000)
                turnSecondsLeft--
            }
            if (turnSecondsLeft == 0 && isLive) {
                if (isMyTurn) {
                    viewModel.repo.forfeitPvpRoom(roomId, mySymbol)
                } else {
                    viewModel.repo.claimPvpTimeoutWin(roomId, oppSymbol)
                }
            }
        }
    }

    // Handle Victory / Draw / Loss animations & atomic wallet payout
    LaunchedEffect(myOutcome) {
        val outcome = myOutcome ?: return@LaunchedEffect
        val r = room ?: return@LaunchedEffect
        if (r.winningLine.size == 3) {
            strikeProgress.snapTo(0f)
            strikeProgress.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        if (!payoutHandled && myUid.isNotBlank()) {
            payoutHandled = true
            when (outcome) {
                "WIN" -> {
                    viewModel.soundFx.playCelebration()
                    celebrate = true
                    viewModel.repo.adjustCoins(myUid, r.winCoins, true, "TIC_TAC_TOE_WIN", "1v1 PvP Win (${r.tierLabel})")
                }
                "DRAW" -> {
                    viewModel.soundFx.playClick()
                    if (r.entryCoins > 0L) {
                        viewModel.repo.adjustCoins(myUid, r.entryCoins, false, "TIC_TAC_TOE_WIN", "1v1 PvP Draw Refund")
                    }
                }
                "LOSS" -> viewModel.soundFx.playError()
            }
        }
        delay(320)
        showResultPopup = true
    }

    BackHandler(enabled = isLive) { showLeaveDialog = true }

    if (room == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.canvas), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.primaryIndigo)
        }
        return
    }

    val liveRoom = room!!

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
                    modifier = Modifier.size(36.dp).bounceClick { if (isLive) showLeaveDialog = true else onExitToLobby() },
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
                        text = "LIVE 1v1 • ${liveRoom.tierLabel}",
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
                        Text("Win +${liveRoom.winCoins}", color = colors.mintAccent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Real Player VS Real Player Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PvpPlayerChip(
                    name = myName.substringBefore(" "),
                    symbol = mySymbol,
                    active = isMyTurn,
                    accent = colors.primaryIndigo,
                    softBg = colors.lavenderSoft,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = CircleShape, color = colors.card, border = BorderStroke(1.dp, colors.cardBorder)) {
                    Text("VS", fontWeight = FontWeight.ExtraBold, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
                PvpPlayerChip(
                    name = oppName.substringBefore(" "),
                    symbol = oppSymbol,
                    active = isLive && !isMyTurn,
                    accent = colors.amberAccent,
                    softBg = colors.amberSoft,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Live Turn Timer Bar
            if (isLive) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = if (isMyTurn) "Your Turn ($mySymbol) — Tap a square" else "${oppName.substringBefore(" ")}'s Turn ($oppSymbol)...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMyTurn) colors.primaryIndigo else colors.amberAccent
                        )
                        Text(
                            text = "${turnSecondsLeft}s",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (turnSecondsLeft <= 5) colors.roseAccent else colors.primaryIndigo
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    LinearProgressIndicator(
                        progress = { (turnSecondsLeft / 15f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = if (turnSecondsLeft <= 5) colors.roseAccent else if (isMyTurn) colors.primaryIndigo else colors.amberAccent,
                        trackColor = colors.inputBg
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Real-Time Synced 3x3 Board with Particle Burst + Strike-Through Line
            PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        for (r in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                                for (c in 0..2) {
                                    val idx = r * 3 + c
                                    val cell = liveRoom.board[idx]
                                    val isWinCell = idx in liveRoom.winningLine
                                    PvpAnimatedCell(
                                        symbol = cell,
                                        isWinningCell = isWinCell,
                                        isRecentMove = idx == liveRoom.lastMoveIndex && isLive,
                                        enabled = isMyTurn && cell.isEmpty(),
                                        onClick = {
                                            viewModel.soundFx.playClick()
                                            scope.launch { viewModel.repo.makePvpMove(roomId, idx, mySymbol) }
                                        },
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    )
                                }
                            }
                        }
                    }

                    if (liveRoom.winningLine.size == 3 && strikeProgress.value > 0f) {
                        val lineBrush = if (myOutcome == "WIN") {
                            Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cw = size.width / 3f
                            val ch = size.height / 3f
                            val s = liveRoom.winningLine.first()
                            val e = liveRoom.winningLine.last()
                            val sx = (s % 3) * cw + cw / 2f
                            val sy = (s / 3) * ch + ch / 2f
                            val ex = (e % 3) * cw + cw / 2f
                            val ey = (e / 3) * ch + ch / 2f
                            val p = strikeProgress.value
                            drawLine(lineBrush, Offset(sx, sy), Offset(sx + (ex - sx) * p, sy + (ey - sy) * p), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text("Real 1v1 Match • Entry: ${liveRoom.entryCoins} Coins • Prize: ${liveRoom.winCoins} Coins", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
        }

        CelebrationBurstOverlay(visible = celebrate)
    }

    // Celebration Result Popup Dialog (Victory / Draw / Defeat)
    if (showResultPopup && myOutcome != null) {
        val isWin = myOutcome == "WIN"
        val isDraw = myOutcome == "DRAW"
        Dialog(onDismissRequest = { showResultPopup = false }, properties = DialogProperties(dismissOnClickOutside = false)) {
            PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp, contentPadding = PaddingValues(22.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(76.dp).clip(CircleShape).background(
                            when {
                                isWin -> colors.amberSoft
                                isDraw -> colors.lavenderSoft
                                else -> colors.roseSoft
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isWin) Image(painterResource(R.drawable.ic_pr_trophy), null, modifier = Modifier.size(46.dp))
                        else if (isDraw) Icon(Icons.Rounded.Handshake, null, tint = colors.primaryIndigo, modifier = Modifier.size(40.dp))
                        else Icon(Icons.Rounded.SentimentDissatisfied, null, tint = colors.roseAccent, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = when {
                            isWin -> "YOU WON!"
                            isDraw -> "MATCH DRAW!"
                            else -> "OPPONENT WON!"
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
                            isWin -> "You defeated $oppName in a live 1v1 match!"
                            isDraw -> "Well played! Your ${liveRoom.entryCoins} coins entry stake has been refunded."
                            else -> "$oppName won this round. Ready for another real opponent?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isWin) colors.mintSoft else if (isDraw) colors.lavenderSoft else colors.inputBg
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painterResource(R.drawable.ic_coin), null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isWin -> "+${liveRoom.winCoins} Coins Credited!"
                                    isDraw -> "+${liveRoom.entryCoins} Coins Refunded"
                                    else -> "0 Coins Won"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isWin) colors.mintAccent else if (isDraw) colors.primaryIndigo else colors.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    PrimaryGradientButton(
                        text = "OK, Back to Arena",
                        gradient = if (isWin) colors.megaGradient else colors.heroGradient,
                        onClick = { showResultPopup = false; onExitToLobby() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.WarningAmber, null, tint = colors.amberAccent) },
            title = { Text("Forfeit Live 1v1 Match?", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = { Text("Leaving mid-match forfeits your ${liveRoom.entryCoins} coins stake and awards the win to $oppName.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    scope.launch {
                        viewModel.repo.forfeitPvpRoom(roomId, mySymbol)
                        onExitToLobby()
                    }
                }) { Text("Forfeit & Leave", color = colors.roseAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Keep Playing") } }
        )
    }
}

@Composable
private fun PvpPlayerChip(name: String, symbol: String, active: Boolean, accent: Color, softBg: Color, modifier: Modifier) {
    val colors = PRTheme.colors
    val pulse = rememberInfiniteTransition(label = "pvpTurn")
    val pv by pulse.animateFloat(1f, 1.035f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pv")
    val scale by animateFloatAsState(if (active) pv else 0.97f, spring(stiffness = Spring.StiffnessMediumLow), label = "sc")
    Surface(
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(15.dp),
        color = if (active) softBg else colors.card,
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) accent else colors.cardBorder)
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
private fun PvpAnimatedCell(
    symbol: String,
    isWinningCell: Boolean,
    isRecentMove: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val colors = PRTheme.colors
    val drawP = remember(symbol) { Animatable(0f) }
    val burstP = remember(symbol) { Animatable(0f) }
    val popS = remember(symbol) { Animatable(1f) }

    LaunchedEffect(symbol) {
        if (symbol.isNotEmpty()) {
            drawP.snapTo(0f); burstP.snapTo(0f); popS.snapTo(0.84f)
            launch { popS.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) }
            launch { drawP.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
            launch { burstP.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        }
    }
    val accent = if (symbol == "X") colors.primaryIndigo else Color(0xFFF97316)

    Surface(
        modifier = modifier.scale(popS.value).bounceClick(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = when {
            isWinningCell -> colors.mintSoft
            symbol == "X" -> colors.lavenderSoft
            symbol == "O" -> colors.amberSoft
            else -> colors.inputBg
        },
        border = BorderStroke(
            if (isWinningCell || isRecentMove) 2.5.dp else 1.2.dp,
            when {
                isWinningCell -> colors.mintAccent
                isRecentMove -> colors.amberAccent
                symbol == "X" -> colors.primaryIndigo.copy(alpha = 0.45f)
                symbol == "O" -> colors.amberAccent.copy(alpha = 0.45f)
                else -> colors.cardBorder
            }
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (symbol.isNotEmpty() && burstP.value < 0.99f) {
                val bp = burstP.value
                val a = (1f - bp).coerceIn(0f, 1f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f; val cy = size.height / 2f; val maxR = size.minDimension * 0.48f
                    drawCircle(accent.copy(alpha = a * 0.45f), maxR * bp, Offset(cx, cy), style = Stroke((3.5f * a).dp.toPx()))
                    for (i in 0 until 10) {
                        val ang = (i * 36.0) * (Math.PI / 180.0)
                        val d = maxR * 0.3f + maxR * 0.7f * bp
                        drawCircle(if (i % 2 == 0) accent.copy(alpha = a) else Color(0xFFFBBF24).copy(alpha = a), (3f * a + 1f).dp.toPx(), Offset(cx + (cos(ang) * d).toFloat(), cy + (sin(ang) * d).toFloat()))
                    }
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(18.dp)) {
                if (symbol == "X") {
                    val p = drawP.value
                    val xB = Brush.linearGradient(listOf(colors.primaryIndigo, colors.primaryViolet))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val sw = 8.5.dp.toPx()
                        val p1 = (p * 2f).coerceIn(0f, 1f); val p2 = ((p - 0.5f) * 2f).coerceIn(0f, 1f)
                        if (p1 > 0f) drawLine(xB, Offset.Zero, Offset(size.width * p1, size.height * p1), sw, StrokeCap.Round)
                        if (p2 > 0f) drawLine(xB, Offset(size.width, 0f), Offset(size.width * (1f - p2), size.height * p2), sw, StrokeCap.Round)
                    }
                } else if (symbol == "O") {
                    val p = drawP.value
                    val oB = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFF97316)))
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(oB, -90f, 360f * p, false, Offset.Zero, Size(size.width, size.height), style = Stroke(8.5.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }
        }
    }
}
