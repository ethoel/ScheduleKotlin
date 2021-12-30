package com.ethoel.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SchedulePageFragment(var counter: Int) : Fragment() {
    private var assignmentAdapter: AssignmentAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val schedulePageView: RecyclerView = inflater.inflate(R.layout.schedule_page, container, false) as RecyclerView
        schedulePageView.layoutManager = LinearLayoutManager(this.context)
        schedulePageView.adapter = AssignmentAdapter(arrayListOf(
            arrayOf("", "M", "T", "W", "T", "F", "S", "S"),
            arrayOf("Date", "1", "2", "3", "4", "5", "6", "7"),
            arrayOf("Hoel", "1", "2", "3", "4", "5", "6", "7"),
            arrayOf("Irene", "1", "2", "3", "4", "5", "6", "7"),
            arrayOf("Elaine", "1", "2", "3", "4", "5", "6", "7"),
            arrayOf("Zach", "1", "2", "3", "4", "5", "6", "7"),
            arrayOf("Lena", "1", "2", "3", "4", "5", "6", "7")))
        assignmentAdapter = schedulePageView.adapter as AssignmentAdapter
        return schedulePageView
    }

    fun updateView(counter: Int) {
        this.counter = counter
    }
}