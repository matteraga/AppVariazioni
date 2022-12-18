package me.matteo.appvariazioni.fragments

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import me.matteo.appvariazioni.R
import me.matteo.appvariazioni.classes.Day
import me.matteo.appvariazioni.classes.Utils
import java.text.SimpleDateFormat
import java.util.*


class TimetableFragment : Fragment(R.layout.fragment_timetable) {

    private val utils = Utils()

    fun updateTabsName() {
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

        var formattedDay = utils.dayName(Day.TODAY)
        formattedDay = formattedDay[0].uppercase() + formattedDay.drop(1)
        if (formattedDay[formattedDay.lastIndex] == 'i') {
            formattedDay = formattedDay.dropLast(1) + "ì"
        }

        val tabLayout = requireView().findViewById<TabLayout>(R.id.tabLayoutTimetable)
        val today = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(0) as TabLayout.TabView
        today.tab?.text = formattedDay + " - " + SimpleDateFormat("dd/MM", Locale.ITALY).format(currentTime.timeInMillis)

        formattedDay = utils.dayName(Day.TOMORROW)
        formattedDay = formattedDay[0].uppercase() + formattedDay.drop(1)
        if (formattedDay[formattedDay.lastIndex] == 'i') {
            formattedDay = formattedDay.dropLast(1) + "ì"
        }

        val tomorrow = (tabLayout.getChildAt(0) as ViewGroup).getChildAt(1) as TabLayout.TabView
        tomorrow.tab?.text = formattedDay + " - " + SimpleDateFormat("dd/MM", Locale.ITALY).format(tomorrowTime.timeInMillis)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerTimetable)
        val adapter = TimetablePagerAdapter(requireActivity())
        viewPager.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutTimetable)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Oggi - "
                1 -> tab.text = "Domani - "
            }
        }.attach()

        updateTabsName()
    }

    private inner class TimetablePagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(
        fragmentActivity
    ) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {

            return if (position == 0) {
                val today = DayTimetableFragment()
                val todayBundle = Bundle()
                todayBundle.putInt("day", Day.TODAY)
                today.arguments = todayBundle
                today
            } else {
                val tomorrow = DayTimetableFragment()
                val tomorrowBundle = Bundle()
                tomorrowBundle.putInt("day", Day.TOMORROW)
                tomorrow.arguments = tomorrowBundle
                tomorrow
            }
        }
    }
}