package com.ethoel.schedule

import android.util.Log
import java.time.LocalDate

class SelectedDate {
    var date: LocalDate = LocalDate.now()
        set(value) {
            if (value != date) {
                priorDate = date
                field = value
                invokeSelectedDateChanged()
            }
        }
    var priorDate: LocalDate = date
        private set
    private var listeners: ArrayList<SelectedDateListener> = ArrayList<SelectedDateListener>(0)

    private fun invokeSelectedDateChanged() {
        listeners.forEach { listener ->
            listener.selectedDateChanged(this)
        }
    }

    fun addListener(listener: SelectedDateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SelectedDateListener) {
        listeners.remove(listener)
    }

    fun yearChanged(): Boolean {
       return date.year != priorDate.year
    }

    fun monthChanged(): Boolean {
        return date.monthValue != priorDate.monthValue
    }

    fun dayChanged(): Boolean {
        return date.dayOfMonth != priorDate.dayOfMonth
    }
}

interface SelectedDateListener {
    fun selectedDateChanged(selectedDate: SelectedDate)
}

