package com.example.reminderly.Utils

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.reminderly.R
import com.example.reminderly.broadcast_receivers.AlarmService
import com.example.reminderly.broadcast_receivers.NewReminderReceiver
import com.example.reminderly.database.Reminder
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.text.SimpleDateFormat
import java.util.*

class MyUtils {

    companion object {

        private val currentDate: Date
            get() {
                return Date()
            }


        /**-------------------------------------Date-------------------------------------*/

        private val locale = Locale("ar")

        private val dateFormat = SimpleDateFormat("EEEE, dd MMMM", locale)
        private val timeFormat = SimpleDateFormat("hh:mm a", locale)


        fun getCurrentDateFormatted(): String {
            return dateFormat.format(currentDate)
        }

        fun getCurrentTimeFormatted(): String {
            return timeFormat.format(currentDate)
        }

        fun formatDate(date: Date): String {
            return dateFormat.format(date)
        }

        fun formatTime(date: Date): String {
            return timeFormat.format(date)
        }


        fun getDateFromCalendar(calendar: Calendar): String {
            return dateFormat.format(calendar.time)
        }

        fun getTimeFromCalendar(calendar: Calendar): String {
            return timeFormat.format(calendar.time)
        }

        //convert list of Calendar to list of CalendarDay
        fun getCalendarDays(activeReminderList: MutableList<Reminder>): MutableList<CalendarDay> {
            val calendarDays = mutableListOf<CalendarDay>()
            for (reminder in activeReminderList) {
                calendarDays.add(
                    CalendarDay.from(
                        reminder.createdAt.get(Calendar.YEAR),
                        reminder.createdAt.get(Calendar.MONTH) + 1,
                        reminder.createdAt.get(Calendar.DAY_OF_MONTH)
                    )
                )
            }
            return calendarDays
        }

        fun calendarDayToCalendar(calendarDay: CalendarDay): Calendar {
            return Calendar.getInstance().apply {
                set(Calendar.YEAR, calendarDay.year)
                set(Calendar.MONTH, calendarDay.month - 1)
                set(Calendar.DAY_OF_MONTH, calendarDay.day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        fun calendarToCalendarDay(calendar: Calendar): CalendarDay {
            return CalendarDay.from(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        /**-------------------------------------keyboard----------------------------------*/


        /**@param view :any view from the caller layout to get token from */
        fun hideKeyboard(context: Context, view: View): Boolean? {

            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            return imm?.hideSoftInputFromWindow(view.windowToken, 0)

        }

        fun showKeyboard(context: Context?) {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        }


        /**------------------------------Helpers------------------------------*/

        fun convertToArabicNumber(englishNum: String): String {
            val arabicChars = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            val builder = StringBuilder()
            for (i in englishNum.indices) {
                if (Character.isDigit(englishNum[i])) {
                    builder.append(arabicChars[englishNum[i].toInt() - 48])
                } else {
                    builder.append(englishNum[i])
                }
            }
            return builder.toString()
        }

        fun convertRepeat(repeat: Int): String {
            return when (repeat) {
                0 -> "مرة واحده"
                1 -> "كل ساعة"
                2 -> "كل يوم"
                3 -> "كل اسبوع"
                4 -> "كل شهر"
                5 -> "كل عام"
                else -> throw Exception("unknown repeat")
            }

        }

        fun convertPriority(repeat: Int): String {
            return when (repeat) {
                0 -> "عادي"
                1 -> "متوسط"
                2 -> "هام"
                else -> throw Exception("unknown repeat")
            }

        }

        fun convertReminderType(repeat: Int): String {
            return when (repeat) {
                0 -> "تنيه بإستخدام الاشعارات"
                1 -> "تنبيه بإستخدام الجرس"
                else -> throw Exception("unknown repeat")
            }

        }

        fun convertNotifyAdv(notifyAdvAmount: Int, notifyAdvUnit: Int): CharSequence? {

            if (notifyAdvAmount + notifyAdvUnit == 0) return "التنبيه المسبق غير مفعل"

            val builder = java.lang.StringBuilder(convertToArabicNumber(notifyAdvAmount.toString()))

            builder.append(
                when (notifyAdvUnit) {
                    0 -> " دقائق "
                    1 -> " ساعات"
                    2 -> " أيام"
                    3 -> " اسابيع"
                    else -> throw Exception("unknown repeat")
                }
            )

            return builder

        }


        /**---------------------------alarm manager----------------------------------------*/

        /**setup alarm manager to trigger NewReminderReceiver on reminder date*/
        fun addAlarm(
            reminderId: Long,
            context: Context?,
            triggerMillis: Long,
            repeat: Int
        ) {
            //cancel any existing alarms for this reminder
            cancelAlarm(reminderId, context)
            //add new onetime alarm/ repeat alarm depending on repeat value
            when (repeat) {
                0 -> {//one time reminder
                    addOneTimeAlarm(reminderId, context, triggerMillis)
                }
                1 -> { //every hour reminder
                    addPeriodicAlarm(reminderId, context, triggerMillis, 3600 * 1000)
                }
                2 -> { //every day reminder
                    addPeriodicAlarm(reminderId, context, triggerMillis, 3600 * 1000 * 24)
                }
                3 -> { //every week reminder
                    addPeriodicAlarm(reminderId, context, triggerMillis, 3600 * 1000 * 24 * 7)
                }
                4 -> { //every month reminder
                    addPeriodicAlarm(reminderId, context, triggerMillis, 3600 * 1000 * 24 * 30L)
                }
                5 -> { //every year reminder
                    addPeriodicAlarm(reminderId, context, triggerMillis, 3600 * 1000 * 24 * 365L)
                }
            }
        }


        private fun addOneTimeAlarm(
            reminderId: Long,
            context: Context?,
            triggerMillis: Long
        ) {
            Log.d("DebugTag", "addOneTimeAlarm: ${reminderId} ,,, ${Date(triggerMillis)}")
            val notifyIntent = Intent(context, NewReminderReceiver::class.java)
            notifyIntent.putExtra("reminderId", reminderId)
            val notifyPendingIntent = PendingIntent.getBroadcast(
                context, reminderId.toInt(), notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.M
            ) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    notifyPendingIntent
                )
            } else {
                alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, notifyPendingIntent)
            }
        }


        private fun addPeriodicAlarm(
            reminderId: Long,
            context: Context?,
            triggerMillis: Long,
            repeatMillis: Long
        ) {
            Log.d(
                "DebugTag",
                "addPeriodicAlarm: ${reminderId} ,,, ${Date(triggerMillis)} ,,, ${Date(repeatMillis)}"
            )

            val notifyIntent = Intent(context, NewReminderReceiver::class.java)
            notifyIntent.putExtra("reminderId", reminderId)
            val notifyPendingIntent = PendingIntent.getBroadcast(
                context, reminderId.toInt(), notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

            alarmManager?.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                repeatMillis,
                notifyPendingIntent
            )

        }


        fun cancelAlarm(
            reminderId: Long,
            context: Context?
        ) {
            val notifyIntent = Intent(context, NewReminderReceiver::class.java)
            notifyIntent.putExtra("reminderId", reminderId)
            val notifyPendingIntent = PendingIntent.getBroadcast(
                context, reminderId.toInt(), notifyIntent,
                PendingIntent.FLAG_ONE_SHOT
            )
            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(notifyPendingIntent)

            //if cancelAlarm is called by clicking notification actions we also want to remove notification
            cancelNotification(reminderId, context)
        }

         fun cancelNotification(reminderId: Long, context: Context?) {
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(reminderId.toInt())
        }

        /**----------------------------general utils---------------------------------------------*/
        fun postponeReminder(
            reminder: Reminder,
            context: Context?,
            day: Int,
            hour: Int,
            minute: Int
        ): Reminder? {
            cancelAlarm(reminder.id,context)

            /** postpone reminder with passed duration*/

            reminder.createdAt.apply {
                add(Calendar.DAY_OF_MONTH, day)
                add(Calendar.HOUR_OF_DAY, hour)
                add(Calendar.MINUTE, minute)
            }


            /** will check that the new reminder date is bigger than current date and return null
             *  if not; because its useless to postpone reminder to a previous date*/

            return if (reminder.createdAt.before(Calendar.getInstance())) {
                Toast.makeText(
                        context,
                        context?.getString(R.string.must_be_upcoming_date),
                        Toast.LENGTH_LONG
                    )
                    .show()

                //remove added duration since reminder won't be updated
                reminder.createdAt.apply {
                    add(Calendar.DAY_OF_MONTH, -day)
                    add(Calendar.HOUR_OF_DAY, -hour)
                    add(Calendar.MINUTE, -minute)
                }
                null
            } else {
                reminder
            }


        }



        /**stop the notification or any ongoing ringing alarm on showing postpone dialog*/
        fun stopAlarmService(context: Context) {
            val notifyIntent = Intent(context, AlarmService::class.java)
            context.stopService(notifyIntent)
        }

    }



}