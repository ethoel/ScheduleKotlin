package com.ethoel.schedule

import android.content.Intent
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
import androidx.core.view.children
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.elevation.SurfaceColors
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), SelectedDateListener {

    var myDate: SelectedDate = SelectedDate().also { it.addListener(this) }
    val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build()
    lateinit var scheduleDatabase: SQLiteDatabase
    lateinit var dateRow: LinearLayout
    lateinit var datePickerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleDatabase = ScheduleDatabaseHelper(this).readableDatabase
        setContentView(R.layout.activity_main)
        initializeViewPager()
        initializeTopAppBar()
        initializeSystemNavigationBar()
        initializeDatePicker()
        initializeDateButtons()
    }

    fun initializeViewPager() {
        findViewById<ViewPager2>(R.id.schedule_view_pager).also { pager ->
            pager.adapter = SchedulePageAdapter(this, myDate.date).also { myDate.addListener(it) }
            pager.setCurrentItem(1, false)
            pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        when (pager.currentItem) {
                            SchedulePageAdapter.PREVIOUS_PAGE -> {
                                myDate.date = myDate.date.minusWeeks(1)
                                pager.setCurrentItem(SchedulePageAdapter.CURRENT_PAGE, false)
                            }
                            SchedulePageAdapter.NEXT_PAGE -> {
                                myDate.date = myDate.date.plusWeeks(1)
                                pager.setCurrentItem(SchedulePageAdapter.CURRENT_PAGE, false)
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
        datePickerButton = findViewById(R.id.date_picker_button)
        datePickerButton.setOnClickListener {
            datePicker.show(supportFragmentManager, "Picker")
        }
        updateDatePickerButton()
    }

    fun updateDatePickerButton() {
        datePickerButton.text = DateTimeFormatter.ofPattern("EEEE, d MMMM u").format(myDate.date)
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

    override fun selectedDateChanged(newDate: LocalDate) {
        updateDatePickerButton()
    }

    //fun emphasizeRow(rowIndex: Int) {
    //    if (rowIndex < scheduleViews.size - 1) {
    //        val mainConstraintLayout: ConstraintLayout = findViewById(R.id.main_constraint_layout)
    //        with(ConstraintSet()) {
    //            clone(mainConstraintLayout)
    //            connect(
    //                scheduleViews[rowIndex + 1][0]!!.id,
    //                ConstraintSet.TOP,
    //                scheduleViews[rowIndex][0]!!.id,
    //                ConstraintSet.BOTTOM,
    //                resources.getDimension(R.dimen.schedule_margin).toInt()
    //            )
    //            applyTo(mainConstraintLayout)
    //        }
    //    }
    //    for(i in scheduleViews[rowIndex].indices) {
    //        scheduleViews[rowIndex][i]!!.setTextAppearance(android.R.style.TextAppearance_Material_Body2)
    //    }
    //}

    //fun reorderRowsBy(order: Int) {
    //    when(order) {
    //        LOONEY_ORDER -> {
    //            val cursor = scheduleDatabase.rawQuery("SELECT DISTINCT anesthesiologist FROM assignments ORDER BY assignment_id", null)
    //            var looneyOrder = HashMap<String, Int>(cursor.count)
    //            cursor.moveToFirst()
    //            do {
    //                looneyOrder[cursor.getString(0)] = cursor.position + 1
    //            } while (cursor.moveToNext())
    //            cursor.close()

    //            var index = 1
    //            while (index < scheduleViews.size) {
    //                var looneyIndex = looneyOrder[scheduleViews[index][0]!!.text]
    //                if (looneyIndex!! != index)
    //                    for (i in scheduleViews[index].indices) {
    //                        var tmp = scheduleViews[index][i]!!.text
    //                        scheduleViews[index][i]!!.text = scheduleViews[looneyIndex][i]!!.text
    //                        scheduleViews[looneyIndex][i]!!.text = tmp
    //                    }
    //                else
    //                    index++
    //            }
    //        }
    //        ALPHA_LOCUM_LAST -> {
    //            var startCopying = false
    //            lateinit var previousTextView: Array<TextView?>
    //            scheduleViews.forEach { textView ->
    //                if (startCopying) {
    //                    for (i in textView.indices) {
    //                        var tmp = previousTextView[i]!!.text
    //                        previousTextView[i]!!.text = textView[i]!!.text
    //                        textView[i]!!.text = tmp
    //                    }
    //                }
    //                if (textView[0]!!.text.trim() == "Locum") {
    //                    startCopying = true
    //                }
    //                previousTextView = textView
    //            }
    //        }
    //    }
    //}

    companion object {
        const val LOONEY_ORDER = 0
        const val ALPHA_LOCUM_LAST = 1
        const val ALPHA_WITH_LOCUM = 2
    }

}