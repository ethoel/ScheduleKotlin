package com.ethoel.schedule

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.FieldPosition
import java.time.LocalDate

class SchedulePageFragment(scrollListener: RecyclerView.OnScrollListener? = null) : Fragment() {
    var schedulePageView: RecyclerView? = null
    var listener: RecyclerView.OnScrollListener? = scrollListener
    var assignmentAdapter: AssignmentAdapter = AssignmentAdapter()
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        schedulePageView = inflater.inflate(R.layout.schedule_page, container, false) as RecyclerView
        schedulePageView!!.layoutManager = object: LinearLayoutManager(this.context) {
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                //works but delayed by one cycle! need to also uncomment line in schedulePageAdapter
                //scrollToPositionWithOffset(SchedulePageAdapter.position, 0)
                //this next line is a little bit weird but works for now to move the current page back to zero TODO
                //scrollToPositionWithOffset(0, 0)
                //Log.d("LENA", "layout completed ${SchedulePageAdapter.position}")
            }
        }
        schedulePageView!!.adapter = assignmentAdapter
        listener?.let { schedulePageView!!.addOnScrollListener(it) }
        return schedulePageView!!
    }
}