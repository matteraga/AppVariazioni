package me.matteo.appvariazioni

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        //Create notification channel
        val channel = NotificationChannel(
            "variazioni",
            "Variazioni",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Notifiche variazioni e errori"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}