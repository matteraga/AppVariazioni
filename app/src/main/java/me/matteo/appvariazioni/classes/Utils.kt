package me.matteo.appvariazioni.classes

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.*
import java.util.regex.Pattern

class Utils {

    fun isWorkScheduled(instance: WorkManager, tag: String): Boolean {
        val statuses = instance.getWorkInfosByTag(tag)
        return try {
            var running = false
            val workInfoList = statuses.get()
            for (workInfo in workInfoList) {
                val state = workInfo.state
                if ((state == WorkInfo.State.RUNNING) or (state == WorkInfo.State.ENQUEUED)) {
                    running = true
                    break
                }
            }
            running
        } catch (thrown: Throwable) {
            false
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    fun dayName (day: Int, time: Long? = null): String {
        val calendar = Calendar.getInstance()
        if (time != null) {
            calendar.timeInMillis = time
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        when (day) {
            0 -> {
                return when (dayOfWeek) {
                    Calendar.MONDAY -> "lunedi"
                    Calendar.TUESDAY -> "martedi"
                    Calendar.WEDNESDAY -> "mercoledi"
                    Calendar.THURSDAY -> "giovedi"
                    Calendar.FRIDAY -> "venerdi"
                    Calendar.SATURDAY -> "sabato"
                    Calendar.SUNDAY -> "sabato"
                    else -> ""
                }
            }
            1 -> {
                return when (dayOfWeek) {
                    Calendar.MONDAY -> "martedi"
                    Calendar.TUESDAY -> "mercoledi"
                    Calendar.WEDNESDAY -> "giovedi"
                    Calendar.THURSDAY -> "venerdi"
                    Calendar.FRIDAY -> "sabato"
                    Calendar.SATURDAY -> "lunedi"
                    Calendar.SUNDAY -> "lunedi"
                    else -> ""
                }
            }
            else -> {
                return ""
            }
        }
    }

    suspend fun getPDFsUrls(body: String): Response {
        try {
            val pdfs = mutableListOf<String>()
            val pattern = Pattern.compile("href=\"(.*?)\"")
            val matcher = pattern.matcher(body)
            while (matcher.find()) {
                val link = matcher.group(1)
                if (link != null && link.endsWith(".pdf")) {
                    pdfs.add(link)
                }
            }

            if (pdfs.isNotEmpty()) {
                return Response(true, pdfs)
            }
            return Response(false)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }

    suspend fun getPDFUrl(pdfs: List<String>, day: Int): Response {
        try {
            val dayName = dayName(day)
            if (dayName != "") {
                for (url in pdfs) {
                    if (url.contains(dayName, true)) {
                        return Response(true, url)
                    }
                }
                return Response(false)
            }
            return Response(false)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        val nightModeFlags: Int = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return when (nightModeFlags) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }
}