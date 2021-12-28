package com.ethoel.schedule

import android.app.ActionBar
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.NestedScrollView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.elevation.SurfaceColors
import java.text.FieldPosition
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), SelectedDateListener {

    var myDate: SelectedDate = SelectedDate().also { it.addListener(this) }
    var scheduleViews: ArrayList<Array<TextView?>> = ArrayList(0)
    val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
    lateinit var scheduleDatabase: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleDatabase = ScheduleDatabaseHelper(this).readableDatabase
        setContentView(R.layout.activity_main)
        initializeViewPager()
        initializeTopAppBar()
        initializeSystemNavigationBar()
        initializeDatePicker()
        initializeDateButtons()
        initializeScheduleViews()
        updateAssignments()
        Log.d("LENA", "Created")
    }

    fun initializeViewPager() {
        findViewById<ViewPager2>(R.id.schedule_view_pager).also {
            it.adapter = ScheduleAdapter(this)
            it.setCurrentItem(1, false)
            it.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    Log.d("LENA", "Position " + position)
                }

                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCROLL_STATE_IDLE || state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        when (it.currentItem) {
                            0 -> {
                                (it.adapter as ScheduleAdapter).decrement()
                                it.setCurrentItem(1, false)
                            }
                            2 -> {
                                (it.adapter as ScheduleAdapter).increment()
                                it.setCurrentItem(1, false)
                            }
                        }
                    }
                }
            })
        }
    }

    fun initializeDatePicker() {
        datePicker.addOnPositiveButtonClickListener { selected ->
            myDate.date = Instant.ofEpochMilli(selected).atOffset(ZoneOffset.UTC).toLocalDate()
        }
        val datePickerButton: Button = findViewById(R.id.date_picker_button)
        datePickerButton.text = DateTimeFormatter.ofPattern("d MMMM u").format(myDate.date)
        datePickerButton.setOnClickListener {
            datePicker.show(supportFragmentManager, "Picker")
        }
    }

    fun initializeSystemNavigationBar() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            window.navigationBarColor = SurfaceColors.SURFACE_0.getColor(this@MainActivity)
    }

    fun initializeTopAppBar() {
        val topAppBar: MaterialToolbar = findViewById(R.id.top_app_bar)
        topAppBar.title = getString(R.string.app_name) + " " + packageManager.getPackageInfo(packageName, 0).versionName
        topAppBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.update_button -> {
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ethoel.schedule")).also { startActivity(it) }
                    true
                }
                else -> false
            }
        }
    }

    fun initializeDateButtons() {
        val nextButton: Button = findViewById(R.id.next_button)
        nextButton.setOnClickListener {
            myDate.date = myDate.date.plusWeeks(1)
        }
        val prevButton: Button = findViewById(R.id.prev_button)
        prevButton.setOnClickListener {
            myDate.date = myDate.date.minusWeeks(1)
        }
    }

    fun initializeScheduleViews() {
        val cursor = scheduleDatabase.rawQuery("SELECT DISTINCT anesthesiologist FROM assignments", null)
        var rowId = R.id.textViewBlank
        repeat(cursor.count + 1) {
            scheduleViews.add(newRow(rowId, "-", Array<String>(7) { "-" }).also { rowId = it[0]!!.id })
        }
        cursor.close()
    }

    override fun selectedDateChanged(newDate: SelectedDate) {
        findViewById<Button>(R.id.date_picker_button).text = DateTimeFormatter.ofPattern("EEEE, d MMMM u").format(newDate.date)
        if (myDate.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) != myDate.priorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) {
            updateAssignments()
        }
    }

    fun updateAssignments() {
        var rowIndex: Int = 0
        val monday = myDate.date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = myDate.date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

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
            scheduleViews[rowIndex][i]!!.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
        }
    }

    fun reorderRowsBy(order: Int) {
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
        assert(index < scheduleViews.size)
        assert(assignment.size == scheduleViews[index].size - 1)
        scheduleViews[index][0]!!.text = anesthesiologist
        for (i in 1 until scheduleViews[index].size) {
            scheduleViews[index][i]!!.text = if (assignment[i - 1].trim() == "") "-" else assignment[i - 1]
        }
    }

    fun clearRowsStartingAt(startIndex: Int) {
        assert(startIndex <= scheduleViews.size)
        for (i in startIndex until scheduleViews.size)
            for (j in 1 until scheduleViews[i].size)
                scheduleViews[i][j]!!.text = "-" // TODO consider hiding view
    }

    fun newRow(rowAboveId: Int, anesthesiologist: String, assignment: Array<String>): Array<TextView?> {
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