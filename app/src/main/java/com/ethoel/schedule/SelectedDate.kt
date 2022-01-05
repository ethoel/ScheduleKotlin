package com.ethoel.schedule

import android.util.Log
import java.time.LocalDate

class SelectedDate {
    var date: LocalDate = LocalDate.now()
        set(value) {
            if (value != date) {
                priorDate = date
                field = value
            }
            invokeSelectedDateChanged()
        }
    var priorDate: LocalDate = date
        private set
    private var listeners: ArrayList<SelectedDateListener> = ArrayList<SelectedDateListener>(0)

    private fun invokeSelectedDateChanged() {
        listeners.forEach { listener ->
            listener.selectedDateChanged(date)
        }
    }

    fun addListener(listener: SelectedDateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SelectedDateListener) {
        listeners.remove(listener)
    }
}

interface SelectedDateListener {
    fun selectedDateChanged(selectedDate: LocalDate)
}

