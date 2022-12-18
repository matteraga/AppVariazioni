package me.matteo.appvariazioni.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.handlers.NotificationHandler
import me.matteo.appvariazioni.classes.variations.AllVariations
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class AutomaticCheckWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    private val notifications = NotificationHandler(context)

    override suspend fun doWork(): Result {
        startForegroundService()

        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        dueDate.set(Calendar.HOUR_OF_DAY, 19)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        dueDate.add(Calendar.HOUR_OF_DAY, 24)
        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
        val dailyWorkRequest = OneTimeWorkRequestBuilder<AutomaticCheckWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .addTag("automatic")
            .build()
        WorkManager.getInstance(context).enqueue(dailyWorkRequest)

        val allVariations = AllVariations(context)
        val result = allVariations.check()
        if (result.success) {
            if (result.bool) {
                notifications.sendOpenAppNotification("Variazioni", "✅ Ci sono variazioni premi per controllare")
            }
            return Result.success()
        }
        notifications.sendOpenAppNotification("Variazioni", "❌ Controllo non riuscito prova manualmente")
        return Result.failure()
    }

    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "variazioni")
                    .setSmallIcon(R.drawable.ic_sync)
                    .setContentTitle("Controllo nuove variazioni...")
                    .setProgress(0, 0, true)
                    .build()
            )
        )
    }
}