package me.matteo.appvariazioni.classes.variations

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.matteo.appvariazioni.classes.Color
import me.matteo.appvariazioni.classes.Icon
import me.matteo.appvariazioni.classes.Response
import me.matteo.appvariazioni.classes.handlers.RequestHandler
import me.matteo.appvariazioni.classes.variations.hourly.HourlyVariation
import me.matteo.appvariazioni.classes.variations.hourly.HourlyVariations
import org.jsoup.Jsoup
import java.util.Calendar
import java.util.regex.Pattern

object CalendarKSerializer : KSerializer<Calendar> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Variations", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Calendar) {
        val string = value.timeInMillis.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Calendar {
        val string = decoder.decodeString()
        val calender = Calendar.getInstance()
        calender.timeInMillis = string.toLong()
        return calender
    }
}

@Serializable
data class Hourly(
    val hour: Int,
    val schoolClass: String,
    val classroom: String,
    val absent: String,
    val substitute: String?,
    val substitute2: String?,
    val paid: Boolean?,
    val notes: String,
    val signature: String?,
    val color: String
)

@Serializable
data class SimpleText(
    val string: String
)

@Serializable
data class AllVariationsList(
    val hourly: PersistentList<Hourly> = persistentListOf(),
    val unhandled: PersistentList<SimpleText> = persistentListOf(),
    val classroom: PersistentList<SimpleText> = persistentListOf()
)

@Serializable
class Variations(
    val schoolClass: String,
    @Serializable(with = CalendarKSerializer::class)
    val checkTime: Calendar
) {
    val variations = AllVariationsList()

    @Transient
    val requests: RequestHandler? = null

    private fun isNetworkAvailable(context: Context): Boolean {
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

    private fun searchAllPDFS(body: String): List<String> {
        try {
            val pdfs = mutableListOf<String>()
            val strings = arrayOf("<h3><strong>VARIAZIONI ORARIO E SOSTITUZIONI</strong></h3>", "<h3><strong>VARIAZIONI AULE</strong></h3>")
            val variationsSection = body.substring(body.indexOf(strings[0]) + strings[0].length + 1).substringBefore(strings[1])
            val doc = Jsoup.parse(variationsSection)

            val hrefs = doc.select("*[href*]")
            for (href in hrefs) {
                val link = href.text()
                if (link.endsWith(".pdf")) {
                    pdfs.add(link)
                }
            }
            return pdfs

            /*val pattern = Pattern.compile("href=\"(.*?)\"")
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
            return Response(false)*/
        } catch (thrown: Throwable) {
            return emptyList()
        }
    }

    private fun dayName (time: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "lunedi"
            Calendar.TUESDAY -> "martedi"
            Calendar.WEDNESDAY -> "mercoledi"
            Calendar.THURSDAY -> "giovedi"
            Calendar.FRIDAY -> "venerdi"
            Calendar.SATURDAY -> "sabato"
            else -> ""
        }
    }

    private fun searchPDFUrl(pdfs: List<String>, day: Calendar): String {
        try {
            val dayName = dayName(day.timeInMillis)
            if (dayName != "") {
                for (url in pdfs) {
                    if (url.contains(dayName, true) && url.contains(day.get(Calendar.DAY_OF_MONTH).toString())) {
                        return url
                    }
                }
                return ""
            }
            return ""
        } catch (thrown: Throwable) {
            return ""
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

    private fun checkHourlyVariationsSchoolSite(bytes: ByteArray): Boolean {
        try {
            val reader: PdfReader = PdfReader(bytes)
            variations.hourly.clear()

            for (pageNum in 0 until reader.numberOfPages) {
                // Custom strategy would be better
                val strategy = SimpleTextExtractionStrategy()
                val pageContent = PdfTextExtractor.getTextFromPage(reader, pageNum + 1, strategy)
                val linesMatrix: MutableList<String> = pageContent.lines().toMutableList()

                // Remove first 3 line for every page
                linesMatrix.removeAt(0)
                linesMatrix.removeAt(0)
                // Old linesMatrix.removeAt(linesMatrix.size - 1)
                linesMatrix.removeAt(0)

                for (lineNum in linesMatrix.indices step 4) {
                    // Only format necessary variations
                    if (linesMatrix[lineNum + 1].lowercase().trim() == schoolClass.lowercase()) {
                        // Shitty string manipulation that break to easily
                        val hour = linesMatrix[lineNum].toIntOrNull() ?: 0
                        val classroom = linesMatrix[lineNum + 2].replace("(", "").replace(")", "")
                        var absent = linesMatrix[lineNum + 3]
                        absent = absent.substringBefore(".") + "."
                        var substitute = linesMatrix[lineNum + 3].substringAfter(".").trim()
                        if (substitute.count { it == '-' } > 1) {
                            substitute = ""
                        } else {
                            substitute = substitute.substringBefore("-").trim()
                        }
                        val substitute2 = null
                        val paid = false
                        var notes = linesMatrix[lineNum + 3].substringAfterLast("-")
                        notes = notes.substring(3).trim()
                        val signature = ""

                        // Create and add new variation object to the list
                        variations.hourly.add(
                            Hourly(
                                hour,
                                schoolClass,
                                classroom,
                                absent,
                                substitute,
                                substitute2,
                                paid,
                                notes,
                                signature,
                                variationColor(notes)
                            )
                        )
                    }
                }
            }
            // Closing the pdf reader
            reader.close()

            return true
        } catch (thrown: Throwable) {
            variations.hourly.clear()
            return false
        }
    }

    public suspend fun check(context: Context, body: String, pdfs: List<String>): Boolean {

        if (requests == null || !isNetworkAvailable(context)) {
            return false
        }

        val pdf = searchPDFUrl(pdfs, checkTime)
        if (pdf != "") {
            val bytes = requests.requestFileBytes(pdf)
            val result = checkHourlyVariationsSchoolSite(bytes.bytes)
        }

        return true
    }
}