package me.matteo.appvariazioni.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import me.matteo.appvariazioni.R
import kotlin.random.Random


class DeleteFilesWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
): CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        startForegroundService()
        try {
            val contentResolver = context.contentResolver
            val directory = DocumentFile.fromTreeUri(context, contentResolver.persistedUriPermissions[0].uri)
                ?: return Result.failure()

            val files = directory.listFiles()
            for (file in files) {
                if (file.name!!.endsWith(".pdf")) {
                    file.delete()
                }
            }

            return Result.success()
        } catch (thrown: Throwable) {
            return Result.failure()
        }
    }

    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "variazioni")
                    .setSmallIcon(R.drawable.ic_sync)
                    .setContentTitle("Eliminazione file...")
                    .setProgress(0, 0, true)
                    .build()
            )
        )
    }
}