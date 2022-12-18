package me.matteo.appvariazioni.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.variations.AllVariations
import kotlin.random.Random

class ManualCheckWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        startForegroundService()

        val allVariations = AllVariations(context)
        val result = allVariations.check()
        if (result.success) {
            return Result.success()
        }
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