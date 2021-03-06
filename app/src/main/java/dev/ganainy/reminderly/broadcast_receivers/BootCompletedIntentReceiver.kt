package dev.ganainy.reminderly.broadcast_receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.footy.database.ReminderDatabase
import dev.ganainy.reminderly.miscellaneous.RESTART_ALARAMS
import dev.ganainy.reminderly.utils.MyUtils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**reschedule alarms after device is rebooted or on app onCreate*/
class BootCompletedIntentReceiver : BroadcastReceiver() {

    private var compositeDisposable = CompositeDisposable()
    override fun onReceive(context: Context, intent: Intent) {

        if ("android.intent.action.BOOT_COMPLETED" == intent.action || intent.hasExtra(
                RESTART_ALARAMS
            )
        ) {

            restartUpcomingAlarms(context)
        }


    }

    private fun restartUpcomingAlarms(context: Context) {
        val reminderDatabaseDao = ReminderDatabase.getInstance(context).reminderDatabaseDao
        compositeDisposable.add(reminderDatabaseDao.getUpcomingReminders(Calendar.getInstance().timeInMillis)
            .subscribeOn(
                Schedulers.io()
            )
             .subscribe { activeReminderList ->
                Observable.fromIterable(activeReminderList).subscribe { reminder ->
                    MyUtils.addAlarmManager(
                        reminder,
                        context
                    )
                  }
                compositeDisposable.clear()
            })
    }
}
