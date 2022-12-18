package me.matteo.appvariazioni.fragments

import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.*
import me.matteo.appvariazioni.classes.list.ListViewAdapter
import me.matteo.appvariazioni.classes.list.ListViewItem
import me.matteo.appvariazioni.classes.variations.timetable.TimetableDay
import me.matteo.appvariazioni.classes.variations.timetable.TimetableHour
import java.util.*

class DayTimetableFragment : Fragment(R.layout.fragment_day_timetable) {

    private val weekTimetable = listOf<TimetableDay>(
        //Lunedì
        TimetableDay(
            listOf(
                TimetableHour(
                    1,
                    1,
                    "Matematica",
                    Icon.MATH,
                    "Sirotti Giuliana",
                    "Aula Magna"
                ),
                TimetableHour(
                    2,
                    3,
                    "Informatica",
                    Icon.COMPUTER_SCIENCE,
                    "Venturi Francesco, D'Andrea Davide",
                    "L13"
                ),
                TimetableHour(
                    5,
                    1,
                    "TP-SIT",
                    Icon.TPSIT,
                    "Lucchi Matteo",
                    "P9"
                )
            )
        ),
        //Martedì
        TimetableDay(
            listOf(
                TimetableHour(
                    1,
                    2,
                    "Italiano",
                    Icon.ITALIAN,
                    "Benini Barbara",
                    "10"
                ),
                TimetableHour(
                    3,
                    1,
                    "Matematica",
                    Icon.MATH,
                    "Sirotti Giuliana",
                    "10"
                ),
                TimetableHour(
                    4,
                    1,
                    "Inglese",
                    Icon.ENGLISH,
                    "Decarli Silvia",
                    "9"
                ),
                TimetableHour(
                    5,
                    2,
                    "SR",
                    Icon.NETWORKS,
                    "Melagranati Lorenzo, D'Andrea Davide",
                    "LT"
                )
            )
        ),
        //Mercoledì
        TimetableDay(
            listOf(
                TimetableHour(
                    1,
                    1,
                    "Matematica",
                    Icon.MATH,
                    "Sirotti Giuliana",
                    "L22"
                ),
                TimetableHour(
                    2,
                    1,
                    "Informatica",
                    Icon.COMPUTER_SCIENCE,
                    "Venturi Francesco",
                    "L22"
                ),
                TimetableHour(
                    3,
                    1,
                    "Storia",
                    Icon.HISTORY,
                    "Benini Barbara",
                    "69"
                ),
                TimetableHour(
                    4,
                    1,
                    "Inglese",
                    Icon.ENGLISH,
                    "Decarli Silvia",
                    "Aula Magna"
                ),
                TimetableHour(
                    5,
                    2,
                    "Telecomunicazioni",
                    Icon.TELECOMMUNICATIONS,
                    "Dall'Ara Jacopo, Tonini Tiziano",
                    "L12"
                )
            )
        ),
        //Giovedì
        TimetableDay(
            listOf(
                TimetableHour(
                    1,
                    1,
                    "Telecomunicazioni",
                    Icon.TELECOMMUNICATIONS,
                    "Dall'Ara Jacopo",
                    "L12"
                ),
                TimetableHour(
                    2,
                    1,
                    "Inglese",
                    Icon.ENGLISH,
                    "Decarli Silvia",
                    "Aula Magna"
                ),
                TimetableHour(
                    3,
                    2,
                    "Motoria",
                    Icon.PE,
                    "Zoffoli Lorenzo",
                    "Palestra Pascal"
                ),
                TimetableHour(
                    5,
                    1,
                    "SR",
                    Icon.NETWORKS,
                    "Melagranati Lorenzo",
                    "10"
                )
            )
        ),
        //Venerdì
        TimetableDay(
            listOf(
                TimetableHour(
                    1,
                    1,
                    "Storia",
                    Icon.HISTORY,
                    "Benini Barbara",
                    "Aula Magna"
                ),
                TimetableHour(
                    2,
                    1,
                    "SR",
                    Icon.NETWORKS,
                    "Melagranati Lorenzo",
                    "10"
                ),
                TimetableHour(
                    3,
                    2,
                    "Informatica",
                    Icon.COMPUTER_SCIENCE,
                    "Venturi Francesco",
                    "10"
                ),
                TimetableHour(
                    5,
                    1,
                    "Matematica",
                    Icon.MATH,
                    "Sirotti Giuliana",
                    "P11"
                )
            )
        ),
        //Sabato
        TimetableDay(
            listOf(
                TimetableHour(
                    1,
                    2,
                    "TP-SIT",
                    Icon.TPSIT,
                    "Lucchi Matteo, Lombardi Nevio",
                    "L1"
                ),
                TimetableHour(
                    3,
                    1,
                    "Religione",
                    Icon.RELIGION,
                    "Baronio Barbara",
                    "P14"
                ),
                TimetableHour(
                    4,
                    2,
                    "Italiano",
                    Icon.ITALIAN,
                    "Benini Barbara",
                    "10"
                )
            )
        )
    )

    private val utils = Utils()

    private fun hourColor(hour: TimetableHour): String {
        return when (hour.subjectInt) {
            Icon.HISTORY -> "#A6A098"
            Icon.ENGLISH -> "#D92332"
            Icon.PE -> "#565AA6"
            Icon.MATH -> "#818AA6"
            Icon.ITALIAN -> "#F27405"
            Icon.COMPUTER_SCIENCE -> "#D7D9C7"
            Icon.NETWORKS -> "#0583F2"
            Icon.RELIGION -> "#F29829"
            Icon.TELECOMMUNICATIONS -> "#04D9C4"
            Icon.TPSIT -> "#358C42"
            else -> Color.BLUE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(view)

        val swipeLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeLayoutTimetable)
        swipeLayout.setOnRefreshListener {
            updateUI(view)
            swipeLayout.isRefreshing = false
        }
    }

    private fun updateUI(view: View) {
        val listView = view.findViewById<ListView>(R.id.listViewTimetable)
        val errorTxt = view.findViewById<TextView>(R.id.errorTextTimetable)

        val day = arguments?.getInt("day", -1) ?: -1

        if (day != -1) {
            val calendar = Calendar.getInstance()
            if (day == Day.TODAY) {
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    calendar.add(Calendar.DATE, -1)
                }
            } else {
                if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    calendar.add(Calendar.DATE, 2)
                } else {
                    calendar.add(Calendar.DATE, 1)
                }
            }
            val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 5
                else -> -1
            }
            val dayTimetable = weekTimetable[dayOfWeek].timetable

            val content = mutableListOf<ListViewItem>()
            /*var formattedDay = utils.dayName(day)
            formattedDay = formattedDay[0].uppercase() + formattedDay.drop(1)
            if (formattedDay[formattedDay.lastIndex] == 'i') {
                formattedDay = formattedDay.dropLast(1) + "ì"
            }
            content.add(ListViewItem(Type.SECTION_NAME, formattedDay))*/
            for (hour in dayTimetable) {
                content.add(
                    ListViewItem(
                        Type.NORMAL,
                        hour.hour.toString() + "° • " + hour.professor,
                        hour.subject + " • " + hour.classroom + " • " + hour.numberOfHours + "h",
                        hourColor(hour),
                        hour.subjectInt
                    )
                )
            }

            val adapter = ListViewAdapter(
                requireActivity(),
                //R.layout.variation_view,
                content
            )
            listView.adapter = adapter
            listView.visibility = View.VISIBLE
            errorTxt.visibility = View.GONE

            if (requireActivity().supportFragmentManager.findFragmentByTag("timetable") is TimetableFragment) {
                val timetable = requireActivity().supportFragmentManager.findFragmentByTag("timetable") as TimetableFragment
                timetable.updateTabsName()
            }
        } else {
            listView.visibility = View.GONE
            errorTxt.visibility = View.VISIBLE
            errorTxt.text = "Errore"
        }
    }
}