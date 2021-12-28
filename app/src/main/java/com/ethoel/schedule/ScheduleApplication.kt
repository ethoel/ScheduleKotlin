package com.ethoel.schedule

import android.app.Application
import com.google.android.material.color.DynamicColors

class ScheduleApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}