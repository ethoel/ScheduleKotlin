package com.ethoel.schedule

import android.app.ActionBar
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import java.text.FieldPosition
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    var selectedDate: LocalDate = LocalDate.now()
    var scheduleViews: ArrayList<View> = ArrayList(0)
    lateinit var yearSpinner: Spinner
    lateinit var monthSpinner: Spinner
    lateinit var daySpinner: Spinner
    lateinit var scheduleDatabase: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleDatabase = ScheduleDatabaseHelper(this).readableDatabase
        setContentView(R.layout.activity_main)
        initializeDateSpinners()
        initializeDateButtons()
        updateSelectedDate()
        updateAssignments()
    }

    fun initializeDateSpinners() {
        // find the views
        yearSpinner = findViewById(R.id.year_spinner)
        monthSpinner = findViewById(R.id.month_spinner)
        daySpinner = findViewById(R.id.day_spinner)

        // initialize the views
        yearSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, arrayOf("2021", "2022")).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        monthSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val days = Array(selectedDate.lengthOfMonth()) { (it + 1).toString().padStart(2, '0') }
        daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // set some of the initial values before setting listeners
        yearSpinner.setSelection((yearSpinner.adapter as ArrayAdapter<String>).getPosition(selectedDate.year.toString()), false)
        monthSpinner.setSelection((monthSpinner.adapter as ArrayAdapter<String>).getPosition(selectedDate.monthValue.toString().padStart(2,'0')), false)
        daySpinner.setSelection((daySpinner.adapter as ArrayAdapter<String>).getPosition(selectedDate.dayOfMonth.toString().padStart(2, '0')), false)

        // set the listeners
        yearSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newYear = (parent!!.getItemAtPosition(position) as String).toInt()
                if (newYear != selectedDate.year) updateSelectedDate(year = newYear)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        monthSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newMonth = (parent!!.getItemAtPosition(position) as String).toInt()
                if (newMonth != selectedDate.monthValue) updateSelectedDate(month = newMonth)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
        daySpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newDay = (parent!!.getItemAtPosition(position) as String).toInt()
                if (newDay != selectedDate.dayOfMonth) updateSelectedDate(day = newDay)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    fun initializeDateButtons() {
        val nextButton: Button = findViewById(R.id.next_button)
        nextButton.setOnClickListener {
            val next = selectedDate.plusWeeks(1)
            updateSelectedDate(next.year, next.monthValue, next.dayOfMonth)
        }
        val prevButton: Button = findViewById(R.id.prev_button)
        prevButton.setOnClickListener {
            val prev = selectedDate.minusWeeks(1)
            updateSelectedDate(prev.year, prev.monthValue, prev.dayOfMonth)
        }
    }

    @Synchronized
    fun clearScheduleViews() {
        val mainConstraintLayout: ConstraintLayout = findViewById(R.id.main_constraint_layout)
        scheduleViews.forEach {
            mainConstraintLayout.removeView(it)
        }
        scheduleViews.clear()
    }

    fun updateSelectedDate(year: Int = selectedDate.year, month: Int = selectedDate.monthValue, day: Int = selectedDate.dayOfMonth) {
        // update the member variable selectedDate appropriately and update days array for daySpinner if needed
        val oldSelectedDate = selectedDate
        val lengthOfPriorMonth = oldSelectedDate.lengthOfMonth()
        val lengthOfMonth = LocalDate.of(year, month, 1).lengthOfMonth()
        selectedDate = if (day > lengthOfMonth) {
            LocalDate.of(year, month, lengthOfMonth)
        } else {
            LocalDate.of(year, month, day)
        }
        if (lengthOfMonth != lengthOfPriorMonth) {
            val days = Array(lengthOfMonth) { (it + 1).toString().padStart(2, '0') }
            daySpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }
        // update the spinners to reflect the new selectedDate if different
        if ((yearSpinner.selectedItem as String).toInt() != selectedDate.year)
            yearSpinner.setSelection((yearSpinner.adapter as ArrayAdapter<String>).getPosition(selectedDate.year.toString()))
        if ((monthSpinner.selectedItem as String).toInt() != selectedDate.monthValue)
            monthSpinner.setSelection((monthSpinner.adapter as ArrayAdapter<String>).getPosition(selectedDate.monthValue.toString().padStart(2, '0')))
        if ((daySpinner.selectedItem as String).toInt() != selectedDate.dayOfMonth)
            daySpinner.setSelection((daySpinner.adapter as ArrayAdapter<String>).getPosition(selectedDate.dayOfMonth.toString().padStart(2, '0')))

        // TODO: selectedDate needs to have listeners--that is how this really should be organized probably
        // update assignments only if week of selected date has changed
        if (selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) != oldSelectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) {
            updateAssignments()
        }
    }


    fun updateAssignments() {
        //Toast.makeText(this@MainActivity, selectedDate.toString(), Toast.LENGTH_SHORT).show()

        clearScheduleViews()

        val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        // update date row
        var monthText: String = DateTimeFormatter.ofPattern("MMM").format(monday)
        if (monday.month != sunday.month)
            monthText += "-" + DateTimeFormatter.ofPattern("MMM").format(sunday)
        var rowId = addRow(R.id.textViewBlank, monthText, Array<String>(7) {
            monday.plusDays(it.toLong()).dayOfMonth.toString()
        })

        val cursor = scheduleDatabase.rawQuery(
            "SELECT date, anesthesiologist, assignment FROM assignments WHERE date BETWEEN ? AND ? ORDER BY anesthesiologist,date",
            arrayOf(monday.toString(), sunday.toString())
        )
        if (cursor.moveToFirst()) do {
            val anesthesiologist = cursor.getString(1)
            var assignments = Array<String>(7) { "-" }
            for (i in assignments.indices) {
                val date = cursor.getString(0)
                if (monday.plusDays(i.toLong()).toString() == date) {
                    assignments[i] = cursor.getString(2)
                }
                if (!cursor.moveToNext() || cursor.getString(1) != anesthesiologist) break
            }

            //rowId = addRow(rowId, anesthesiologist, assignments)
            //Log.d("LENA", "Data: $anesthesiologist ${assignments.contentToString()}")
        } while (!cursor.isAfterLast)
    }

    fun addRow(rowAboveId: Int, anesthesiologist: String, assignment: Array<String>): Int {
        val mainConstraintLayout: ConstraintLayout = findViewById(R.id.main_constraint_layout)
        var prevId: Int = View.generateViewId()
        mainConstraintLayout.addView(TextView(this@MainActivity).also { it.text = anesthesiologist; it.id = prevId; scheduleViews.add(it) })
        with(ConstraintSet()) {
            clone(mainConstraintLayout)
            connect(prevId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(prevId, ConstraintSet.TOP, rowAboveId, ConstraintSet.BOTTOM)
            constrainWidth(prevId, 0)
            setHorizontalWeight(prevId, 2.toFloat())
            applyTo(mainConstraintLayout)
        }
        val rowId = prevId

        var nextId: Int = View.generateViewId()
        repeat(assignment.size - 1) { i ->
            mainConstraintLayout.addView(TextView(this@MainActivity).also { it.text = assignment[i]; it.id = nextId; scheduleViews.add(it) })
            with(ConstraintSet()) {
                clone(mainConstraintLayout)
                connect(prevId, ConstraintSet.END, nextId, ConstraintSet.START)
                connect(nextId, ConstraintSet.START, prevId, ConstraintSet.END)
                connect(nextId, ConstraintSet.BOTTOM, prevId, ConstraintSet.BOTTOM)
                constrainWidth(nextId, 0)
                setHorizontalWeight(nextId, 1.toFloat())
                applyTo(mainConstraintLayout)
            }
            prevId = nextId
            nextId = View.generateViewId()
        }

        mainConstraintLayout.addView(TextView(this@MainActivity).also { it.text = assignment[assignment.size - 1]; it.id = nextId; scheduleViews.add(it) })
        with(ConstraintSet()) {
            clone(mainConstraintLayout)
            connect(prevId, ConstraintSet.END, nextId, ConstraintSet.START)
            connect(nextId, ConstraintSet.START, prevId, ConstraintSet.END)
            connect(nextId, ConstraintSet.BOTTOM, prevId, ConstraintSet.BOTTOM)
            connect(nextId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            setHorizontalWeight(nextId, 1.toFloat())
            constrainWidth(nextId, 0)
            applyTo(mainConstraintLayout)
        }
        return rowId
    }
}