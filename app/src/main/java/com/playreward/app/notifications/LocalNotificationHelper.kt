package com.playreward.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.playreward.app.MainActivity
import com.playreward.app.R
import java.util.concurrent.TimeUnit

object LocalNotificationHelper {
    const val CHANNEL_TASKS = "playreward_tasks"
    const val CHANNEL_UPDATES = "playreward_updates"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val taskChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Tasks & Rewards",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task reminders, approvals, rejections, and reward alerts"
            }
            val updateChannel = NotificationChannel(
                CHANNEL_UPDATES,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "PlayReward APK download and update status"
            }
            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(updateChannel)
        }
    }

    fun showNotification(context: Context, id: Int, title: String, message: String, channelId: String = CHANNEL_TASKS) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun scheduleOneDayLeftReminder(context: Context, taskId: String, taskTitle: String, expiresAtMillis: Long) {
        val oneDayBefore = expiresAtMillis - TimeUnit.DAYS.toMillis(1)
        val delayMs = (oneDayBefore - System.currentTimeMillis()).coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
        val data = Data.Builder()
            .putString("taskId", taskId)
            .putString("taskTitle", taskTitle)
            .build()
        val request = OneTimeWorkRequestBuilder<TaskExpiryReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_reminder_$taskId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class TaskExpiryReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getString("taskId") ?: return Result.success()
        val taskTitle = inputData.getString("taskTitle") ?: "Your task"
        LocalNotificationHelper.showNotification(
            context = applicationContext,
            id = taskId.hashCode(),
            title = "1 day left to complete task!",
            message = "Only 1 day left for $taskTitle. Complete the remaining steps before it expires."
        )
        return Result.success()
    }
}
