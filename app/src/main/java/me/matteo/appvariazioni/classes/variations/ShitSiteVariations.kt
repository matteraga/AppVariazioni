package me.matteo.appvariazioni.classes.variations

import android.text.Html
import me.matteo.appvariazioni.classes.Color
import me.matteo.appvariazioni.classes.Icon
import me.matteo.appvariazioni.classes.Response
import me.matteo.appvariazioni.classes.variations.hourly.HourlyVariation
import okhttp3.*
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*


class ShitSiteVariations {

    //Initiate custom client with cookies instead of request handler
    private var client = OkHttpClient().newBuilder()
        .cookieJar(object : CookieJar {
            val cookies = mutableListOf<Cookie>()

            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookies.forEach {
                    this.cookies.add(it)
                }
                println(this.cookies.toString())
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookies
            }
        })
        .build()

    //Variations list to string
    private fun variationsToString(variations: List<HourlyVariation>): String {
        return try {
            var string = ""
            for (variation in variations) {
                string += if (variation.substitute == "") {
                    variation.hour.toString() + "° • " + variation.absent + "/" + variation.notes + "/" + variation.color + "/"+ variation.type + "|"
                } else {
                    variation.hour.toString() + "° • " + variation.absent + "/" + variation.notes + "\nSostituto: " + variation.substitute + "/" + variation.color + "/"+ variation.type + "|"
                }
            }
            string
        } catch (thrown: Throwable) {
            ""
        }
    }

    private fun variationColor(note: String): String {
        //Blue is the default color
        var color = Color.BLUE
        if (note.contains("entra", true)) {
            //Red if late entry
            color = Color.RED
        } else if (note.contains("esce", true)) {
            //Green if early exit
            color = Color.GREEN
        }
        return color
    }

    private fun variationType(note: String): Int {
        var type = Icon.SUBSTITUTE
        if (note.contains("entra", true)) {
            type = Icon.ENTRY
        } else if (note.contains("esce", true)) {
            type = Icon.EXIT
        }
        return type
    }

    suspend fun getShitSiteVariations(schoolClass: String): Response {
        try {
            //Body with school code
            val requestBody: RequestBody = FormBody.Builder()
                .add("pass", "PC88075LD")
                .build()

            //Requests with body
            val request = Request.Builder()
                .url("http://www.sostituzionidocenti.com/fe/controllaCodice.php")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {

                val timeString = response.headers("date")[0]
                val responseTime = Calendar.getInstance()
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.UK)
                responseTime.time = sdf.parse(timeString) as Date

                val currentTime = Calendar.getInstance()
                if (currentTime.get(Calendar.DAY_OF_YEAR) != responseTime.get(Calendar.DAY_OF_YEAR) ||
                    currentTime.get(Calendar.YEAR) != responseTime.get(Calendar.YEAR)
                ) {
                    response.close()
                    return Response(false)
                }

                val body = response.body!!.string()
                response.close()
                if (body.isNotBlank()) {
                    val doc = Jsoup.parse(body)
                    //Get the table
                    val table = doc.getElementsByClass("cinereousTable").first()
                    //Get the table body
                    val tBody = table!!.select("tbody").first()
                    if (tBody != null && tBody.children().size > 0) {
                        val variations = mutableListOf<HourlyVariation>()
                        val rows = tBody.select("tr")
                        for (row in rows) {
                            val cols = row.select("td")
                            //Contains instead of equals because equals doesn't work
                            //Check only school class variations
                            if (cols[1].toString().contains(schoolClass, true)) {
                                //Cols content without html tags
                                val colsFormatted = mutableListOf<String>()
                                cols.forEach {
                                    colsFormatted.add(
                                        Html.fromHtml(
                                            it.toString(),
                                            Html.FROM_HTML_MODE_COMPACT
                                        ).toString().trim()
                                    )
                                }
                                //Add to list
                                variations.add(
                                    HourlyVariation(
                                        colsFormatted[0].toIntOrNull() ?: 0,
                                        Html.fromHtml(colsFormatted[1], Html.FROM_HTML_MODE_COMPACT).toString().substringBefore("("),
                                        Html.fromHtml(colsFormatted[1], Html.FROM_HTML_MODE_COMPACT).toString().substringAfter("("),
                                        colsFormatted[2],
                                        colsFormatted[3].replace("-", ""),
                                        "",
                                        false,
                                        colsFormatted[6],
                                        "",
                                        variationColor(colsFormatted[6]),
                                        variationType(colsFormatted[6])
                                    )
                                )
                            }
                        }
                        //If there are variations
                        if (variations.isNotEmpty()) {
                            return Response(true, variationsToString(variations))
                        }
                        //If no variations
                        return Response(true, "")
                    }
                }
            }
            response.close()
            //If error
            return Response(false)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}