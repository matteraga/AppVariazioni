package me.matteo.appvariazioni.classes.variations

import android.text.Html
import me.matteo.appvariazioni.classes.Response
import org.jsoup.Jsoup

class UnhandledText {

    private fun unhandledToString(unhandled: List<String>): String {
        return try {
            var string = ""
            for (text in unhandled) {
                string += text + "|"
            }
            string
        } catch (thrown: Throwable) {
            ""
        }
    }

     suspend fun getUnhandledText(body: String): Response {
        try {
            val unhandled = mutableListOf<String>()
            //String used too get only a part of the body, not using Jsoup because it's easier this way
            val strings = arrayOf("<h3><strong>VARIAZIONI ORARIO E SOSTITUZIONI</strong></h3>", "<h3><strong>VARIAZIONI AULE</strong></h3>")
            val variationsSection = body.substring(body.indexOf(strings[0]) + strings[0].length + 1).substringBefore(strings[1])
            val doc = Jsoup.parse(variationsSection)
            //Get all paragraphs
            val paragraphs = doc.select("p")
            for (paragraph in paragraphs) {
                val text = paragraph.toString()
                //If it is not a link or the last line add it
                if (!text.contains("href") && !text.contains("PC88075LD")) {
                    //Remove html tags
                    val formatted = Html.fromHtml(paragraph.toString(), Html.FROM_HTML_MODE_COMPACT).toString()
                    unhandled.add(formatted.trim())
                }
            }

            /*val pattern = Pattern.compile("<p>(.*?)</p>")
            val matcher = pattern.matcher(variationsSection)
            while (matcher.find()) {
                val paragraph = matcher.group(1)
                if (paragraph != null) {
                    if (!paragraph.contains("href") && !paragraph.contains("PC88075LD", true)) {
                        val formatted = Html.fromHtml(paragraph, Html.FROM_HTML_MODE_COMPACT).toString()
                        unhandled.add(formatted)
                    }
                }
            }*/

            if (unhandled.isNotEmpty()) {
                return Response(true, unhandledToString(unhandled))
            }
            return Response(true, "")
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}