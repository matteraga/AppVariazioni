package me.matteo.appvariazioni.classes.handlers

import android.webkit.URLUtil
import me.matteo.appvariazioni.classes.Response
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*


class RequestHandler {
    private val client = OkHttpClient()

    //Get html body
    suspend fun requestHtml(url: String): Response {
        try {
            if (!URLUtil.isValidUrl(url)) {
                return Response(false)
            }

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return Response(false)
            }

            val timeString = response.headers("date")[0]
            val responseTime = Calendar.getInstance()
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.UK)
            responseTime.time = sdf.parse(timeString) as Date

            val currentTime = Calendar.getInstance()
            if (currentTime.get(Calendar.DAY_OF_YEAR) != responseTime.get(Calendar.DAY_OF_YEAR) ||
                currentTime.get(Calendar.YEAR) != responseTime.get(Calendar.YEAR)
            ) {
                println(false)
                response.close()
                return Response(false)
            }

            val body = response.body!!.string()
            if (body.isNotBlank()) {
                response.close()
                return Response(true, body)
            }
            response.close()
            return Response(false)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }

    //Get file bytes
    suspend fun requestFileBytes(url: String): Response {
        try {
            if (!URLUtil.isValidUrl(url)) {
                return Response(false)
            }
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                return Response(false)
            }

            val bytes = response.body!!.bytes()
            if (bytes.isNotEmpty()) {
                response.close()
                return Response(true, bytes)
            }
            response.close()
            return Response(false)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}