package com.ethoel.schedule

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import java.io.*
import java.lang.Exception
import java.lang.RuntimeException
import java.net.URL
import kotlin.concurrent.thread

class ScheduleDatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    private val preferences: SharedPreferences = context.getSharedPreferences( "${context.packageName}.database_versions", Context.MODE_PRIVATE)
    private var listeners: ArrayList<ScheduleDatabaseListener> = ArrayList(0)
    var scheduleDatabase: SQLiteDatabase? = null
        get() {
            if (field == null) {
                field = readableDatabase
                field!!.execSQL("CREATE TABLE IF NOT EXISTS assignments (assignment_id INTEGER PRIMARY KEY, date TEXT NOT NULL, anesthesiologist TEXT NOT NULL, assignment TEXT NOT NULL, UNIQUE(date, anesthesiologist))")
            }
            return field
        }

    @Synchronized
    private fun updateDatabaseAsNeeded() {
        var onlineVersion: String = ""
        try {
            val versionReader = URL("https://pacificanesthesia.s3.us-west-2.amazonaws.com/version.txt").openConnection().run {
                        addRequestProperty("User-Agent", "PAStAnne")
                        BufferedReader(InputStreamReader(getInputStream())) }
            onlineVersion = versionReader.readLine().trim()
            versionReader.close()
            Log.d("LENA", "Version retrieved successfully")
        } catch (e: Exception) {
            invokeOnDatabaseUpdated(FAILED)
            Log.d("LENA", "Could not retrieve version number: ${e.message}")
            return
        }

        if (preferences.getString(DATABASE_NAME, "")!! < onlineVersion) {
            try {
                Log.d("LENA", "Update from old to new version")
                val inputStream: InputStream = URL("https://pacificanesthesia.s3.us-west-2.amazonaws.com/schedule.db").openConnection().run {
                    addRequestProperty("User-Agent", "PAStAnne")
                    getInputStream() }
                Log.d("LENA", "Got input stream")
                context.deleteDatabase(DATABASE_NAME)
                val outputStream = FileOutputStream(File(context.getDatabasePath(ScheduleDatabaseHelper.DATABASE_NAME).path))
                Log.d("LENA", "Got output stream")
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.flush()
                outputStream.close()
                preferences.edit().apply() {
                    putString(DATABASE_NAME, onlineVersion)
                    apply()
                }
                Log.d("LENA", "Schedule updated")
                scheduleDatabase?.close()
                scheduleDatabase = readableDatabase
                invokeOnDatabaseUpdated(UPDATED, onlineVersion)
            } catch (e: Exception) {
                Log.d("LENA", "Schedule failed to update ${e.message}")
                invokeOnDatabaseUpdated(FAILED)
            }
        } else {
            Log.d("LENA", "Already up to date")
            invokeOnDatabaseUpdated(ALREADY_UP_TO_DATE, preferences.getString(DATABASE_NAME, "")!!)
        }
    }

    private fun invokeOnDatabaseUpdated(result: Int, version: String = "") {
        listeners.forEach { it.onScheduleDatabaseUpdated(result, version) }
    }


    override fun getWritableDatabase(): SQLiteDatabase {
        throw RuntimeException("The $DATABASE_NAME database is not writable")
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        return super.getReadableDatabase()
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // nothing to do
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // nothing to do
    }

    fun addListener(listener: ScheduleDatabaseListener) {
        listeners.add(listener)
    }

    fun updateDatabase() {
        thread {  updateDatabaseAsNeeded() }
    }

    companion object {
        const val DATABASE_NAME = "schedule_database"

        const val UPDATED: Int = 1
        const val FAILED: Int = 2
        const val ALREADY_UP_TO_DATE: Int = 3
    }
}

interface ScheduleDatabaseListener {
    fun onScheduleDatabaseUpdated(result: Int, version: String)
}