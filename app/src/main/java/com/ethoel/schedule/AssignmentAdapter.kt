package com.ethoel.schedule

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
    var scheduleDatabaseHelper: ScheduleDatabaseHelper? = null
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

        var headerRows = assignments.take(2) as ArrayList<Array<String>>

        // update date row
        var monthText: String = DateTimeFormatter.ofPattern("MMM").format(monday)
        if (monday.month != sunday.month)
            monthText += "-${DateTimeFormatter.ofPattern("MMM").format(sunday)}"

        headerRows[1][0] = monthText
        for (i in 1 until headerRows[1].size)
            headerRows[1][i] = monday.plusDays((i - 1).toLong()).dayOfMonth.toString()

        // update assignments
        var assignmentRows: ArrayList<Array<String>> = ArrayList(assignments.size - 2)
        Log.d("LENA", "about to shit from the database")
        if (scheduleDatabaseHelper != null) {
            val cursor = scheduleDatabaseHelper!!.scheduleDatabase!!.rawQuery(
                "SELECT date, anesthesiologist, assignment FROM assignments WHERE date BETWEEN ? AND ? ORDER BY anesthesiologist,date",
                arrayOf(monday.toString(), sunday.toString())
            )
            if (cursor.moveToFirst()) do {
                val anesthesiologist = cursor.getString(1)
                var assignmentRow = Array<String>(8) { if (it == 0) anesthesiologist else "-" }
                for (i in 1 until assignmentRow.size) {
                    val date = cursor.getString(0)
                    if (monday.plusDays((i - 1).toLong()).toString() == date) {
                        assignmentRow[i] =
                            cursor.getString(2).trim().let { if (it == "") "-" else it }
                    } else
                        continue // find the first date with data
                    if (!cursor.moveToNext() || cursor.getString(1) != anesthesiologist) break
                }
                assignmentRows.add(assignmentRow)
            } while (!cursor.isAfterLast)
            cursor.close()
        }
        Log.d("LENA", "got shit from the database ${assignmentRows.size}")

        if (assignmentRows.size == 0 && assignments.size > 2) {
            assignmentRows = assignments.drop(2) as ArrayList<Array<String>>
            for (i in assignmentRows.indices)
                for (j in 1 until assignmentRows[i].size)
                    assignmentRows[i][j] = "-"
        }

        reorderRowsBy(LOONEY_ORDER, assignmentRows)

        headerRows.addAll(assignmentRows)
        assignments = headerRows

        notifyDataSetChanged()
    }

    private fun reorderRowsBy(order: Int, assignmentRows: ArrayList<Array<String>>) {
        if (assignmentRows.size < 2) return
        when(order) {
            LOONEY_ORDER -> {
                if (scheduleDatabaseHelper != null) {
                    val cursor = scheduleDatabaseHelper!!.scheduleDatabase!!.rawQuery(
                        "SELECT DISTINCT anesthesiologist FROM assignments ORDER BY assignment_id",
                        null
                    )
                    var looneyOrder = HashMap<String, Int>(cursor.count)
                    cursor.moveToFirst()
                    do {
                        looneyOrder[cursor.getString(0)] = cursor.position
                    } while (cursor.moveToNext())
                    cursor.close()

                    var index = 0
                    while (index < assignmentRows.size) {
                        var looneyIndex = looneyOrder[assignmentRows[index][0]]
                        assert(looneyIndex!! < assignmentRows.size)
                        if (looneyIndex != index)
                            for (i in assignmentRows[index].indices) {
                                var tmp = assignmentRows[index][i]
                                assignmentRows[index][i] = assignmentRows[looneyIndex!!][i]
                                assignmentRows[looneyIndex!!][i] = tmp
                            }
                        else
                            index++
                    }
                }
            }
            ALPHA_LOCUM_LAST -> {
                var startCopying = false
                lateinit var previousRow: Array<String>
                assignmentRows.forEach { row ->
                    if (startCopying) {
                        for (i in row.indices) {
                            var tmp = previousRow[i]
                            previousRow[i] = row[i]
                            row[i] = tmp
                        }
                    }
                    if (row[0].trim() == "Locum") {
                        startCopying = true
                    }
                    previousRow = row
                }
            }
        }
    }

    companion object {
        const val DAY_ROW = 0
        const val DATE_ROW = 1
        const val ASSIGNMENT_ROW = 2

        const val LOONEY_ORDER = 0
        const val ALPHA_LOCUM_LAST = 1
        const val ALPHA_WITH_LOCUM = 2
    }
}