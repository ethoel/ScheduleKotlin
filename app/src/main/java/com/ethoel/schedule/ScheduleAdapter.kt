package com.ethoel.schedule

import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ScheduleAdapter(activity: AppCompatActivity): FragmentStateAdapter(activity) {
    private var counter: Int = 0
    private var scheduleFragments: Array<ScheduleFragment> = arrayOf(ScheduleFragment(-1), ScheduleFragment(0), ScheduleFragment(1))

    override fun getItemCount(): Int {
        return scheduleFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return scheduleFragments[position]
    }

    fun decrement() {
        counter--
        scheduleFragments[0].updateView(counter - 1)
        scheduleFragments[1].updateView(counter)
        scheduleFragments[2].updateView(counter + 1)
        Log.d("LENA", "Decrementing $counter")
    }

    fun increment() {
        counter++
        scheduleFragments[0].updateView(counter - 1)
        scheduleFragments[1].updateView(counter)
        scheduleFragments[2].updateView(counter + 1)
        Log.d("LENA", "Incrementing $counter")
    }
}