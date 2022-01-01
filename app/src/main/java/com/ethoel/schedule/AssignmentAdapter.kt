package com.ethoel.schedule

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.ArrayList

class AssignmentAdapter(): RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {
    var activity: MainActivity? = null
    private var assignments: ArrayList<Array<String>> = arrayListOf(arrayOf("", "M", "T", "W", "T", "F", "S", "S"), Array<String>(8) { "" })

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {}

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> DAY_ROW
            1 -> DATE_ROW
            else -> ASSIGNMENT_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentAdapter.ViewHolder {
        return when (viewType) {
            DAY_ROW -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.day_row, parent, false))
            DATE_ROW -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.date_row, parent, false))
            else -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.assignment_row, parent, false))
        }
    }

    override fun onBindViewHolder(holder: AssignmentAdapter.ViewHolder, position: Int) {
        assert((holder.view as ViewGroup).childCount == assignments[position].size)

        var i = 0
        (holder.view as ViewGroup).children.forEach { (it as TextView).text = assignments[position][i++]
        }
    }

    override fun getItemCount(): Int {
        return assignments.size
    }

    fun setDate(date: LocalDate) {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        assignments = assignments.take(2) as ArrayList<Array<String>>

        // update date row
        var monthText: String = DateTimeFormatter.ofPattern("MMM").format(monday)
        if (monday.month != sunday.month)
            monthText += "-${DateTimeFormatter.ofPattern("MMM").format(sunday)}"

        assignments[1][0] = monthText
        for (i in 1 until assignments[1].size)
            assignments[1][i] = monday.plusDays((i - 1).toLong()).dayOfMonth.toString()

        // update assignments
        var assignmentRows: ArrayList<Array<String>> = ArrayList(0)
        val cursor = activity!!.scheduleDatabase.rawQuery(
            "SELECT date, anesthesiologist, assignment FROM assignments WHERE date BETWEEN ? AND ? ORDER BY anesthesiologist,date",
            arrayOf(monday.toString(), sunday.toString())
        )
        if (cursor.moveToFirst()) do {
            val anesthesiologist = cursor.getString(1)
            var assignmentRow = Array<String>(8) { if (it == 0) anesthesiologist else "-" }
            for (i in 1 until assignmentRow.size) {
                val date = cursor.getString(0)
                if (monday.plusDays((i - 1).toLong()).toString() == date) {
                    assignmentRow[i] = cursor.getString(2).trim().let { if (it == "") "-" else it}
                } else
                    continue // find the first date with data
                if (!cursor.moveToNext() || cursor.getString(1) != anesthesiologist) break
            }
            assignmentRows.add(assignmentRow)
        } while (!cursor.isAfterLast)
        cursor.close()

        assignments.addAll(assignmentRows)

        notifyItemRangeChanged(0, assignments.size)
    }

    companion object {
        const val DAY_ROW = 0
        const val DATE_ROW = 1
        const val ASSIGNMENT_ROW = 2
    }
}