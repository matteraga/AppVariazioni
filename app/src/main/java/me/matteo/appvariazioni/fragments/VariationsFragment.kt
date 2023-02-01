package me.matteo.appvariazioni.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.Type
import me.matteo.appvariazioni.classes.list.ListViewAdapter
import me.matteo.appvariazioni.classes.list.ListViewItem


class VariationsFragment : Fragment(R.layout.fragment_variations) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI(view)

        val swipeLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeLayoutVariations)
        swipeLayout.setOnRefreshListener {
            updateUI(view)
            swipeLayout.isRefreshing = false
        }
    }

    private fun updateUI(view: View) {
        val listView = view.findViewById<ListView>(R.id.listViewVariations)

        val dayKey = arguments?.getString("dayKey", "")
        val classroomKey = arguments?.getString("classroomKey", "")

        val sharedPref: SharedPreferences? =
            requireActivity().getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE)
        if (sharedPref != null) {
            val content = mutableListOf<ListViewItem>()

            content.add(ListViewItem(Type.SECTION_NAME, "Variazioni"))
            val variationsString = sharedPref.getString(dayKey, "Nessun controllo eseguito") ?: "Nessun controllo eseguito"
            if (variationsString.contains("/")) {
                val variationsArray = variationsString.substringBeforeLast("|").split("|")
                for (variation in variationsArray) {
                    val data = variation.split("/")
                    if (data.size == 4 && data[3].toIntOrNull() != null) {
                        content.add(ListViewItem(Type.NORMAL, data[0], data[1], data[2], data[3].toInt()))
                    }
                }
            } else {
                content.add(ListViewItem(Type.SIMPLE_TEXT_BACKGROUND, variationsString))
            }

            content.add(ListViewItem(Type.SECTION_NAME, "Non gestite"))
            val unhandledString = sharedPref.getString(Key.UNHANDLED_TEXT, "Nessun controllo eseguito") ?: "Nessun controllo eseguito"
            if (unhandledString.contains("|")) {
                val unhandledArray = unhandledString.substringBeforeLast("|").split("|")
                for (text in unhandledArray) {
                    content.add(ListViewItem(Type.SIMPLE_TEXT_BACKGROUND, text))
                }
            } else {
                if (unhandledString == "") {
                    content.removeAt(content.lastIndex)
                } else {
                    content.add(ListViewItem(Type.SIMPLE_TEXT_BACKGROUND, unhandledString))
                }
            }

            content.add(ListViewItem(Type.SECTION_NAME, "Variazioni aula"))
            val classroomsString = sharedPref.getString(classroomKey, "Nessun controllo eseguito") ?: "Nessun controllo eseguito"
            if (classroomsString == "") {
                content.removeAt(content.lastIndex)
            } else {
                content.add(ListViewItem(Type.SIMPLE_TEXT_BACKGROUND, classroomsString))
            }

            val adapter = ListViewAdapter(
                requireActivity(),
                //R.layout.variation_view,
                content
            )
            listView.adapter = adapter

            if (requireActivity().supportFragmentManager.findFragmentByTag("home") is HomeFragment) {
                val home = requireActivity().supportFragmentManager.findFragmentByTag("home") as HomeFragment
                home.updateLastCheckAndTabsName()
            }
        } else {
            val adapter = ListViewAdapter(
                requireActivity(),
                listOf(ListViewItem(Type.SIMPLE_TEXT_BACKGROUND, "Errore"))
            )
            listView.adapter = adapter
        }
    }
}