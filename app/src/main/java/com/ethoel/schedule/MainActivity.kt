package com.ethoel.schedule

import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
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
    var scheduleViews: ArrayList<Array<TextView?>> = ArrayList(0)
    lateinit var yearSpinner: Spinner
    lateinit var monthSpinner: Spinner
    lateinit var daySpinner: Spinner
    lateinit var scheduleDatabase: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LENA", "onCreate")
        super.onCreate(savedInstanceState)
        scheduleDatabase = ScheduleDatabaseHelper(this).readableDatabase
        setContentView(R.layout.activity_main)
        supportActionBar!!.title = getString(R.string.app_name) + " v" + packageManager.getPackageInfo(packageName, 0).versionName
        initializeDateSpinners()
        initializeDateButtons()
        initializeScheduleViews()
        updateSelectedDate()
        updateAssignments()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        Log.d("LENA", "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d("LENA", "onOptionsItemsSelected")
        when (item.itemId) {
            R.id.update_button -> Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ethoel.schedule")).also { startActivity(it) }
        }
        return super.onOptionsItemSelected(item)
    }

    fun initializeDateSpinners() {
        Log.d("LENA", "initializeDateSpinners")
        // find the views
        yearSpinner = findViewById(R.id.year_spinner)
        monthSpinner = findViewById(R.id.month_spinner)
        daySpinner = findViewById(R.id.day_spinner)

        // initialize the views
        yearSpinner.adapter = ArrayAdapter(this@MainActivity, R.layout.spinner_item, arrayOf("2021", "2022")).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        monthSpinner.adapter = ArrayAdapter(this@MainActivity, R.layout.spinner_item, arrayOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12")).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        val days = Array(selectedDate.lengthOfMonth()) { (it + 1).toString().padStart(2, '0') }
        daySpinner.adapter = ArrayAdapter(this, R.layout.spinner_item, days).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
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
        Log.d("LENA", "initializeDateButtons")
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

    fun initializeScheduleViews() {
        Log.d("LENA", "initializeScheduleViews")
        val cursor = scheduleDatabase.rawQuery("SELECT DISTINCT anesthesiologist FROM assignments", null)
        var rowId = R.id.textViewBlank
        repeat(cursor.count + 1) {
            scheduleViews.add(newRow(rowId, "-", Array<String>(7) { "-" }).also { rowId = it[0]!!.id })
        }
        cursor.close()
    }

    fun updateSelectedDate(year: Int = selectedDate.year, month: Int = selectedDate.monthValue, day: Int = selectedDate.dayOfMonth) {
        Log.d("LENA", "updateSelectedDate")
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
            daySpinner.adapter = ArrayAdapter(this, R.layout.spinner_item, days).apply {
                setDropDownViewResource(R.layout.spinner_dropdown_item)
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
        Log.d("LENA", "updateAssignments")
        var rowIndex: Int = 0
        val monday = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        // update date row
        var monthText: String = DateTimeFormatter.ofPattern("MMM").format(monday)
        if (monday.month != sunday.month)
            monthText += "-" + DateTimeFormatter.ofPattern("MMM").format(sunday)
        updateRow(rowIndex++, monthText, Array<String>(7) {
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
                } else
                    continue // find the first date with data
                if (!cursor.moveToNext() || cursor.getString(1) != anesthesiologist) break
            }

            updateRow(rowIndex++, anesthesiologist, assignments)
        } while (!cursor.isAfterLast)

        cursor.close()

        clearRowsStartingAt(rowIndex)

        reorderRowsBy(LOONEY_ORDER)
        emphasizeRow(0)
    }

    fun emphasizeRow(rowIndex: Int) {
        Log.d("LENA", "emphasizeRow")
        if (rowIndex < scheduleViews.size - 1) {
            val mainConstraintLayout: ConstraintLayout = findViewById(R.id.main_constraint_layout)
            with(ConstraintSet()) {
                clone(mainConstraintLayout)
                connect(
                    scheduleViews[rowIndex + 1][0]!!.id,
                    ConstraintSet.TOP,
                    scheduleViews[rowIndex][0]!!.id,
                    ConstraintSet.BOTTOM,
                    resources.getDimension(R.dimen.schedule_margin).toInt()
                )
                applyTo(mainConstraintLayout)
            }
        }
        for(i in scheduleViews[rowIndex].indices) {
            scheduleViews[rowIndex][i]!!.setTextColor(getColor(R.color.design_default_color_on_secondary))
        }
    }

    fun reorderRowsBy(order: Int) {
        Log.d("LENA", "reorderRowsBy")
        when(order) {
            LOONEY_ORDER -> {
                val cursor = scheduleDatabase.rawQuery("SELECT DISTINCT anesthesiologist FROM assignments ORDER BY assignment_id", null)
                var looneyOrder = HashMap<String, Int>(cursor.count)
                cursor.moveToFirst()
                do {
                    looneyOrder[cursor.getString(0)] = cursor.position + 1
                } while (cursor.moveToNext())
                cursor.close()

                var index = 1
                while (index < scheduleViews.size) {
                    var looneyIndex = looneyOrder[scheduleViews[index][0]!!.text]
                    if (looneyIndex!! != index)
                        for (i in scheduleViews[index].indices) {
                            var tmp = scheduleViews[index][i]!!.text
                            scheduleViews[index][i]!!.text = scheduleViews[looneyIndex][i]!!.text
                            scheduleViews[looneyIndex][i]!!.text = tmp
                        }
                    else
                        index++
                }
            }
            ALPHA_LOCUM_LAST -> {
                var startCopying = false
                lateinit var previousTextView: Array<TextView?>
                scheduleViews.forEach { textView ->
                    if (startCopying) {
                        for (i in textView.indices) {
                            var tmp = previousTextView[i]!!.text
                            previousTextView[i]!!.text = textView[i]!!.text
                            textView[i]!!.text = tmp
                        }
                    }
                    if (textView[0]!!.text.trim() == "Locum") {
                        startCopying = true
                    }
                    previousTextView = textView
                }
            }
        }
    }

    fun updateRow(index: Int, anesthesiologist: String, assignment: Array<String>) {
        Log.d("LENA", "updateRow")
        assert(index < scheduleViews.size)
        assert(assignment.size == scheduleViews[index].size - 1)
        scheduleViews[index][0]!!.text = anesthesiologist
        for (i in 1 until scheduleViews[index].size) {
            scheduleViews[index][i]!!.text = if (assignment[i - 1].trim() == "") "-" else assignment[i - 1]
        }
    }

    fun clearRowsStartingAt(startIndex: Int) {
        Log.d("LENA", "clearRowsStartingAt")
        assert(startIndex <= scheduleViews.size)
        for (i in startIndex until scheduleViews.size)
            for (j in 1 until scheduleViews[i].size)
                scheduleViews[i][j]!!.text = "-" // TODO consider hiding view
    }

    fun newRow(rowAboveId: Int, anesthesiologist: String, assignment: Array<String>): Array<TextView?> {
        Log.d("LENA", "newRow")
        var views = Array<TextView?>(assignment.size + 1) { null }

        val mainConstraintLayout: ConstraintLayout = findViewById(R.id.main_constraint_layout)
        var prevId: Int = View.generateViewId()
        mainConstraintLayout.addView(TextView(this@MainActivity).also { it.text = anesthesiologist; it.id = prevId; views[0] = it })
        with(ConstraintSet()) {
            clone(mainConstraintLayout)
            connect(prevId, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, resources.getDimension(R.dimen.schedule_margin).toInt())
            connect(prevId, ConstraintSet.TOP, rowAboveId, ConstraintSet.BOTTOM)
            constrainWidth(prevId, 0)
            setHorizontalWeight(prevId, 2.toFloat())
            applyTo(mainConstraintLayout)
        }

        var nextId: Int = View.generateViewId()
        repeat(assignment.size - 1) { i ->
            mainConstraintLayout.addView(TextView(this@MainActivity).also { it.text = assignment[i]; it.id = nextId; views[i + 1] = it })
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

        mainConstraintLayout.addView(TextView(this@MainActivity).also { it.text = assignment[assignment.size - 1]; it.id = nextId; views[assignment.size] = it })
        with(ConstraintSet()) {
            clone(mainConstraintLayout)
            connect(prevId, ConstraintSet.END, nextId, ConstraintSet.START)
            connect(nextId, ConstraintSet.START, prevId, ConstraintSet.END)
            connect(nextId, ConstraintSet.BOTTOM, prevId, ConstraintSet.BOTTOM)
            connect(nextId, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, resources.getDimension(R.dimen.schedule_margin).toInt())
            setHorizontalWeight(nextId, 1.toFloat())
            constrainWidth(nextId, 0)
            applyTo(mainConstraintLayout)
        }
        return views
    }

    companion object {
        const val LOONEY_ORDER = 0
        const val ALPHA_LOCUM_LAST = 1
        const val ALPHA_WITH_LOCUM = 2
    }
}