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

class AssignmentAdapter(): RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {
    var activity: MainActivity? = null
    private var assignments: ArrayList<Array<String>> = arrayListOf(
        arrayOf( "", "M", "T", "W", "T", "F", "S", "S" ),
        arrayOf( "Date", "1", "2", "3", "4", "5", "6", "7" ))

    class ViewHolder(val view: View): RecyclerView.ViewHolder(view) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.assignment_row, parent, false))
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

        var monthText: String = DateTimeFormatter.ofPattern("MMM").format(monday)
        if (monday.month != sunday.month)
            monthText += "-${DateTimeFormatter.ofPattern("MMM").format(sunday)}"

        assignments[DATE_ROW][0] = monthText
        for (i in 1 until assignments[DATE_ROW].size) {
            assignments[DATE_ROW][i] = monday.plusDays((i - 1).toLong()).dayOfMonth.toString()
        }

        // update the rest from database
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

        for (i in assignmentRows.indices) {
            if (i + DATE_ROW + 1 < assignments.size)
                assignments[i + DATE_ROW + 1] = assignmentRows[i]
            else
                assignments.add(assignmentRows[i])
        }

        notifyItemRangeChanged(DATE_ROW, assignments.size - DATE_ROW)
    }

    companion object {
        const val DATE_ROW = 1
    }
}
