package com.playreward.app

import android.app.Application
import com.google.firebase.database.FirebaseDatabase
import com.playreward.app.notifications.LocalNotificationHelper

class PlayRewardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (_: Exception) {
        }
        LocalNotificationHelper.createChannels(this)
    }
}
