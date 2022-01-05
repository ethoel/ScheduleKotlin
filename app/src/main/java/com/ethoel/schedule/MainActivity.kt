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
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
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

class MainActivity : AppCompatActivity(), SelectedDateListener {

    var myDate: SelectedDate = SelectedDate().also { it.addListener(this) }
    lateinit var scheduleDatabase: SQLiteDatabase
    lateinit var datePickerButton: Button
    lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleDatabase = ScheduleDatabaseHelper(this).readableDatabase
        setContentView(R.layout.activity_main)
        initializeViewPager()
        initializeTopAppBar()
        initializeSystemNavigationBar()
        initializeDatePicker()
        initializeDateButtons()

        myDate.date = LocalDate.now()
    }

    fun initializeViewPager() {
        viewPager = findViewById<ViewPager2>(R.id.schedule_view_pager).also { pager ->
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
                    thread {
                        try {
                            // where should this code go? clearly no need to ask for another readable database TODO
                            // how to make this not completely public? TODO
                            val inputStream = URL("https://pacificanesthesia.s3.us-west-2.amazonaws.com/schedule.db").openStream()
                            val outputFile = File(getDatabasePath(ScheduleDatabaseHelper.DATABASE_NAME).path)
                            val outputStream = FileOutputStream(outputFile)
                            inputStream.copyTo(outputStream)
                            inputStream.close()
                            outputStream.flush()
                            outputStream.close()
                            Log.d("LENA", "Schedule updated")
                            runOnUiThread {
                                myDate.date = myDate.date
                                Toast.makeText(this, "Schedule updated", Toast.LENGTH_SHORT).show()
                            }
                        } catch (exception: Exception) {
                            Log.d("LENA", "Schedule failed to update")
                            runOnUiThread {
                                Toast.makeText (this, "Schedule update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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


}