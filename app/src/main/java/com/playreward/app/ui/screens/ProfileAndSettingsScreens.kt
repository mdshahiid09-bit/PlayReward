package com.playreward.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.theme.ThemeMode
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()
    val supportChannels by viewModel.supportChannels.collectAsState()

    var showSupportSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var verifying by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Summary Card with Small Detail Chips + Email Verified Status
        item {
            PRCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(colors.heroGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (profile?.fullName?.firstOrNull() ?: 'P').uppercase(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.fullName?.ifBlank { "PlayReward User" } ?: "PlayReward User", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        Text(profile?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        val verified = profile?.emailVerified == true
                        Surface(
                            modifier = Modifier.bounceClick(enabled = !verified && !verifying) {
                                scope.launch {
                                    verifying = true
                                    val r = viewModel.repo.sendVerificationEmail()
                                    verifying = false
                                    if (r.isSuccess) viewModel.showToast("Verification email sent! Tap Refresh after verifying.", ToastType.SUCCESS)
                                    else viewModel.showToast(r.exceptionOrNull()?.message ?: "Failed to send email.", ToastType.ERROR)
                                }
                            },
                            shape = CircleShape,
                            color = if (verified) colors.mintSoft else colors.amberSoft
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (verified) Icons.Rounded.Verified else Icons.Rounded.MarkEmailUnread, null, tint = if (verified) colors.mintAccent else colors.amberAccent, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (verified) "Email Verified" else "Verify Email Now",
                                    color = if (verified) colors.mintAccent else Color(0xFFB45309),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val res = viewModel.repo.refreshEmailVerified()
                            if (res.getOrDefault(false)) viewModel.showToast("Email verified!", ToastType.SUCCESS)
                            else viewModel.showToast("Email not verified yet.", ToastType.INFO)
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, "Refresh verification", tint = colors.primaryIndigo)
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileDetailPill("Gender", profile?.gender?.ifBlank { "Not set" } ?: "Not set", Modifier.weight(1f))
                    ProfileDetailPill("Age", if ((profile?.age ?: 0) > 0) "${profile?.age} yrs" else "Not set", Modifier.weight(1f))
                }
            }
        }

        // Big Professional Statistics Card
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Account Statistics", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileStatTile("Tasks Completed", "${profile?.totalTasksCompleted ?: 0}", Icons.Rounded.TaskAlt, colors.primaryIndigo, colors.lavenderSoft, Modifier.weight(1f))
                    ProfileStatTile("Total Coins Earned", "${profile?.totalCoinsEarned ?: 0}", Icons.Rounded.MonetizationOn, colors.amberAccent, colors.amberSoft, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileStatTile("Total Withdrawn", "₹${String.format("%.2f", profile?.totalWithdrawnInr ?: 0.0)}", Icons.Rounded.AccountBalanceWallet, colors.mintAccent, colors.mintSoft, Modifier.weight(1f))
                    ProfileStatTile("Total Referrals", "${profile?.totalReferrals ?: 0}", Icons.Rounded.Groups, colors.primaryViolet, colors.lavenderSoft, Modifier.weight(1f))
                }
            }
        }

        // Grouped Settings & Profile Actions Card (Reference Video 00:58)
        item {
            PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                ProfileMenuRow(Icons.Rounded.Edit, "Edit Profile", colors.lavenderSoft, colors.primaryIndigo) { onNavigate("edit_profile") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.Settings, "Settings (Password, Theme, Delete)", colors.amberSoft, colors.amberAccent) { onNavigate("settings") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.SupportAgent, "Contact Support", colors.mintSoft, colors.mintAccent) { showSupportSheet = true }
            }
        }

        // Legal Pages Card
        item {
            PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                ProfileMenuRow(Icons.Rounded.Info, "About PlayReward", colors.inputBg, colors.textSecondary) { onNavigate("legal/about") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.Description, "Terms & Conditions", colors.inputBg, colors.textSecondary) { onNavigate("legal/terms") }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                ProfileMenuRow(Icons.Rounded.PrivacyTip, "Privacy Policy", colors.inputBg, colors.textSecondary) { onNavigate("legal/privacy") }
            }
        }

        // Standalone Red Sign Out Card (Reference Video 01:03)
        item {
            PRCard(
                onClick = { showLogoutDialog = true },
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Logout, null, tint = colors.roseAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = colors.roseAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showSupportSheet) {
        ModalBottomSheet(onDismissRequest = { showSupportSheet = false }, containerColor = colors.card) {
            SupportChannelsContent(channels = supportChannels, onClose = { showSupportSheet = false })
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = colors.card,
            title = { Text("Sign Out?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out of PlayReward?", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                }) { Text("Sign Out", color = colors.roseAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileDetailPill(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = PRTheme.colors
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.inputBg) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Text(value, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        }
    }
}

@Composable
private fun ProfileStatTile(label: String, value: String, icon: ImageVector, tint: Color, bg: Color, modifier: Modifier = Modifier) {
    val colors = PRTheme.colors
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = colors.inputBg, border = BorderStroke(1.dp, colors.cardBorder)) {
        Column(modifier = Modifier.padding(11.dp)) {
            IconBadge(icon, bg, tint, size = 32.dp, cornerRadius = 10.dp)
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ProfileMenuRow(icon: ImageVector, title: String, badgeBg: Color, iconTint: Color, onClick: () -> Unit) {
    val colors = PRTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, badgeBg, iconTint, size = 38.dp, cornerRadius = 12.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.textPrimary, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EditProfileScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()

    var fullName by remember(profile?.fullName) { mutableStateOf(profile?.fullName ?: "") }
    var gender by remember(profile?.gender) { mutableStateOf(profile?.gender?.ifBlank { "Male" } ?: "Male") }
    var ageText by remember(profile?.age) { mutableStateOf(if ((profile?.age ?: 0) > 0) profile!!.age.toString() else "") }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
            Text("Edit Profile", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        PRCard(cornerRadius = 16.dp) {
            PRTextField(value = profile?.email ?: "", onValueChange = {}, label = "Email ID (Cannot be changed)", enabled = false, leadingIcon = Icons.Rounded.Email)
            Spacer(Modifier.height(12.dp))
            PRTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", leadingIcon = Icons.Rounded.Person)
            Spacer(Modifier.height(12.dp))
            Text("Gender", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("Male", "Female", "Other").forEach { g ->
                    FilterChip(selected = gender == g, onClick = { gender = g }, label = { Text(g) }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
            PRTextField(
                value = ageText,
                onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                label = "Age",
                leadingIcon = Icons.Rounded.Cake,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(Modifier.height(18.dp))
            PrimaryGradientButton(
                text = "Save Profile",
                isLoading = saving,
                enabled = fullName.isNotBlank(),
                onClick = {
                    val uid = profile?.uid ?: return@PrimaryGradientButton
                    val age = ageText.toIntOrNull() ?: 0
                    scope.launch {
                        saving = true
                        val res = viewModel.repo.updateProfile(uid, fullName, gender, age)
                        saving = false
                        if (res.isSuccess) {
                            viewModel.showToast("Profile updated!", ToastType.SUCCESS)
                            onBack()
                        } else {
                            viewModel.showToast(res.exceptionOrNull()?.message ?: "Update failed.", ToastType.ERROR)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenChangePassword: () -> Unit,
    onOpenDeleteAccount: () -> Unit
) {
    val colors = PRTheme.colors
    val themeMode by viewModel.themeMode.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val bgServiceEnabled by viewModel.bgServiceEnabled.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val deleteReq = profile?.deleteRequest
    val daysToDelete = if (deleteReq?.requested == true && deleteReq.scheduledDeleteAt > System.currentTimeMillis()) {
        ceil((deleteReq.scheduledDeleteAt - System.currentTimeMillis()).toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(1)
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }

        // Appearance & Theme
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Appearance & Theme", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System").forEach { (m, label) ->
                        val active = themeMode == m
                        Surface(
                            modifier = Modifier.weight(1f).bounceClick {
                                viewModel.soundFx.playClick()
                                viewModel.setTheme(m)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) colors.primaryIndigo else colors.inputBg,
                            border = BorderStroke(1.dp, if (active) colors.primaryIndigo else colors.cardBorder)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 9.dp)) {
                                Text(label, color = if (active) Color.White else colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Sound, Haptics & Background Service
        item {
            PRCard(cornerRadius = 16.dp) {
                Text("Sound, Effects & Background", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound Effects", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        Text("Click, reward & celebration tones", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Switch(checked = soundEnabled, onCheckedChange = {
                        viewModel.setSoundEnabled(it)
                        if (it) viewModel.soundFx.playClick()
                    })
                }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Haptic Feedback", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        Text("Subtle touch vibration on actions", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Switch(checked = hapticsEnabled, onCheckedChange = { viewModel.setHapticsEnabled(it) })
                }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Keep Active in Background", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                        Text("Foreground service keeps timers & downloads alive", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                    Switch(checked = bgServiceEnabled, onCheckedChange = { viewModel.setBgServiceEnabled(it) })
                }
            }
        }

        // Account & Security — Links to Dedicated Screens
        item {
            PRCard(cornerRadius = 16.dp, contentPadding = PaddingValues(vertical = 6.dp)) {
                Text(
                    text = "Account & Security",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                ProfileMenuRow(Icons.Rounded.LockReset, "Change Password", colors.lavenderSoft, colors.primaryIndigo) {
                    viewModel.soundFx.playClick()
                    onOpenChangePassword()
                }
                HorizontalDivider(color = colors.cardBorder, modifier = Modifier.padding(horizontal = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.soundFx.playClick()
                        onOpenDeleteAccount()
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Icons.Rounded.DeleteForever, colors.roseSoft, colors.roseAccent, size = 38.dp, cornerRadius = 12.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Delete Account", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.roseAccent)
                        Text(
                            text = if (deleteReq?.requested == true) "Scheduled — $daysToDelete days left to recover" else "Permanent removal with 7-day grace period",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = colors.roseAccent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ============================================================================
// DEDICATED CHANGE PASSWORD SCREEN
// ============================================================================

@Composable
fun ChangePasswordScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val passwordsMatch = newPass.isNotEmpty() && newPass == confirmPass

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Change Password", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }

        item {
            PRCard(cornerRadius = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.Security, colors.lavenderSoft, colors.primaryIndigo, size = 44.dp, cornerRadius = 13.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Update Account Password", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text("Use at least 6 characters for strong security.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                PRTextField(
                    value = oldPass,
                    onValueChange = { oldPass = it },
                    label = "Old Password",
                    leadingIcon = Icons.Rounded.Lock,
                    visualTransformation = if (showOld) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showOld = !showOld }) {
                            Icon(if (showOld) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = colors.textSecondary)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                PRTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = "New Password",
                    leadingIcon = Icons.Rounded.LockReset,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(if (showNew) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = colors.textSecondary)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                PRTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = "Confirm New Password",
                    leadingIcon = Icons.Rounded.VerifiedUser,
                    isError = confirmPass.isNotEmpty() && !passwordsMatch,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation()
                )
                if (confirmPass.isNotEmpty() && !passwordsMatch) {
                    Spacer(Modifier.height(4.dp))
                    Text("Passwords do not match", color = colors.roseAccent, fontSize = 12.sp)
                }
                Spacer(Modifier.height(18.dp))
                PrimaryGradientButton(
                    text = "Save Changes",
                    isLoading = saving,
                    enabled = oldPass.isNotBlank() && newPass.length >= 6 && passwordsMatch,
                    leadingIcon = Icons.Rounded.CheckCircle,
                    onClick = {
                        scope.launch {
                            saving = true
                            val res = viewModel.repo.changePassword(oldPass, newPass)
                            saving = false
                            if (res.isSuccess) {
                                oldPass = ""; newPass = ""; confirmPass = ""
                                viewModel.showToast("Password changed successfully!", ToastType.SUCCESS)
                                onBack()
                            } else {
                                viewModel.showToast(res.exceptionOrNull()?.message ?: "Could not update password.", ToastType.ERROR)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ============================================================================
// DEDICATED DELETE ACCOUNT SCREEN (7-DAY GRACE & RECOVERY PERIOD)
// ============================================================================

@Composable
fun DeleteAccountScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colors = PRTheme.colors
    val scope = rememberCoroutineScope()
    val profile by viewModel.userProfile.collectAsState()

    var deletePass by remember { mutableStateOf("") }
    var deleteReason by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }

    val deleteReq = profile?.deleteRequest
    val daysLeft = if (deleteReq?.requested == true && deleteReq.scheduledDeleteAt > System.currentTimeMillis()) {
        ceil((deleteReq.scheduledDeleteAt - System.currentTimeMillis()).toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).toInt().coerceAtLeast(1)
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text("Delete Account", style = MaterialTheme.typography.titleLarge, color = colors.roseAccent)
            }
        }

        // Detailed Information Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = colors.roseSoft,
                border = BorderStroke(1.dp, colors.roseBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WarningAmber, null, tint = colors.roseAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Important Deletion Policy", fontWeight = FontWeight.Bold, color = colors.roseAccent, fontSize = 14.5.sp)
                    }
                    Text("• Your account enters a 7-day recovery window after confirmation.", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text("• After 7 days, your profile, coins, INR balance, task history, and referral records will be permanently deleted.", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                    Text("• You can cancel deletion anytime within those 7 days to restore full access.", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                }
            }
        }

        item {
            PRCard(cornerRadius = 16.dp) {
                if (deleteReq?.requested == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Schedule, colors.roseSoft, colors.roseAccent, size = 44.dp, cornerRadius = 13.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Deletion Scheduled", style = MaterialTheme.typography.titleMedium, color = colors.roseAccent)
                            Text("$daysLeft ${if (daysLeft == 1) "day" else "days"} left to recover your account", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryGradientButton(
                        text = "Cancel Deletion & Recover Account",
                        isLoading = processing,
                        leadingIcon = Icons.Rounded.Restore,
                        onClick = {
                            val uid = profile?.uid ?: return@PrimaryGradientButton
                            scope.launch {
                                processing = true
                                val res = viewModel.repo.cancelAccountDeletion(uid)
                                processing = false
                                if (res.isSuccess) {
                                    viewModel.showToast("Account recovered! Deletion cancelled.", ToastType.SUCCESS)
                                } else {
                                    viewModel.showToast(res.exceptionOrNull()?.message ?: "Could not cancel deletion.", ToastType.ERROR)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Confirm Your Identity", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.height(12.dp))
                    PRTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = "Reason for Leaving (Optional)",
                        placeholder = "Help us improve PlayReward",
                        singleLine = false,
                        minLines = 2
                    )
                    Spacer(Modifier.height(12.dp))
                    PRTextField(
                        value = deletePass,
                        onValueChange = { deletePass = it },
                        label = "Account Password (Required for Email Login)",
                        leadingIcon = Icons.Rounded.Lock,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = colors.textSecondary)
                            }
                        }
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = !processing,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.roseAccent),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (processing) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.2.dp)
                        } else {
                            Icon(Icons.Rounded.DeleteForever, null, tint = Color.White, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Request Permanent Deletion", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = colors.card,
            icon = { Icon(Icons.Rounded.DeleteForever, null, tint = colors.roseAccent) },
            title = { Text("Schedule Account Deletion?", color = colors.roseAccent, fontWeight = FontWeight.Bold) },
            text = { Text("Your account will be permanently deleted in 7 days unless you cancel it before then.", color = colors.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    scope.launch {
                        processing = true
                        val res = viewModel.repo.requestAccountDeletion(deletePass, deleteReason)
                        processing = false
                        if (res.isSuccess) {
                            deletePass = ""
                            viewModel.showToast("Deletion scheduled (7 days left to recover).", ToastType.INFO)
                        } else {
                            viewModel.showToast(res.exceptionOrNull()?.message ?: "Authentication failed.", ToastType.ERROR)
                        }
                    }
                }) { Text("Confirm Delete", color = colors.roseAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LegalPageScreen(pageType: String, viewModel: MainViewModel, onBack: () -> Unit) {
    val colors = PRTheme.colors
    val legal by viewModel.legalConfig.collectAsState()
    val (title, body) = when (pageType) {
        "about" -> "About PlayReward" to legal.about.ifBlank { "PlayReward is a rewards app for verified tasks, quizzes, and skill games." }
        "terms" -> "Terms & Conditions" to legal.terms.ifBlank { "Terms & Conditions managed from the admin panel will appear here." }
        else -> "Privacy Policy" to legal.privacy.ifBlank { "Privacy Policy managed from the admin panel will appear here." }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.canvas).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, null, tint = colors.textPrimary, modifier = Modifier.size(20.dp)) }
                Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            }
        }
        item {
            PRCard(cornerRadius = 16.dp) {
                Text(body, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            }
        }
    }
}
