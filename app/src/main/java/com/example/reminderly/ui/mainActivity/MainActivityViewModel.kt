package com.example.reminderly.ui.mainActivity

import androidx.lifecycle.ViewModel
import com.example.footy.database.ReminderDatabaseDao
import com.example.reminderly.database.Reminder
import io.reactivex.Observable
import java.util.*

class MainActivityViewModel(val database: ReminderDatabaseDao):ViewModel() {

    /**millis of the begging of next today so we can get any reminders after that (upcoming reminders)*/
    private val nextDayMillis:Long
        get() {
          return  Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY,0)
                set(Calendar.MINUTE,0)
                set(Calendar.SECOND,0)
                add(Calendar.DAY_OF_MONTH,1)

            }.timeInMillis
        }

    /**millis of the begging of today so we can get any reminders before that (overdue reminders)*/
    private val todayMillis:Long
        get() {
            return  Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY,0)
                set(Calendar.MINUTE,0)
                set(Calendar.SECOND,0)

            }.timeInMillis
        }

    fun getDoneReminders(): Observable<MutableList<Reminder>> {
        return database.getDoneReminders()
    }

    fun getUpcomingReminders(): Observable<MutableList<Reminder>> {
        return database.getUpcomingReminders(nextDayMillis)
    }

    fun getOverdueReminders(): Observable<MutableList<Reminder>> {
        return database.getOverdueReminders(todayMillis)
    }

    fun getTodayReminders(): Observable<MutableList<Reminder>> {
        return database.getTodayReminders(todayMillis,nextDayMillis)
    }






}