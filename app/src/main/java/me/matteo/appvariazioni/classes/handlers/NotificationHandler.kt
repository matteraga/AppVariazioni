package me.matteo.appvariazioni.classes.handlers

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import me.matteo.appvariazioni.MainActivity
import me.matteo.appvariazioni.R


class NotificationHandler(private val context: Context) {

    private fun sendNotification(notification: Notification) {
        with(NotificationManagerCompat.from(context)) {
            notify(1, notification)
        }
    }

    fun sendPDFNotification(title: String, content: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, "variazioni")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)

        sendNotification(builder.build())
    }

    fun sendBrowserNotification(title: String, content: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, "variazioni")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)

        sendNotification(builder.build())
    }

    fun sendOpenAppNotification(title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, "variazioni")
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)

        sendNotification(builder.build())
    }
}