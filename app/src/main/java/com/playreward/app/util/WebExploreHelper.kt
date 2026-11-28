package com.playreward.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import com.playreward.app.service.PlayRewardForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object WebExploreHelper {
    fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "https://playreward.app"
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    fun formatDuration(seconds: Int): String {
        val s = seconds.coerceAtLeast(0)
        return if (s >= 60) {
            val m = s / 60
            val rem = s % 60
            if (rem == 0) "${m}m" else "${m}m ${rem}s"
        } else {
            "${s}s"
        }
    }

    fun openInBrowser(context: Context, rawUrl: String): Boolean {
        val url = normalizeUrl(rawUrl)
        return try {
            val chromeIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chromeIntent)
            true
        } catch (_: Exception) {
            try {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun startLiveCountdownNotification(
        context: Context,
        scope: CoroutineScope,
        requiredSeconds: Int,
        onTimeReached: () -> Unit
    ): Job {
        val startMs = SystemClock.elapsedRealtime()
        return scope.launch {
            var notifiedDone = false
            while (!notifiedDone) {
                val elapsed = ((SystemClock.elapsedRealtime() - startMs) / 1000L).toInt()
                val left = (requiredSeconds - elapsed).coerceAtLeast(0)
                if (left > 0) {
                    val pct = ((elapsed * 100) / requiredSeconds.coerceAtLeast(1)).coerceIn(0, 99)
                    PlayRewardForegroundService.start(
                        context = context,
                        title = "Exploring Website (${formatDuration(elapsed)} / ${formatDuration(requiredSeconds)})",
                        text = "${formatDuration(left)} left — stay on website to unlock your Gem!",
                        progress = pct
                    )
                    delay(1000)
                } else {
                    notifiedDone = true
                    PlayRewardForegroundService.start(
                        context = context,
                        title = "Explore Complete! Return to PlayReward",
                        text = "Time met (${formatDuration(requiredSeconds)})! Switch back now to claim your Gem.",
                        progress = 100
                    )
                    onTimeReached()
                }
            }
        }
    }
}
