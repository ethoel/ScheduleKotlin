package com.ethoel.schedule

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
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
import java.io.*
import java.lang.Exception
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), SelectedDateListener, ScheduleDatabaseListener {

    var myDate: SelectedDate = SelectedDate().also { it.addListener(this) }
    lateinit var scheduleDatabaseHelper: ScheduleDatabaseHelper
    lateinit var datePickerButton: Button
    lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scheduleDatabaseHelper = ScheduleDatabaseHelper(this).also {
            it.addListener(this)
            it.updateDatabase(false)
        }
        initializeViewPager(savedInstanceState)
        initializeTopAppBar()
        initializeSystemNavigationBar()
        initializeDatePicker()
        initializeDateButtons()

        if (savedInstanceState != null) {
            myDate.date = LocalDate.parse(savedInstanceState.getString("selected_date"))
        } else {
            myDate.date = LocalDate.now()
        }
        Log.d("LENA", "onCreate()")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selected_date", myDate.date.toString())
    }

    //override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    //    super.onRestoreInstanceState(savedInstanceState)
    //    myDate = SelectedDate().also { it.addListener(this) }
    //    scheduleDatabaseHelper = ScheduleDatabaseHelper(this).also {
    //        it.addListener(this)
    //        it.restoreDatabase()
    //    }
    //    myDate.date = LocalDate.parse(savedInstanceState.getString("selected_date"))
    //}

    fun initializeViewPager(savedInstanceState: Bundle?) {
        viewPager = findViewById<ViewPager2>(R.id.schedule_view_pager).also { pager ->
            //pager.adapter = SchedulePageAdapter(this, myDate.date).also { myDate.addListener(it) }
            pager.adapter = WeekPageAdapter(this, myDate.date).also { myDate.addListener(it) }
            pager.setCurrentItem(1, false)
            pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        when (pager.currentItem) {
                            WeekPageAdapter.PREVIOUS_PAGE -> {
                                myDate.date = myDate.date.minusWeeks(1)
                                pager.setCurrentItem(WeekPageAdapter.CURRENT_PAGE, false)
                            }
                            WeekPageAdapter.NEXT_PAGE -> {
                                myDate.date = myDate.date.plusWeeks(1)
                                pager.setCurrentItem(WeekPageAdapter.CURRENT_PAGE, false)
                            }
                        }
                    }
                }
            })
        }
    }

    fun initializeDatePicker() {
        datePickerButton = findViewById(R.id.date_picker_button)
        datePickerButton.setOnClickListener {
            //TODO is this really the best way about it
            MaterialDatePicker.Builder.datePicker().setSelection(myDate.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()).build().apply {
                addOnPositiveButtonClickListener { selected ->
                    myDate.date = Instant.ofEpochMilli(selected).atOffset(ZoneOffset.UTC).toLocalDate()
                }
                show(supportFragmentManager, "Picker")
            }
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
                    scheduleDatabaseHelper.updateDatabase(true)
                    true
                }
                R.id.today_button -> {
                    myDate.date = LocalDate.now()
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

    override fun onScheduleDatabaseUpdated(verbose: Boolean, result: Int, version: String) {
        runOnUiThread {
            when (result) {
                ScheduleDatabaseHelper.UPDATED -> {
                    Toast.makeText(this, "Now up to date as of $version", Toast.LENGTH_SHORT).show()
                    myDate.date = myDate.date
                }
                ScheduleDatabaseHelper.FAILED -> {
                    if (verbose)
                        Toast.makeText(this, "Could not establish connection", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    if (verbose)
                        Toast.makeText(this, "Up to date as of $version", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}