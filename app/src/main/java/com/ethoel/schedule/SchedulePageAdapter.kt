package com.ethoel.schedule

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class SchedulePageAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity) {
    private var counter: Int = 0
    private var schedulePageFragments: Array<SchedulePageFragment> = arrayOf(SchedulePageFragment(-1), SchedulePageFragment(0), SchedulePageFragment(1))

    override fun getItemCount(): Int {
        return schedulePageFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return schedulePageFragments[position]
    }

    fun decrement() {
        counter--
        schedulePageFragments[0].updateView(counter - 1)
        schedulePageFragments[1].updateView(counter)
        schedulePageFragments[2].updateView(counter + 1)
        Log.d("LENA", "Decrementing $counter")
    }

    fun increment() {
        counter++
        schedulePageFragments[0].updateView(counter - 1)
        schedulePageFragments[1].updateView(counter)
        schedulePageFragments[2].updateView(counter + 1)
        Log.d("LENA", "Incrementing $counter")
    }
}