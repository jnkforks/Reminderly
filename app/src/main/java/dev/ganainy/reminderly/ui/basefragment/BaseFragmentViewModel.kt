package dev.ganainy.reminderly.ui.basefragment

import android.app.Application
import androidx.lifecycle.ViewModel
import com.example.footy.database.ReminderDatabaseDao
import dev.ganainy.reminderly.database.Reminder
import io.reactivex.Completable

class BaseFragmentViewModel(app:Application, val database:ReminderDatabaseDao) : ViewModel() {

    fun updateReminderFavorite(reminder: Reminder) :Completable{
        reminder.isFavorite =
            !reminder.isFavorite //change favorite value then update in database
      return database.update(reminder)
    }

    fun updateReminder(reminder: Reminder) :Completable{
               return database.update(reminder)
    }

    fun updateReminderDone(reminder: Reminder) :Completable{
        reminder.isDone = true
        return database.update(reminder)
    }

    fun deleteReminder(reminder: Reminder): Completable {
        return database.delete(reminder)
    }



}
