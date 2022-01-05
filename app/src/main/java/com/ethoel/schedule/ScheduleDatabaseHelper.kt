package com.ethoel.schedule

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.RuntimeException
import java.net.URL

class ScheduleDatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val preferences: SharedPreferences = context.getSharedPreferences( "${context.packageName}.database_versions", Context.MODE_PRIVATE)

    private fun installedDatabaseIsOutdated(): Boolean {
        return preferences.getInt(DATABASE_NAME, 0) < DATABASE_VERSION
    }

    private fun writeDatabaseVersionInPreferences() {
        preferences.edit().apply() {
            putInt(DATABASE_NAME, DATABASE_VERSION)
            apply()
        }
    }

    @Synchronized
    private fun installOrUpdateIfNecessary() {
        if(installedDatabaseIsOutdated()) {
            context.deleteDatabase(DATABASE_NAME)
            if (!installDatabaseFromHttp()) installDatabaseFromAssets()
            writeDatabaseVersionInPreferences()
        }
    }

    private fun installDatabaseFromAssets() {
        val inputStream = context.assets.open("$ASSETS_PATH/$DATABASE_NAME.db")

        try {
            val outputFile = File(context.getDatabasePath(DATABASE_NAME).path)
            val outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.flush()
            outputStream.close()
        } catch (exception: Throwable) {
            throw RuntimeException("The $DATABASE_NAME database could not be installed.", exception)
        }
    }

    private fun installDatabaseFromHttp(): Boolean {
        try {
            val inputStream =
                URL("https://pacificanesthesia.s3.us-west-2.amazonaws.com/schedule.db").openStream()
            val outputFile = File(context.getDatabasePath(ScheduleDatabaseHelper.DATABASE_NAME).path)
            val outputStream = FileOutputStream(outputFile)
            Log.d("LENA", "copying file")
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            Toast.makeText(context, "Schedule updated", Toast.LENGTH_SHORT).show()
            return true
        } catch (exception: Exception) {
            Toast.makeText(context, "Schedule update failed", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    override fun getWritableDatabase(): SQLiteDatabase {
        throw RuntimeException("The $DATABASE_NAME database is not writable")
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        installOrUpdateIfNecessary()
        return super.getReadableDatabase()
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // nothing to do
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // nothing to do
    }

    companion object {
        const val ASSETS_PATH = "databases"
        const val DATABASE_NAME = "schedule"
        const val DATABASE_VERSION = 5
    }
}