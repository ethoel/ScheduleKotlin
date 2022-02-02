package com.ethoel.schedule

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.ArrayList

class AssignmentAdapter(val context: MainActivity): RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {
    var todayColumn: Int = -1
    var selectedColumn: Int = -1
    var selectedRow: Int = -1
    var scheduleDatabaseHelper: ScheduleDatabaseHelper? = null
    private var assignments: ArrayList<Array<String>> = arrayListOf(arrayOf("", "M", "T", "W", "T", "F", "S", "S"), Array<String>(8) { "" })

    class ViewHolder(val view: View, val row: Int, val assignmentAdapter: AssignmentAdapter): RecyclerView.ViewHolder(view) {
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> DAY_ROW
            1 -> DATE_ROW
            else -> position
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentAdapter.ViewHolder {
        return when (viewType) {
            DAY_ROW -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.day_row, parent, false), viewType, this)
            DATE_ROW -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.date_row, parent, false), viewType, this)
            else -> ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.assignment_row, parent, false), viewType, this)
        }.also {
            (it.view as ViewGroup).children.forEachIndexed { index, textView ->
                textView.setOnClickListener {
                    selectedRow = when {
                        index < 1 && viewType < 2 -> -1
                        viewType < 2 -> selectedRow
                        viewType == selectedRow && index < 1 -> -1
                        else -> viewType
                    }
                    selectedColumn = when {
                        index < 1 && viewType < 2 -> -1
                        index < 1 -> selectedColumn
                        index == selectedColumn && viewType < 2 -> -1
                        else -> index
                    }
                    notifyDataSetChanged()
                    Log.d("LENA", "selected row $selectedRow col $selectedColumn")
                }
            }
        }
    }

    override fun onBindViewHolder(holder: AssignmentAdapter.ViewHolder, position: Int) {
        assert((holder.view as ViewGroup).childCount == assignments[position].size)

        (holder.view as ViewGroup).children.forEachIndexed { index, view ->
            (view as TextView).text = assignments[position][index]

            when {
                index == selectedColumn && position == selectedRow -> {
                    colorSelectedIntersection(view, assignments.size - 1 == position, holder.view.childCount - 1 == index)
                }
                index == todayColumn && (index == selectedColumn || position == selectedRow) -> {
                    colorSelectedTodayCol(view, position, holder.view.childCount - 1 == index)
                }
                position == selectedRow -> {
                    colorSelectedRow(view, holder.view, index)
                }
                index == selectedColumn -> {
                    colorSelectedCol(view, position)
                }
                index == todayColumn -> {
                    colorTodayCol(view, position)
                }
                else -> {
                    uncolor(view)
                }
            }
        }
    }

    fun colorSelectedIntersection(view: TextView, isLastRow: Boolean, isLastColumn: Boolean) {
        view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSecondary, Color.WHITE))
        when {
            isLastColumn && isLastRow -> {
                view.setBackgroundResource(R.drawable.selected_intersection_bottomright)
            }
            isLastRow -> {
                view.setBackgroundResource(R.drawable.selected_intersection_bottom)
            }
            isLastColumn -> {
                view.setBackgroundResource(R.drawable.selected_intersection_right)
            }
            else -> {
                view.setBackgroundResource(R.drawable.selected_intersection_middle)
            }
        }
    }

    fun colorSelectedTodayCol(view: TextView, row: Int, isLastColumn: Boolean) {
        when (row) {
            0 -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnPrimary, Color.BLACK))
                view.setBackgroundResource(R.drawable.today_top)
            }
            1 -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnPrimary, Color.BLACK))
                view.setBackgroundResource(R.drawable.today_bottom)
            }
            selectedRow -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnTertiaryContainer, Color.BLACK))
                if (isLastColumn)
                    view.setBackgroundResource(R.drawable.selected_horizontal_right)
                else
                    view.setBackgroundResource(R.drawable.selected_horizontal_middle)
            }
            assignments.size - 1 -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnTertiaryContainer, Color.BLACK))
                view.setBackgroundResource(R.drawable.selected_vertical_bottom)
            }
            else -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnTertiaryContainer, Color.BLACK))
                view.setBackgroundResource(R.drawable.selected_vertical_middle)
            }
        }
    }

    fun colorTodayCol(view: TextView, row: Int) {
        when (row) {
            0 -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnPrimary, Color.BLACK))
                view.setBackgroundResource(R.drawable.today_top)
            }
            1 -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnPrimary, Color.BLACK))
                view.setBackgroundResource(R.drawable.today_bottom_rounded)
            }
            else -> {
                view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurface, Color.BLACK))
                view.setBackgroundColor(MaterialColors.getColor(context, R.attr.colorSurface, Color.WHITE))
            }
        }
    }

    fun colorSelectedCol(view: TextView, row: Int) {
        view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnTertiaryContainer, Color.BLACK))
        when (row) {
            0 -> {
                view.setBackgroundResource(R.drawable.selected_vertical_top)
            }
            assignments.size - 1 -> {
                view.setBackgroundResource(R.drawable.selected_vertical_bottom)
            }
            else -> {
                view.setBackgroundResource(R.drawable.selected_vertical_middle)
            }
        }
    }

    fun colorSelectedRow(view: TextView, viewGroup: ViewGroup, col: Int) {
        view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnTertiaryContainer, Color.BLACK))
        when (col) {
            0 -> {
                view.setBackgroundResource(R.drawable.selected_horizontal_left)
            } viewGroup.childCount - 1 -> {
                view.setBackgroundResource(R.drawable.selected_horizontal_right)
            } else -> {
                view.setBackgroundResource(R.drawable.selected_horizontal_middle)
            }
        }
    }

    fun uncolor(view: TextView) {
        view.setTextColor(MaterialColors.getColor(context, R.attr.colorOnSurface, Color.BLACK))
        view.setBackgroundColor(MaterialColors.getColor(context, R.attr.colorSurface, Color.WHITE))
    }

    override fun getItemCount(): Int {
        return assignments.size
    }

    fun setDate(date: LocalDate) {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        todayColumn = LocalDate.now().let { today ->
            if (today in monday..sunday) {
                //Log.d("LENA", "ChronoUnit ${ChronoUnit.DAYS.between(monday, today).toInt() + 1}")
                ChronoUnit.DAYS.between(monday, today).toInt() + 1
            } else {
                -1
            }
        }

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
        //Log.d("LENA", "about to get shit from the database")
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
        Log.d("LENA", "got shit from the database ${assignmentRows.size} for ${date.toString()}")

        if (assignmentRows.size == 0 && assignments.size > 2) {
            assignmentRows = assignments.drop(2) as ArrayList<Array<String>>
            for (i in assignmentRows.indices)
                for (j in 1 until assignmentRows[i].size)
                    assignmentRows[i][j] = "-"
        }

        reorderRowsBy(LOONEY_ORDER, assignmentRows)

        headerRows.addAll(assignmentRows)
        assignments = headerRows

        //assignments.forEach {
        //    Log.d("LENA", "${it[0]}")
        //}

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

        const val LOONEY_ORDER = 0
        const val ALPHA_LOCUM_LAST = 1
        const val ALPHA_WITH_LOCUM = 2
    }
}