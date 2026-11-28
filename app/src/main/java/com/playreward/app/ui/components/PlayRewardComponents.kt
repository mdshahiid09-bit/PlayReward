package com.playreward.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playreward.app.ui.theme.PRTheme
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.965f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bounceScale"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun PRCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(13.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PRTheme.colors
    val shape = RoundedCornerShape(cornerRadius)
    val baseMod = if (onClick != null) modifier.bounceClick(onClick = onClick) else modifier

    Surface(
        modifier = baseMod,
        shape = shape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = if (colors.isDark) 2.dp else 3.dp
    ) {
        Column(
            modifier = Modifier
                .background(colors.cardSheenGradient)
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    gradient: Brush? = null,
    height: Dp = 46.dp,
    cornerRadius: Dp = 14.dp
) {
    val colors = PRTheme.colors
    val brush = gradient ?: colors.heroGradient
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveEnabled = enabled && !isLoading

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(
                brush = if (effectiveEnabled) brush else Brush.linearGradient(
                    listOf(colors.primaryIndigo.copy(alpha = 0.4f), colors.primaryViolet.copy(alpha = 0.4f))
                )
            )
            .bounceClick(enabled = effectiveEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun SecondaryOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    height: Dp = 44.dp
) {
    val colors = PRTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, colors.cardBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.card,
            contentColor = colors.textPrimary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.primaryIndigo,
                strokeWidth = 2.2.dp
            )
        } else {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
        }
    }
}

@Composable
fun PRTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val colors = PRTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = colors.textSecondary, fontSize = 14.sp) }
        } else null,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.card,
            unfocusedContainerColor = colors.inputBg,
            disabledContainerColor = colors.inputBg.copy(alpha = 0.6f),
            focusedBorderColor = colors.primaryIndigo,
            unfocusedBorderColor = colors.cardBorder,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            cursorColor = colors.primaryIndigo
        )
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    val colors = PRTheme.colors
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base = if (colors.isDark) Color(0xFF23273D) else Color(0xFFE6E9F4)
    val highlight = if (colors.isDark) Color(0xFF33395A) else Color(0xFFF7F8FC)
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 220f)
    )
    Box(modifier = modifier.clip(shape).background(brush))
}

@Composable
fun ShimmerListPlaceholder(count: Int = 4) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            PRCard(contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBox(Modifier.size(48.dp), RoundedCornerShape(14.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(Modifier.fillMaxWidth(0.65f).height(16.dp), RoundedCornerShape(6.dp))
                        ShimmerBox(Modifier.fillMaxWidth(0.9f).height(12.dp), RoundedCornerShape(6.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    ShimmerBox(Modifier.width(64.dp).height(28.dp), CircleShape)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.lavenderSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primaryIndigo,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryGradientButton(
                text = actionLabel,
                onClick = onAction,
                height = 46.dp,
                cornerRadius = 24.dp
            )
        }
    }
}

enum class ToastType { SUCCESS, ERROR, INFO }

@Composable
fun FloatingToast(
    message: String?,
    type: ToastType = ToastType.INFO,
    modifier: Modifier = Modifier
) {
    val colors = PRTheme.colors
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val (icon, tint) = when (type) {
            ToastType.SUCCESS -> Icons.Rounded.CheckCircle to colors.mintAccent
            ToastType.ERROR -> Icons.Rounded.Error to colors.roseAccent
            ToastType.INFO -> Icons.Rounded.Info to colors.primaryIndigo
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.cardBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color,
    size: Dp = 36.dp,
    cornerRadius: Dp = 11.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun CelebrationBurstOverlay(visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible) return
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiProgress"
    )
    val palette = listOf(
        Color(0xFF5B6AF0), Color(0xFFF59E0B), Color(0xFF10B981),
        Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF8B5CF6)
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.35f
        for (i in 0 until 28) {
            val angle = (i * 13f) * (Math.PI / 180.0)
            val dist = (60f + (i % 5) * 45f) * progress
            val x = cx + (cos(angle) * dist).toFloat()
            val y = cy + (sin(angle) * dist).toFloat() + (progress * progress * 140f)
            drawCircle(
                color = palette[i % palette.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)),
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun DecorativeOrbsOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.14f),
            radius = size.minDimension * 0.52f,
            center = Offset(size.width * 0.92f, size.height * 0.08f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = size.minDimension * 0.36f,
            center = Offset(size.width * 0.10f, size.height * 0.95f)
        )
    }
}

@Composable
fun ShimmerSweepOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cardSheen")
    val xOffset by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheenX"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    start = Offset(xOffset, 0f),
                    end = Offset(xOffset + 220f, 220f)
                )
            )
    )
}
