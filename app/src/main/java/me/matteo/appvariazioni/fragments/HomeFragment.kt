package me.matteo.appvariazioni.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.registerReceiver
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.TabView
import com.google.android.material.tabs.TabLayoutMediator
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Day
import me.matteo.appvariazioni.classes.Key
import me.matteo.appvariazioni.classes.Strings
import me.matteo.appvariazioni.classes.Utils
import java.text.SimpleDateFormat
import java.util.*


class HomeFragment : Fragment(R.layout.fragment_home) {

    private val utils = Utils()
    private var minuteUpdateReceiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.top_bar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.pdf -> openPDF()
            R.id.browser -> openBrowser()
        }
        return true
    }

    private fun openPDF() {
        val sharedPref =
            requireActivity().getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE)
        val tabLayout = view?.findViewById<TabLayout>(R.id.tabLayoutHome)
        val tab = tabLayout?.selectedTabPosition
        var uri: Uri = Uri.EMPTY
        when (tab) {
            Day.TODAY -> uri = Uri.parse(sharedPref.getString(Key.TODAY_URI, "")) ?: Uri.EMPTY
            Day.TOMORROW -> uri = Uri.parse(sharedPref.getString(Key.TOMORROW_URI, "")) ?: Uri.EMPTY
        }
        if (DocumentFile.fromSingleUri(requireActivity(), uri)!!.exists()) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }
            startActivity(Intent.createChooser(intent, "Apri con"))
        } else {
            Toast.makeText(requireActivity(), "File non trovato", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openBrowser() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(Strings.VARIATIONS_LINK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        startActivity(Intent.createChooser(intent, "Naviga con"))
    }

    private fun updateLastCheck() {
        val time = requireActivity().getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE).getLong(Key.LAST_CHECK, 0L)
        val lastCheckText = requireView().findViewById<TextView>(R.id.lastCheckText)
        if (time != 0L) {
            val savedTime = Calendar.getInstance()
            savedTime.timeInMillis = time
            val currentTime = Calendar.getInstance()
            var difference = currentTime.timeInMillis - savedTime.timeInMillis

            val secondsInMilli: Long = 1000
            val minutesInMilli = secondsInMilli * 60
            val hoursInMilli = minutesInMilli * 60
            val daysInMilli = hoursInMilli * 24

            val elapsedDays: Long = difference / daysInMilli
            difference %= daysInMilli

            val elapsedHours: Long = difference / hoursInMilli
            difference %= hoursInMilli

            val elapsedMinutes: Long = difference / minutesInMilli
            difference %= minutesInMilli

            //val elapsedSeconds: Long = difference / secondsInMilli

            if (elapsedMinutes < 1) {
                lastCheckText.text = "Ora"
            } else if (elapsedHours < 1) {
                lastCheckText.text = elapsedMinutes.toString() + if (elapsedMinutes == 1L) " minuto fa" else " minuti fa"
            } else if (elapsedDays < 1) {
                lastCheckText.text = elapsedHours.toString() + if (elapsedHours == 1L) " ora fa" else " ore fa"
            } else {
                lastCheckText.text = elapsedDays.toString() + if (elapsedDays == 1L) " giorno fa" else " giorni fa"
            }

            /*if (currentTime.get(Calendar.DAY_OF_YEAR) == savedTime.get(Calendar.DAY_OF_YEAR) &&
                currentTime.get(Calendar.YEAR) == savedTime.get(Calendar.YEAR)
            ) {
                lastCheckText.text = SimpleDateFormat("HH:mm").format(time)
            } else {
                lastCheckText.text = SimpleDateFormat("dd-MM-yyyy • HH:mm").format(time)
            }*/
        } else {
            lastCheckText.text = "Mai"
        }
    }

    fun updateLastCheckAndTabsName() {
        val sharedPref =
            requireActivity().getSharedPreferences(Key.VAR_PREFERENCES, Context.MODE_PRIVATE)
        var time = sharedPref.getLong(Key.LAST_CHECK, 0L)

        updateLastCheck()

        if (time == 0L) {
            time = Calendar.getInstance().timeInMillis
        }

        val currentTime = Calendar.getInstance()
        currentTime.timeInMillis = time
        if (currentTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            currentTime.add(Calendar.DATE, -1)
        }

        val tomorrowTime = Calendar.getInstance()
        tomorrowTime.timeInMillis = time
        if (tomorrowTime.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
            tomorrowTime.add(Calendar.DATE, 2)
        } else {
            tomorrowTime.add(Calendar.DATE, 1)
        }

        val untouchedTime = Calendar.getInstance()
        untouchedTime.timeInMillis = time

        var formattedDay = utils.dayName(Day.TODAY, untouchedTime.timeInMillis)
        formattedDay = formattedDay[0].uppercase() + formattedDay.drop(1)
        if (formattedDay[formattedDay.lastIndex] == 'i') {
            formattedDay = formattedDay.dropLast(1) + "ì"
        }

        val tabLayout = requireView().findViewById<TabLayout>(R.id.tabLayoutHome)
        val today = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(0) as TabView
        today.tab?.text = formattedDay + " - " + SimpleDateFormat("dd/MM", Locale.ITALY).format(currentTime.timeInMillis)

        formattedDay = utils.dayName(Day.TOMORROW, untouchedTime.timeInMillis)
        formattedDay = formattedDay[0].uppercase() + formattedDay.drop(1)
        if (formattedDay[formattedDay.lastIndex] == 'i') {
            formattedDay = formattedDay.dropLast(1) + "ì"
        }

        val tomorrow = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(1) as TabView
        tomorrow.tab?.text = formattedDay + " - " + SimpleDateFormat("dd/MM", Locale.ITALY).format(tomorrowTime.timeInMillis)
    }

    private fun startMinuteUpdater() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_TIME_TICK)
        minuteUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateLastCheck()
            }
        }
        registerReceiver(requireActivity(), minuteUpdateReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerHome)
        val adapter = DaysPagerAdapter(requireActivity())
        viewPager.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutHome)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Oggi"
                1 -> tab.text = "Domani"
            }
        }.attach()

        updateLastCheckAndTabsName()
    }

    override fun onResume() {
        super.onResume()

        startMinuteUpdater()
    }

    override fun onPause() {
        super.onPause()

        requireActivity().unregisterReceiver(minuteUpdateReceiver)
    }

    private inner class DaysPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(
        fragmentActivity
    ) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return if (position == 0) {
                val today = VariationsFragment()
                val todayBundle = Bundle()
                todayBundle.putString("dayKey", Key.TODAY_VAR)
                todayBundle.putString("classroomKey", Key.TODAY_CLASSROOM_VARIATION)
                today.arguments = todayBundle
                today
            } else {
                val tomorrow = VariationsFragment()
                val tomorrowBundle = Bundle()
                tomorrowBundle.putString("dayKey", Key.TOMORROW_VAR)
                tomorrowBundle.putString("classroomKey", Key.TOMORROW_CLASSROOM_VARIATION)
                tomorrow.arguments = tomorrowBundle
                tomorrow
            }
        }
    }
}