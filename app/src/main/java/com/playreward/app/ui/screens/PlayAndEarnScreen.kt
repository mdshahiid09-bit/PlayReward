package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.playreward.app.R
import com.playreward.app.data.model.QuizCategory
import com.playreward.app.data.model.QuizQuestion
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayAndEarnScreen(
    viewModel: MainViewModel,
    onOpenCategory: (String, String) -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val quizSettings by viewModel.quizSettings.collectAsState()
    val categories by viewModel.quizCategories.collectAsState()

    var claiming by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    val currentGems = profile?.quizGems ?: 0
    val goal = quizSettings.gemGoal.coerceAtLeast(1)
    val progress = (currentGems.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val canClaim = currentGems >= goal

    Box(modifier = Modifier.fillMaxSize().background(colors.canvas)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.statusBarsPadding()) {
                    Text("Play & Earn", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    Text("Answer quizzes, explore to collect gems, and claim coins", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }

            // Gem Goal Progress Card
            item {
                PRCard(cornerRadius = 22.dp, contentPadding = PaddingValues(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(colors.lavenderSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(painterResource(R.drawable.ic_gem), contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quiz Gem Goal", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Collect $goal gems to claim ${quizSettings.rewardCoins} coins", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        }
                        Surface(shape = CircleShape, color = colors.lavenderSoft) {
                            Text(
                                text = "$currentGems / $goal",
                                color = colors.primaryIndigo,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = colors.primaryIndigo,
                        trackColor = colors.lavenderSoft
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryGradientButton(
                        text = if (canClaim) "Claim ${quizSettings.rewardCoins} Coins" else "Collect ${goal - currentGems} More Gems",
                        enabled = canClaim,
                        isLoading = claiming,
                        leadingIcon = Icons.Rounded.Celebration,
                        onClick = {
                            val uid = profile?.uid ?: return@PrimaryGradientButton
                            scope.launch {
                                claiming = true
                                val res = viewModel.repo.claimGemGoal(
                                    uid = uid,
                                    gemField = "quizGems",
                                    goal = goal,
                                    rewardCoins = quizSettings.rewardCoins,
                                    txType = "QUIZ_GEM",
                                    txTitle = "Quiz Gem Goal Reward ($goal Gems)"
                                )
                                claiming = false
                                if (res.isSuccess) {
                                    showCelebration = true
                                    viewModel.showToast("+${quizSettings.rewardCoins} coins credited to your wallet!", ToastType.SUCCESS)
                                    delay(2600)
                                    showCelebration = false
                                } else {
                                    viewModel.showToast(res.exceptionOrNull()?.message ?: "Claim failed.", ToastType.ERROR)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Text("Quiz Categories", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }

            if (!quizSettings.enabled) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.PauseCircle,
                        title = "Quizzes Temporarily Paused",
                        subtitle = "The admin has paused Play & Earn quizzes. Please check back later."
                    )
                }
            } else if (categories.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.Quiz,
                        title = "No Quiz Categories Yet",
                        subtitle = "Categories and quizzes added from the admin panel will appear here automatically."
                    )
                }
            } else {
                items(categories, key = { it.id }) { cat ->
                    QuizCategoryCard(category = cat, onClick = { onOpenCategory(cat.id, cat.name) })
                }
            }
        }

        CelebrationBurstOverlay(visible = showCelebration)
    }
}

@Composable
private fun QuizCategoryCard(category: QuizCategory, onClick: () -> Unit) {
    val colors = PRTheme.colors
    PRCard(onClick = onClick, cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (category.iconUrl.isNotBlank()) {
                AsyncImage(
                    model = category.iconUrl,
                    contentDescription = category.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp))
                )
            } else {
                IconBadge(Icons.Rounded.Psychology, colors.lavenderSoft, colors.primaryIndigo, size = 50.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                if (category.description.isNotBlank()) {
                    Text(category.description, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 2)
                }
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.primaryIndigo)
        }
    }
}

@Composable
fun QuizPlayScreen(
    categoryId: String,
    categoryName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val profile by viewModel.userProfile.collectAsState()
    val settings by viewModel.quizSettings.collectAsState()
    val questions by remember(categoryId) { viewModel.repo.observeQuizzesForCategory(categoryId) }
        .collectAsState(initial = emptyList())

    var currentIndex by remember { mutableIntStateOf(0) }
    var showHint by remember { mutableStateOf(false) }
    var showClaimDialog by remember { mutableStateOf(false) }
    var waitingWebReturn by remember { mutableStateOf(false) }
    var webOpenedAtMs by remember { mutableLongStateOf(0L) }
    var earlyReturnWarning by remember { mutableStateOf<String?>(null) }

    // Lifecycle observer to enforce required website explore time (e.g. 15 seconds)
    DisposableEffect(lifecycleOwner, waitingWebReturn, webOpenedAtMs) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingWebReturn) {
                waitingWebReturn = false
                val elapsedSec = ((System.currentTimeMillis() - webOpenedAtMs) / 1000L).toInt()
                val reqSec = settings.requiredExploreSeconds.coerceAtLeast(1)
                if (elapsedSec >= reqSec) {
                    val uid = profile?.uid
                    if (uid != null) {
                        scope.launch {
                            viewModel.repo.addGems(uid, "quizGems", settings.gemsPerCorrect)
                            viewModel.showToast("+${settings.gemsPerCorrect} Gem earned! Explore verified (${elapsedSec}s).", ToastType.SUCCESS)
                            if (questions.isNotEmpty()) currentIndex = (currentIndex + 1) % questions.size
                            showHint = false
                        }
                    }
                } else {
                    earlyReturnWarning = "You returned after ${elapsedSec}s. Please explore the website for at least ${reqSec} seconds to earn your gem."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding()) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
            }
            Text(categoryName, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = colors.lavenderSoft) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_gem), null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${profile?.quizGems ?: 0}/${settings.gemGoal}", color = colors.primaryIndigo, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (questions.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.Quiz,
                title = "No Quizzes in $categoryName",
                subtitle = "Quizzes published in this category from the admin panel will appear here.",
                actionLabel = "Back to Categories",
                onAction = onBack
            )
        } else {
            val q = questions[currentIndex % questions.size]
            val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD)
            val labels = listOf("A", "B", "C", "D")

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Soft Lavender Question Card (Matching Reference Video 01:13)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.lavenderSoft,
                    border = BorderStroke(1.dp, colors.primaryIndigo.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(categoryName.uppercase(), color = colors.primaryIndigo, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(q.question, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                    }
                }

                if (q.hint.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            modifier = Modifier.bounceClick { showHint = !showHint },
                            shape = CircleShape,
                            color = colors.amberSoft,
                            border = BorderStroke(1.dp, colors.amberAccent.copy(alpha = 0.45f))
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lightbulb, null, tint = colors.amberAccent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Use Hint", color = colors.amberAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                    AnimatedVisibility(showHint) {
                        PRCard(contentPadding = PaddingValues(12.dp)) {
                            Text("Hint: ${q.hint}", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                    }
                }

                options.forEachIndexed { idx, opt ->
                    PRCard(
                        onClick = {
                            if (idx == q.correctIndex) {
                                showClaimDialog = true
                            } else {
                                viewModel.showToast("Incorrect answer — try again!", ToastType.ERROR)
                            }
                        },
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.inputBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(labels[idx], fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(opt, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        }
                    }
                }
            }
        }
    }

    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            containerColor = colors.card,
            title = { Text("Correct Answer!", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    "Tap Claim Gem to open the partner website. Explore it for at least ${settings.requiredExploreSeconds} seconds, then return to collect +${settings.gemsPerCorrect} gem.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                PrimaryGradientButton(
                    text = "Claim Gem",
                    height = 44.dp,
                    onClick = {
                        showClaimDialog = false
                        val url = settings.websiteUrl.ifBlank { "https://playreward.app" }
                        webOpenedAtMs = System.currentTimeMillis()
                        waitingWebReturn = true
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {
                            waitingWebReturn = false
                            viewModel.showToast("Unable to open browser.", ToastType.ERROR)
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) { Text("Cancel") }
            }
        )
    }

    earlyReturnWarning?.let { msg ->
        AlertDialog(
            onDismissRequest = { earlyReturnWarning = null },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.TimerOff, null, tint = colors.roseAccent) },
            title = { Text("Returned Too Early", color = colors.textPrimary) },
            text = { Text(msg, color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = { earlyReturnWarning = null }) { Text("Got It") }
            }
        )
    }
}
