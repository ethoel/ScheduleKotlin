package com.ethoel.schedule

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.time.LocalDate

class SchedulePageAdapter(activity: AppCompatActivity, date: LocalDate): FragmentStateAdapter(activity), SelectedDateListener {
    private var schedulePageFragments: Array<SchedulePageFragment> =
    arrayOf(
        SchedulePageFragment().apply { assignmentAdapter.activity = activity as MainActivity; assignmentAdapter.setDate(date.minusWeeks(1))  },
        SchedulePageFragment().apply { assignmentAdapter.activity = activity as MainActivity; assignmentAdapter.setDate(date) },
        SchedulePageFragment().apply { assignmentAdapter.activity = activity as MainActivity; assignmentAdapter.setDate(date.plusWeeks(1)) })

    override fun getItemCount(): Int {
        return schedulePageFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return schedulePageFragments[position]
    }

    companion object {
        const val PREVIOUS_PAGE: Int = 0
        const val CURRENT_PAGE: Int = 1
        const val NEXT_PAGE: Int = 2
    }

    override fun selectedDateChanged(selectedDate: LocalDate) {
        schedulePageFragments[PREVIOUS_PAGE].assignmentAdapter.setDate(selectedDate.minusWeeks(1))
        schedulePageFragments[CURRENT_PAGE].assignmentAdapter.setDate(selectedDate)
        schedulePageFragments[NEXT_PAGE].assignmentAdapter.setDate(selectedDate.plusWeeks(1))
    }
}