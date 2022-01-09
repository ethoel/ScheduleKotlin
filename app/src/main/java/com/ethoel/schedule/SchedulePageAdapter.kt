package com.ethoel.schedule

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.time.LocalDate

class SchedulePageAdapter(activity: AppCompatActivity, date: LocalDate): FragmentStateAdapter(activity), SelectedDateListener  {
    private var previousPage = SchedulePageFragment().apply { assignmentAdapter.scheduleDatabaseHelper = (activity as MainActivity).scheduleDatabaseHelper; assignmentAdapter.setDate(date.minusWeeks(1))  }
    private var nextPage = SchedulePageFragment().apply { assignmentAdapter.scheduleDatabaseHelper = (activity as MainActivity).scheduleDatabaseHelper; assignmentAdapter.setDate(date.plusWeeks(1))  }
    private var schedulePageFragments: Array<SchedulePageFragment> =
    arrayOf(
        previousPage,
        SchedulePageFragment(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                //position = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        }).apply { assignmentAdapter.scheduleDatabaseHelper = (activity as MainActivity).scheduleDatabaseHelper; assignmentAdapter.setDate(date) },
        nextPage)

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
        var position = 0
        var offset = 0
    }

    override fun selectedDateChanged(selectedDate: LocalDate) {
        schedulePageFragments[PREVIOUS_PAGE].assignmentAdapter.setDate(selectedDate.minusWeeks(1))
        schedulePageFragments[CURRENT_PAGE].assignmentAdapter.setDate(selectedDate)
        schedulePageFragments[NEXT_PAGE].assignmentAdapter.setDate(selectedDate.plusWeeks(1))
    }
}