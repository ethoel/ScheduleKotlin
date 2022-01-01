package com.ethoel.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class SchedulePageFragment() : Fragment() {
    var assignmentAdapter: AssignmentAdapter = AssignmentAdapter()
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val schedulePageView: RecyclerView = inflater.inflate(R.layout.schedule_page, container, false) as RecyclerView
        schedulePageView.layoutManager = LinearLayoutManager(this.context)
        schedulePageView.adapter = assignmentAdapter
        return schedulePageView
    }
}