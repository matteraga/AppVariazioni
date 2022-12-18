package me.matteo.appvariazioni.classes.variations

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import me.matteo.appvariazioni.classes.Day
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.Response
import me.matteo.appvariazioni.classes.Utils
import me.matteo.appvariazioni.classes.handlers.FileHandler
import me.matteo.appvariazioni.classes.handlers.RequestHandler
import me.matteo.appvariazioni.classes.variations.hourly.HourlyVariations
import java.text.SimpleDateFormat
import java.util.*

class AllVariations(
    private val context: Context
) {
    private val utils = Utils()
    private val requests = RequestHandler()
    private val files = FileHandler()

    private suspend fun checkHourlyVariations(schoolClass: String, pdfs: List<String>, day: Int): Response {
        val url = utils.getPDFUrl(pdfs, day)
        if (url.success) {
            val bytes = requests.requestFileBytes(url.string)
            if (bytes.success) {
                val variations = HourlyVariations()
                val result = variations.getSchoolClassVariations(schoolClass, bytes.bytes)
                return if (result.success) {
                    if (result.string != "") {
                        Response(true, result.string, bytes.bytes)
                    } else {
                        Response(true, "Nessuna variazione", bytes.bytes)
                    }
                } else {
                    Response(false, "Ultimo controllo non risucito")
                }
            } else {
                return Response(false, "Ultimo controllo non risucito")
            }
        } else {
            return Response(false, "File non trovato")
        }
    }

    suspend fun check(): Response {
        try {
            val schoolClass = context.getSharedPreferences(Key.SETTINGS_PREFERENCES, Context.MODE_PRIVATE).getString(Key.CLASSROOM, "") ?: ""
            val sharedPref = context.getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE)

            var thereAreVariations = false

            //Check for internet
            if (!utils.isNetworkAvailable(context)) {
                return Response(false)
            }

            //Get the html body
            val html =
                requests.requestHtml("https://www.ispascalcomandini.it/variazioni-orario-istituto-tecnico-tecnologico/2017/09/15/")
            if (!html.success) {
                return Response(false)
            }

            //Search for all the pdfs in the body
            val pdfs = utils.getPDFsUrls(html.string)
            if (!pdfs.success) {
                return Response(false)
            }

            val currentTime = Calendar.getInstance()
            if (currentTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                currentTime.add(Calendar.DATE, -1)
            }

            val tomorrowTime = Calendar.getInstance()
            if (tomorrowTime.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                tomorrowTime.add(Calendar.DATE, 2)
            } else {
                tomorrowTime.add(Calendar.DATE, 1)
            }

            val untouchedTime = Calendar.getInstance()

            val today = checkHourlyVariations(schoolClass, pdfs.stringList, Day.TODAY)
            if (today.success) {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_VAR, today.string)
                    apply()
                }

                val document = DocumentFile.fromSingleUri(context, Uri.parse(sharedPref.getString(Key.TODAY_URI, "") ?: ""))
                if (document?.exists() == true) {
                    val stream = context.contentResolver.openInputStream(document.uri)
                    if (!stream?.readBytes().contentEquals(today.bytes)) {
                        val uri = files.savePDF(
                            context,
                            today.bytes,
                            "Variazioni_" + SimpleDateFormat("dd-MM-yyyy", Locale.ITALY).format(currentTime.time)
                        )
                        with(sharedPref.edit()) {
                            putString(Key.TODAY_URI, uri.uri.toString())
                            apply()
                        }
                    }
                    stream?.close()
                } else {
                    val uri = files.savePDF(
                        context,
                        today.bytes,
                        "Variazioni_" + SimpleDateFormat("dd-MM-yyyy", Locale.ITALY).format(currentTime.time)
                    )
                    with(sharedPref.edit()) {
                        putString(Key.TODAY_URI, uri.uri.toString())
                        apply()
                    }
                }
            } else {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_VAR, today.string)
                    putString(Key.TODAY_URI, Uri.EMPTY.toString())
                    apply()
                }
            }

            val tomorrow = checkHourlyVariations(schoolClass, pdfs.stringList, Day.TOMORROW)
            if (tomorrow.success) {
                with(sharedPref.edit()) {
                    putString(Key.TOMORROW_VAR, tomorrow.string)
                    apply()
                }

                if (tomorrow.string.contains("/")){
                    thereAreVariations = true
                }

                val document = DocumentFile.fromSingleUri(context, Uri.parse(sharedPref.getString(Key.TOMORROW_URI, "") ?: ""))
                if (document?.exists() == true) {
                    val stream = context.contentResolver.openInputStream(document.uri)
                    if (!stream?.readBytes().contentEquals(tomorrow.bytes)) {
                        val uri = files.savePDF(
                            context,
                            tomorrow.bytes,
                            "Variazioni_" + SimpleDateFormat("dd-MM-yyyy", Locale.ITALY).format(tomorrowTime.time)
                        )
                        with(sharedPref.edit()) {
                            putString(Key.TOMORROW_URI, uri.uri.toString())
                            apply()
                        }
                    }
                    stream?.close()
                } else {
                    val uri = files.savePDF(
                        context,
                        tomorrow.bytes,
                        "Variazioni_" + SimpleDateFormat("dd-MM-yyyy", Locale.ITALY).format(tomorrowTime.time)
                    )
                    with(sharedPref.edit()) {
                        putString(Key.TOMORROW_URI, uri.uri.toString())
                        apply()
                    }
                }
            } else {
                with(sharedPref.edit()) {
                    putString(Key.TOMORROW_VAR, tomorrow.string)
                    putString(Key.TOMORROW_URI, Uri.EMPTY.toString())
                    apply()
                }
            }

            val unhandledText = UnhandledText()
            val text = unhandledText.getUnhandledText(html.string)
            if (text.success && text.string != "") {
                with(sharedPref.edit()) {
                    putString(Key.UNHANDLED_TEXT, text.string)
                    apply()
                }

                if (text.string.contains(schoolClass, true)) {
                    thereAreVariations = true
                }
            } else if (text.success) {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_CLASSROOM_VARIATION, "")
                    apply()
                }
            } else {
                with(sharedPref.edit()) {
                    putString(Key.UNHANDLED_TEXT, "Ultimo controllo non riuscito")
                    apply()
                }
            }

            //Today
            val classroomVariationsToday = ClassroomVariations()
            val classroomsToday = classroomVariationsToday.getDayClassroomVariations(html.string, utils.dayName(Day.TODAY), currentTime.get(Calendar.DAY_OF_MONTH))
            if (classroomsToday.success && classroomsToday.string != "") {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_CLASSROOM_VARIATION, classroomsToday.string)
                    apply()
                }
            } else if (classroomsToday.success) {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_CLASSROOM_VARIATION, "")
                    apply()
                }
            } else {
                with(sharedPref.edit()) {
                    putString(Key.TODAY_CLASSROOM_VARIATION, "Ultimo controllo non riuscito")
                    apply()
                }
            }

            //Tomorrow
            val classroomVariationsTomorrow = ClassroomVariations()
            val classroomsTomorrow = classroomVariationsTomorrow.getDayClassroomVariations(html.string, utils.dayName(Day.TOMORROW), tomorrowTime.get(Calendar.DAY_OF_MONTH))
            if (classroomsTomorrow.success && classroomsTomorrow.string != "") {
                with(sharedPref.edit()) {
                    putString(Key.TOMORROW_CLASSROOM_VARIATION, classroomsTomorrow.string)
                    apply()
                }

                if (classroomsTomorrow.string.contains(schoolClass, true)) {
                    thereAreVariations = true
                }
            } else if (classroomsTomorrow.success) {
                with(sharedPref.edit()) {
                    putString(Key.TOMORROW_CLASSROOM_VARIATION, "")
                    apply()
                }
            } else {
                with(sharedPref.edit()) {
                    putString(Key.TOMORROW_CLASSROOM_VARIATION, "Ultimo controllo non riuscito")
                    apply()
                }
            }

            with(sharedPref.edit()) {
                putLong(Key.LAST_CHECK, untouchedTime.timeInMillis)
                apply()
            }

            return Response(true, thereAreVariations)
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}