package me.matteo.appvariazioni.classes.variations.hourly

import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy
import me.matteo.appvariazioni.classes.Color
import me.matteo.appvariazioni.classes.Icon
import me.matteo.appvariazioni.classes.Response


class HourlyVariations {

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

    suspend fun getSchoolClassVariations(schoolClass: String, bytes: ByteArray): Response {
        try {
            val reader: PdfReader = PdfReader(bytes)
            val hourlyVariations = mutableListOf<HourlyVariation>()

            for (pageNum in 0 until reader.numberOfPages) {
                //Custom strategy would be better
                val strategy = SimpleTextExtractionStrategy()
                val pageContent = PdfTextExtractor.getTextFromPage(reader, pageNum + 1, strategy)
                val linesMatrix: MutableList<String> = pageContent.lines().toMutableList()

                //Remove first 3 line for every page
                linesMatrix.removeAt(0)
                linesMatrix.removeAt(0)
                //Old linesMatrix.removeAt(linesMatrix.size - 1)
                linesMatrix.removeAt(0)

                for (lineNum in linesMatrix.indices step 4) {
                    //Only format necessary variations
                    if (linesMatrix[lineNum + 1].lowercase().trim() == schoolClass.lowercase()) {
                        //Shitty string manipulation that break to easily
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

                        //Create and add new variation object to the list
                        hourlyVariations.add(
                            HourlyVariation(
                                hour,
                                schoolClass,
                                classroom,
                                absent,
                                substitute,
                                substitute2,
                                paid,
                                notes,
                                signature,
                                variationColor(notes),
                                variationType(notes)
                            )
                        )
                    }
                }
            }
            //Closing the pdf reader
            reader.close()

            //If there are variations
            if (hourlyVariations.isNotEmpty()) {
                return Response(true, variationsToString(hourlyVariations))
            }

            //If there aren't variations
            return Response(true, "")
        } catch (thrown: Throwable) {
            return Response(false)
        }
    }
}