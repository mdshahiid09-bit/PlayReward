package com.playreward.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.playreward.app.R
import com.playreward.app.data.model.AppConfig
import com.playreward.app.data.model.GithubReleaseInfo
import com.playreward.app.data.model.OnboardingPage
import com.playreward.app.data.model.StartupPopupConfig
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.update.DownloadProgressState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val colors = PRTheme.colors
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(1400)
        onFinished()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(colors.canvas),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "PlayReward",
                modifier = Modifier.size(96.dp).scale(scale.value)
            )
            Spacer(Modifier.height(20.dp))
            Text("PlayReward", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Complete tasks, play games, earn rewards", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                color = colors.primaryIndigo,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    adminPages: List<OnboardingPage>,
    onComplete: () -> Unit
) {
    val colors = PRTheme.colors
    val pages = remember(adminPages) {
        if (adminPages.isNotEmpty()) adminPages else listOf(
            OnboardingPage("1", "Complete Verified Tasks", "Discover top apps, follow simple step-by-step instructions, and collect real coin & INR rewards.", "", 0),
            OnboardingPage("2", "Play Quizzes & Games", "Answer quizzes to collect gems, crack number challenges, and challenge AI in 1v1 Tic-Tac-Toe.", "", 1),
            OnboardingPage("3", "Invite & Earn Together", "Share your unique referral code with friends and receive instant rewards when they withdraw.", "", 2),
            OnboardingPage("4", "Fast UPI & Bank Payouts", "Convert coins to INR anytime and withdraw securely to your saved UPI ID or bank account.", "", 3)
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val fallbackIcons = listOf(Icons.Rounded.TaskAlt, Icons.Rounded.SportsEsports, Icons.Rounded.CardGiftcard, Icons.Rounded.AccountBalanceWallet)

    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onComplete) {
                Text("Skip", color = colors.primaryIndigo, fontWeight = FontWeight.SemiBold)
            }
        }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { idx ->
            val page = pages[idx]
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (page.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = page.imageUrl,
                        contentDescription = page.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(220.dp).clip(RoundedCornerShape(28.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier.size(160.dp).clip(RoundedCornerShape(36.dp)).background(colors.heroGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = fallbackIcons[idx % fallbackIcons.size],
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
                Spacer(Modifier.height(36.dp))
                Text(page.title, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(page.subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, textAlign = TextAlign.Center)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(pages.size) { idx ->
                val active = pagerState.currentPage == idx
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (active) 28.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.primaryIndigo else colors.cardBorder)
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        val isLast = pagerState.currentPage == pages.lastIndex
        PrimaryGradientButton(
            text = if (isLast) "Get Started" else "Next",
            onClick = {
                if (isLast) onComplete()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MaintenanceScreen(
    config: AppConfig,
    currentUid: String?,
    onSaveBypassUid: (String) -> Unit
) {
    val colors = PRTheme.colors
    var uidInput by remember { mutableStateOf(currentUid ?: "") }
    var saving by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).background(colors.amberSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.BuildCircle, contentDescription = null, tint = colors.amberAccent, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(22.dp))
            Text(config.maintenanceTitle, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(config.maintenanceMessage, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))

            PRCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.AdminPanelSettings, colors.lavenderSoft, colors.primaryIndigo, size = 38.dp, cornerRadius = 10.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Admin Bypass Access", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Enter registered Admin UID to test live", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(14.dp))
                PRTextField(
                    value = uidInput,
                    onValueChange = { uidInput = it },
                    label = "Admin User UID",
                    placeholder = "Paste registered Firebase UID",
                    leadingIcon = Icons.Rounded.Key
                )
                Spacer(Modifier.height(14.dp))
                PrimaryGradientButton(
                    text = "Save Changes",
                    isLoading = saving,
                    enabled = uidInput.isNotBlank(),
                    onClick = {
                        saving = true
                        onSaveBypassUid(uidInput.trim())
                        saving = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun StartupImagePopupOverlay(
    config: StartupPopupConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var remaining by remember { mutableIntStateOf(config.closeDelaySeconds.coerceAtLeast(3)) }
    LaunchedEffect(config.imageUrl) {
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp).clip(RoundedCornerShape(24.dp))
                .bounceClick(enabled = config.actionUrl.isNotBlank()) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(config.actionUrl)))
                    } catch (_: Exception) {}
                }
        ) {
            AsyncImage(
                model = config.imageUrl,
                contentDescription = "Announcement",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 560.dp).clip(RoundedCornerShape(24.dp))
            )
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
        ) {
            if (remaining > 0) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.65f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))) {
                    Text(
                        text = "${remaining}s",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.White)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color(0xFF111827))
                }
            }
        }
    }
}

@Composable
fun ForcedUpdateScreen(
    release: GithubReleaseInfo,
    downloadState: DownloadProgressState,
    onStartDownload: () -> Unit
) {
    val colors = PRTheme.colors
    val pulse = rememberInfiniteTransition(label = "updatePulse")
    val iconScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.size(84.dp).scale(iconScale).clip(RoundedCornerShape(24.dp)).background(colors.heroGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
                Spacer(Modifier.height(18.dp))
                Surface(shape = CircleShape, color = colors.lavenderSoft) {
                    Text(
                        text = "New Version ${release.tagName}",
                        color = colors.primaryIndigo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Update Required", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = release.releaseNotes.ifBlank { "A new version of PlayReward is available. Update now to continue." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))

                when (downloadState) {
                    is DownloadProgressState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadState.percent / 100f },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                            color = colors.primaryIndigo,
                            trackColor = colors.lavenderSoft
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Downloading ${downloadState.percent}% (${String.format("%.1f", downloadState.downloadedMb)} / ${String.format("%.1f", downloadState.totalMb)} MB)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primaryIndigo
                        )
                    }
                    is DownloadProgressState.Completed -> {
                        Text("Download complete! Tap below if installer didn't open.", style = MaterialTheme.typography.bodyMedium, color = colors.mintAccent, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(14.dp))
                        PrimaryGradientButton(text = "Install Update", onClick = onStartDownload, modifier = Modifier.fillMaxWidth())
                    }
                    is DownloadProgressState.Failed -> {
                        Text(downloadState.reason, color = colors.roseAccent, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(14.dp))
                        PrimaryGradientButton(text = "Retry Download", onClick = onStartDownload, modifier = Modifier.fillMaxWidth())
                    }
                    DownloadProgressState.Idle -> {
                        PrimaryGradientButton(
                            text = "Download & Install Update",
                            leadingIcon = Icons.Rounded.Download,
                            onClick = onStartDownload,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
