package me.matteo.appvariazioni.workers

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.variations.ShitSiteVariations
import java.util.*
import kotlin.random.Random


class ShitSiteCheckWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        startForegroundService()

        val sharedPref =
            context.getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE)
        //Should never be an empty string
        val schoolClass = sharedPref.getString(Key.CLASSROOM, "") ?: ""
        //Don't check if sunday
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            val shitSiteVariations = ShitSiteVariations()
            val result = shitSiteVariations.getShitSiteVariations(schoolClass)
            if (result.success && result.string != "") {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_VAR, result.string)
                    apply()
                }
                return Result.success()
            } else if (result.success) {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_VAR, "")
                    apply()
                }
                return Result.success()
            } else {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_VAR, "Ultimo controllo non riuscito")
                    apply()
                }
                return Result.failure()
            }
        } else {
            return Result.failure()
        }
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