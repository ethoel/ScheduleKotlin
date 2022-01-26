package com.ethoel.schedule

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate

class WeekPageAdapter(activity: MainActivity, date: LocalDate): RecyclerView.Adapter<WeekPageAdapter.ViewHolder>(), SelectedDateListener {
    private var pages: ArrayList<AssignmentAdapter> = arrayListOf(
        AssignmentAdapter(activity).apply { scheduleDatabaseHelper = activity.scheduleDatabaseHelper; setDate(date.minusWeeks(1)) },
        AssignmentAdapter(activity).apply { scheduleDatabaseHelper = activity.scheduleDatabaseHelper; setDate(date) },
        AssignmentAdapter(activity).apply { scheduleDatabaseHelper = activity.scheduleDatabaseHelper; setDate(date.plusWeeks(1)) }
    )
    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.schedule_page, parent, false))
        //return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.testlayout, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.view as RecyclerView).run {
            layoutManager = LinearLayoutManager(context)
            adapter = pages[position]
        }
        //Log.d("LENA", "onBind $position")
        //(holder.view as ViewGroup).children.forEach { (it as TextView).text = position.toString() }
    }

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun selectedDateChanged(selectedDate: LocalDate) {
        pages[PREVIOUS_PAGE].setDate(selectedDate.minusWeeks(1))
        pages[CURRENT_PAGE].setDate(selectedDate)
        pages[NEXT_PAGE].setDate(selectedDate.plusWeeks(1))
    }

    companion object {
        const val PREVIOUS_PAGE: Int = 0
        const val CURRENT_PAGE: Int = 1
        const val NEXT_PAGE: Int = 2
    }
}