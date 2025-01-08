package com.example.caffeinated

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.caffeinated.models.CaffeineDrink
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class MyDatabaseManager(
    context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "MyCaffeineDB"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "HISTORY"
        const val COLUMN_DATE = "date"
        const val COLUMN_TIME = "time"
        const val COLUMN_ENTRY = "entry"
        const val COLUMN_CALORIES = "calories"
        const val COLUMN_VOLUME = "volume"
        const val COLUMN_NAME = "name"
        const val COLUMN_TYPE = "type"
        const val COLUMN_TIME_CONSUMED = "time_consumed"

        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private fun parseDateTime(dateTimeStr: String?): LocalDateTime? {
        if (dateTimeStr == null) return null

        return try {
            LocalDateTime.parse(dateTimeStr, dateTimeFormatter)
        } catch (e: DateTimeParseException) {
            try {
                val date = LocalDate.parse(dateTimeStr, dateFormatter)
                date.atStartOfDay()
            } catch (e: DateTimeParseException) {
                null
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // creates table if does not exists, called HISTORY with date and caffeine columns
        val createTableQuery =
            """
                CREATE TABLE IF NOT EXISTS $TABLE_NAME ( 
                $COLUMN_DATE TEXT, 
                $COLUMN_TIME TEXT, 
                $COLUMN_ENTRY INTEGER, 
                $COLUMN_CALORIES INTEGER, 
                $COLUMN_VOLUME TEXT, 
                $COLUMN_NAME TEXT, 
                $COLUMN_TYPE TEXT,
                $COLUMN_TIME_CONSUMED TEXT
            ) 
            """.trimIndent()

        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        p1: Int,
        p2: Int,
    ) {
        TODO("Not yet implemented")
    }

    //  Insert a entry into the database
    fun insertEntry(
        date: String,
        time: String,
        caffeineDrink: CaffeineDrink,
    ) {
        val contentValues =
            ContentValues().apply {
                put(COLUMN_DATE, date)
                put(COLUMN_TIME, time)
                put(COLUMN_ENTRY, caffeineDrink.caffeineMg)
                put(COLUMN_CALORIES, caffeineDrink.calories)
                put(COLUMN_VOLUME, caffeineDrink.volumeMl)
                put(COLUMN_NAME, caffeineDrink.drink)
                put(COLUMN_TYPE, caffeineDrink.type)
                put(COLUMN_TIME_CONSUMED, caffeineDrink.timeConsumed?.format(dateTimeFormatter))
            }
        writableDatabase.insert(TABLE_NAME, null, contentValues)
    }

    //  Read all the entries from the database
    fun readEntries(): List<Pair<String, CaffeineDrink>> {
        val result = mutableListOf<Pair<String, CaffeineDrink>>()

        //  read from database with a SQL query
        val cursor =
            readableDatabase.rawQuery("SELECT * FROM $TABLE_NAME", null)
        while (cursor.moveToNext()) {
            //  grabs date and entry at the column 0 through loop down rows
            val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            val entry = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENTRY))
            val calories = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CALORIES))
            val volume = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOLUME))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
            val timeConsumedStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_CONSUMED))

            val timeConsumed = parseDateTime(timeConsumedStr)

            result.add(Pair(date, CaffeineDrink(entry, calories, volume, name, type, timeConsumed)))
        }

        return result
    }

    //  Read all the entries from the today
    fun readToday(today: String): List<CaffeineDrink> {
        val todayEntries = mutableListOf<CaffeineDrink>()

        //  read from database with a SQL query
        val cursor =
            readableDatabase.rawQuery(
                "SELECT $COLUMN_ENTRY, $COLUMN_CALORIES, $COLUMN_VOLUME, $COLUMN_NAME, $COLUMN_TYPE, $COLUMN_TIME_CONSUMED " +
                    "FROM $TABLE_NAME WHERE $COLUMN_DATE = ?",
                arrayOf(today),
            )
        while (cursor.moveToNext()) {
            //  grabs caffeine drinks at the column 0 through loop down rows
            val entry = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENTRY))
            val calories = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CALORIES))
            val volume = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VOLUME))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))
            val type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))
            val timeConsumedStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME_CONSUMED))

            val timeConsumed = parseDateTime(timeConsumedStr)

            todayEntries.add(CaffeineDrink(entry, calories, volume, name, type, timeConsumed))
        }

        return todayEntries
    }

    // delete selected drink from db
    fun deleteEntry(
        timeConsumed: LocalDateTime,
        caffeineDrink: CaffeineDrink,
    ) {
        val formattedTime = timeConsumed.format(dateTimeFormatter)
        writableDatabase.delete(
            TABLE_NAME,
            "$COLUMN_TIME_CONSUMED = ? AND $COLUMN_NAME = ? AND $COLUMN_ENTRY = ?",
            arrayOf(formattedTime, caffeineDrink.drink, caffeineDrink.caffeineMg.toString()),
        )
    }

    // get 5 recent drinks from db
    fun getRecentDrinks(limit: Int = 5): List<String> {
        val query = """
            SELECT DISTINCT $COLUMN_NAME 
            FROM $TABLE_NAME 
            WHERE $COLUMN_DATE = ? 
            ORDER BY $COLUMN_TIME_CONSUMED DESC 
            LIMIT $limit
        """

        val result = mutableListOf<String>()
        readableDatabase.rawQuery(query, arrayOf(LocalDate.now().toString())).use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME))?.let {
                    result.add(it)
                }
            }
        }
        return result
    }
}
