package com.playreward.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.playreward.app.R
import com.playreward.app.data.model.SupportChannel
import com.playreward.app.ui.components.*
import com.playreward.app.ui.theme.PRTheme
import com.playreward.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class AuthMode { LANDING, SIGN_IN, SIGN_UP, FORGOT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthFlowScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit
) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val supportChannels by viewModel.supportChannels.collectAsState()
    val referralConfig by viewModel.referralConfig.collectAsState()
    val appConfig by viewModel.appConfig.collectAsState()

    var mode by remember { mutableStateOf(AuthMode.LANDING) }
    var loading by remember { mutableStateOf(false) }
    var showSupportSheet by remember { mutableStateOf(false) }

    // Sign In fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var keepLoggedIn by remember { mutableStateOf(true) }
    var showLoginPass by remember { mutableStateOf(false) }

    // Sign Up fields
    var fullName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var ageText by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var agreeTerms by remember { mutableStateOf(false) }
    var showSignUpPass by remember { mutableStateOf(false) }

    // Forgot Password
    var forgotEmail by remember { mutableStateOf("") }

    val webClientId = stringResource(R.string.default_web_client_id)
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    scope.launch {
                        loading = true
                        val res = viewModel.repo.signInWithGoogle(idToken, referralConfig, appConfig.coinsPerInr)
                        loading = false
                        if (res.isSuccess) {
                            viewModel.prefs.setKeepLoggedIn(true)
                            viewModel.syncAuthUid()
                            viewModel.showToast("Welcome to PlayReward!", ToastType.SUCCESS)
                            onAuthSuccess()
                        } else {
                            viewModel.showToast(res.exceptionOrNull()?.message ?: "Google sign-in failed.", ToastType.ERROR)
                        }
                    }
                } else {
                    viewModel.showToast("Configure Firebase Web Client ID in google-services.json.", ToastType.ERROR)
                }
            } catch (e: Exception) {
                viewModel.showToast(e.message ?: "Google sign-in cancelled.", ToastType.ERROR)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mode != AuthMode.LANDING) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { mode = AuthMode.LANDING }) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = when (mode) {
                            AuthMode.SIGN_IN -> "Sign In"
                            AuthMode.SIGN_UP -> "Create Account"
                            AuthMode.FORGOT -> "Reset Password"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(56.dp))
            }

            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = "PlayReward",
                modifier = Modifier.size(if (mode == AuthMode.LANDING) 84.dp else 64.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text("PlayReward", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            Text(
                text = "Play games, complete tasks, earn real rewards",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            when (mode) {
                AuthMode.LANDING -> {
                    // Dark Charcoal Continue with Google button matching reference video
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .bounceClick(enabled = !loading) {
                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(webClientId)
                                    .requestEmail()
                                    .build()
                                val client = GoogleSignIn.getClient(context, gso)
                                googleLauncher.launch(client.signInIntent)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = colors.darkCta
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(22.dp), color = colors.card, strokeWidth = 2.5.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "Continue with Google",
                                        color = if (colors.isDark) Color(0xFF111827) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    SecondaryOutlineButton(
                        text = "Sign in with Email",
                        leadingIcon = Icons.Rounded.Email,
                        onClick = { mode = AuthMode.SIGN_IN },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    PrimaryGradientButton(
                        text = "Create New Account",
                        leadingIcon = Icons.Rounded.PersonAdd,
                        onClick = { mode = AuthMode.SIGN_UP },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(22.dp))
                    TextButton(onClick = { showSupportSheet = true }) {
                        Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = colors.primaryIndigo, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Contact Support", color = colors.primaryIndigo, fontWeight = FontWeight.SemiBold)
                    }
                }

                AuthMode.SIGN_IN -> {
                    PRCard(modifier = Modifier.fillMaxWidth()) {
                        PRTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = "Email ID",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(12.dp))
                        PRTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = "Password",
                            leadingIcon = Icons.Rounded.Lock,
                            visualTransformation = if (showLoginPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showLoginPass = !showLoginPass }) {
                                    Icon(if (showLoginPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = colors.textSecondary)
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { keepLoggedIn = !keepLoggedIn }) {
                                Checkbox(checked = keepLoggedIn, onCheckedChange = { keepLoggedIn = it })
                                Text("Keep me logged in", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                            }
                            TextButton(onClick = {
                                forgotEmail = loginEmail
                                mode = AuthMode.FORGOT
                            }) {
                                Text("Forgot password?", color = colors.primaryIndigo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryGradientButton(
                            text = "Sign In",
                            isLoading = loading,
                            enabled = loginEmail.isNotBlank() && loginPassword.length >= 6,
                            onClick = {
                                scope.launch {
                                    loading = true
                                    val res = viewModel.repo.signInWithEmail(loginEmail, loginPassword)
                                    loading = false
                                    if (res.isSuccess) {
                                        viewModel.prefs.setKeepLoggedIn(keepLoggedIn)
                                        viewModel.syncAuthUid()
                                        viewModel.showToast("Welcome back!", ToastType.SUCCESS)
                                        onAuthSuccess()
                                    } else {
                                        viewModel.showToast(res.exceptionOrNull()?.message ?: "Invalid credentials.", ToastType.ERROR)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { mode = AuthMode.SIGN_UP }) {
                        Text("Don't have an account? Sign Up", color = colors.primaryIndigo, fontWeight = FontWeight.SemiBold)
                    }
                }

                AuthMode.SIGN_UP -> {
                    PRCard(modifier = Modifier.fillMaxWidth()) {
                        PRTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name", leadingIcon = Icons.Rounded.Person)
                        Spacer(Modifier.height(10.dp))
                        PRTextField(
                            value = signUpEmail,
                            onValueChange = { signUpEmail = it },
                            label = "Email ID",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Gender", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Male", "Female", "Other").forEach { g ->
                                val selected = gender == g
                                FilterChip(
                                    selected = selected,
                                    onClick = { gender = g },
                                    label = { Text(g) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        PRTextField(
                            value = ageText,
                            onValueChange = { ageText = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = "Age",
                            leadingIcon = Icons.Rounded.Cake,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        if (referralConfig.enabled) {
                            Spacer(Modifier.height(10.dp))
                            PRTextField(
                                value = referralCode,
                                onValueChange = { referralCode = it.uppercase() },
                                label = "Referral Code (Optional)",
                                placeholder = "Enter friend's invite code",
                                leadingIcon = Icons.Rounded.CardGiftcard
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        PRTextField(
                            value = signUpPassword,
                            onValueChange = { signUpPassword = it },
                            label = "Password",
                            leadingIcon = Icons.Rounded.Lock,
                            visualTransformation = if (showSignUpPass) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showSignUpPass = !showSignUpPass }) {
                                    Icon(if (showSignUpPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = colors.textSecondary)
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        PRTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = "Confirm Password",
                            leadingIcon = Icons.Rounded.LockReset,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { agreeTerms = !agreeTerms }) {
                            Checkbox(checked = agreeTerms, onCheckedChange = { agreeTerms = it })
                            Text("I agree to the Terms & Conditions and Privacy Policy", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        }
                        Spacer(Modifier.height(14.dp))
                        PrimaryGradientButton(
                            text = "Create Account",
                            isLoading = loading,
                            enabled = agreeTerms && fullName.isNotBlank() && signUpEmail.contains("@") && signUpPassword.length >= 6,
                            onClick = {
                                val age = ageText.toIntOrNull() ?: 0
                                if (age < 13 || age > 100) {
                                    viewModel.showToast("Please enter a valid age (13-100).", ToastType.ERROR)
                                    return@PrimaryGradientButton
                                }
                                if (signUpPassword != confirmPassword) {
                                    viewModel.showToast("Passwords do not match.", ToastType.ERROR)
                                    return@PrimaryGradientButton
                                }
                                scope.launch {
                                    loading = true
                                    val res = viewModel.repo.signUpWithEmail(
                                        fullName = fullName,
                                        email = signUpEmail,
                                        gender = gender,
                                        age = age,
                                        password = signUpPassword,
                                        referralCodeInput = referralCode,
                                        referralConfig = referralConfig,
                                        coinsPerInr = appConfig.coinsPerInr
                                    )
                                    loading = false
                                    if (res.isSuccess) {
                                        viewModel.prefs.setKeepLoggedIn(true)
                                        viewModel.syncAuthUid()
                                        viewModel.showToast("Account created! Verification email sent.", ToastType.SUCCESS)
                                        onAuthSuccess()
                                    } else {
                                        viewModel.showToast(res.exceptionOrNull()?.message ?: "Sign up failed.", ToastType.ERROR)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                AuthMode.FORGOT -> {
                    PRCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Enter your registered email address and we'll send a password reset link.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                        Spacer(Modifier.height(14.dp))
                        PRTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it },
                            label = "Registered Email ID",
                            leadingIcon = Icons.Rounded.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                        Spacer(Modifier.height(16.dp))
                        PrimaryGradientButton(
                            text = "Send Verification Link",
                            isLoading = loading,
                            enabled = forgotEmail.contains("@"),
                            onClick = {
                                scope.launch {
                                    loading = true
                                    val res = viewModel.repo.sendPasswordReset(forgotEmail)
                                    loading = false
                                    if (res.isSuccess) {
                                        viewModel.showToast("Password reset link sent to $forgotEmail", ToastType.SUCCESS)
                                        mode = AuthMode.SIGN_IN
                                    } else {
                                        viewModel.showToast(res.exceptionOrNull()?.message ?: "Failed to send reset link.", ToastType.ERROR)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showSupportSheet) {
        ModalBottomSheet(onDismissRequest = { showSupportSheet = false }, containerColor = colors.card) {
            SupportChannelsContent(channels = supportChannels, onClose = { showSupportSheet = false })
        }
    }
}

@Composable
fun SupportChannelsContent(channels: List<SupportChannel>, onClose: (() -> Unit)? = null) {
    val colors = PRTheme.colors
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).navigationBarsPadding()) {
        Text("Contact Support", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Reach out through our official channels", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(16.dp))
        if (channels.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.SupportAgent,
                title = "No Support Channels Yet",
                subtitle = "Support links added from the admin panel will appear here in real time."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                channels.forEach { ch ->
                    PRCard(
                        onClick = {
                            if (ch.profileUrl.isNotBlank()) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ch.profileUrl)))
                                } catch (_: Exception) {}
                            }
                            onClose?.invoke()
                        },
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (ch.logoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ch.logoUrl,
                                    contentDescription = ch.name,
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                IconBadge(Icons.Rounded.Forum, colors.lavenderSoft, colors.primaryIndigo, size = 42.dp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ch.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text(ch.profileUrl, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, maxLines = 1)
                            }
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
