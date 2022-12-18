package me.matteo.appvariazioni.classes.variations

import android.text.Html
import me.matteo.appvariazioni.classes.Response
import org.jsoup.Jsoup
import java.util.*

class ClassroomVariations {

    private fun monthName (): String {
        return when (Calendar.getInstance().get(Calendar.MONTH)) {
            Calendar.JANUARY -> "gennaio"
            Calendar.FEBRUARY -> "febbraio"
            Calendar.MARCH -> "marzo"
            Calendar.APRIL -> "aprile"
            Calendar.MAY -> "maggio"
            Calendar.JUNE -> "giugno"
            Calendar.JULY -> "luglio"
            Calendar.AUGUST -> "agosto"
            Calendar.SEPTEMBER -> "settembre"
            Calendar.OCTOBER -> "ottobre"
            Calendar.NOVEMBER -> "novembre"
            Calendar.DECEMBER -> "dicembre"
            else -> ""
        }
    }

     suspend fun getDayClassroomVariations(body: String, dayName: String, dayNumber: Int): Response {
        try {
            //String used too get only a part of the body, not using Jsoup because it's easier this way
            val strings = arrayOf("<h3><strong>VARIAZIONI AULE</strong></h3>", "<h3><strong>AULE PER SPORTELLI</strong></h3>")
            val variationsSection = body.substring(body.indexOf(strings[0]) + strings[0].length + 1).substringBefore(strings[1])
            val doc = Jsoup.parse(variationsSection)
            //Get all elements in red in the section
            val paragraphs = doc.select("*[style*=#ff0000]")
            for (paragraph in paragraphs.indices) {
                val text = paragraphs[paragraph].toString()
                val month = monthName()
                if (text.contains(dayName.dropLast(1), true) && text.contains(dayNumber.toString(), true)) {
                    for (secondParagraph in paragraph+1 until paragraphs.size) {
                        val textSecond = paragraphs[secondParagraph].toString()
                        if (textSecond.contains(month, true)) {
                            var final = variationsSection.substringAfter(text)
                            final = final.substringBefore(textSecond)
                            final = Html.fromHtml(final, Html.FROM_HTML_MODE_COMPACT).toString()
                            return Response(true, final.trim())
                        }
                    }
                    //No day after
                    var final = variationsSection.substringAfter(text)
                    final = Html.fromHtml(final, Html.FROM_HTML_MODE_COMPACT).toString()
                    return Response(true, final.trim())
                }
            }
            return Response(true, "")
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}