package dev.ganainy.reminderly.ui.baseFragment

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BasicGridItem
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.bottomsheets.gridItems
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.example.footy.database.ReminderDatabase
import com.example.ourchat.ui.chat.ReminderAdapter
import com.example.ourchat.ui.chat.ReminderClickListener
import dev.ganainy.reminderly.R
import dev.ganainy.reminderly.database.Reminder
import dev.ganainy.reminderly.ui.mainActivity.ICommunication
import dev.ganainy.reminderly.utils.MyUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber


open class BaseFragment : Fragment() {

    lateinit var adapter: ReminderAdapter
    private val disposable = CompositeDisposable()
    private lateinit var viewModel: BaseFragmentViewModel
    private lateinit var viewModelFactory: ProvideDatabaseViewModelFactory

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_base, container, false)
    }

    companion object {
        fun newInstance() = BaseFragment()
    }

    @SuppressLint("CheckResult")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initViewModel()

        /**triggered by viewmodel when it needs to show some toast*/
        disposable.add(
            viewModel.toastSubject
                .subscribe({ stringResourceId ->
                    MyUtils.showCustomToast(requireContext(), stringResourceId)
                }, {
                    MyUtils.showCustomToast(requireContext(), R.string.something_went_wrong)
                    Timber.d("${it}")
                })
        )

        /**triggered by viewmodel to cancel alarms of certain reminder*/
        disposable.add(
            viewModel.cancelAlarmSubject
                .subscribe({ reminderToCancelAlarmsOf ->
                    MyUtils.cancelAlarmManager(reminderToCancelAlarmsOf, requireContext())
                }, {
                    Timber.d("${it}")
                })
        )

        /**triggered by viewmodel when it needs to add alarm related to certain reminder*/
        disposable.add(
            viewModel.addAlarmSubject
                .subscribe({ reminderToAddAlarmTo ->
                    MyUtils.addAlarmManager(reminderToAddAlarmTo, requireContext())
                }, {
                    MyUtils.showCustomToast(requireContext(), R.string.something_went_wrong)
                    Timber.d("${it}")
                })
        )


        /**triggered by viewmodel when it needs to cancel notification of certain reminder*/
        disposable.add(
            viewModel.cancelNotificationSubject
                .subscribe({ reminderToCancelNotificationOf ->
                    MyUtils.cancelNotification(reminderToCancelNotificationOf.id, requireContext())
                }, {
                    Timber.d("${it}")
                })
        )
    }

    private fun initViewModel() {
        val reminderDatabaseDao = ReminderDatabase.getInstance(requireContext()).reminderDatabaseDao

        viewModelFactory =

            ProvideDatabaseViewModelFactory(
                requireActivity().application,
                reminderDatabaseDao
            )

        viewModel = ViewModelProvider(this, viewModelFactory).get(BaseFragmentViewModel::class.java)
    }

    fun initAdapter() {
        adapter = ReminderAdapter(requireActivity(), object : ReminderClickListener {
            override fun onReminderClick(reminder: Reminder) {
                editReminder(reminder)
            }

            override fun onFavoriteClick(reminder: Reminder, position: Int) {
                updateReminderFavorite(reminder)
            }

            override fun onMenuClick(reminder: Reminder, position: Int) {
                showOptionsSheet(reminder)
            }

        })
    }

    private fun updateReminderFavorite(reminder: Reminder) {
        disposable.add(viewModel.updateReminderFavorite(reminder).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
            //onComplete
            }, {//onError
                Timber.d("${it}")
            })
        )
    }

    private fun showOptionsSheet(
        reminder: Reminder
    ) {
        val items = listOf(
            BasicGridItem(R.drawable.ic_check_grey, getString(R.string.done)),
            BasicGridItem(R.drawable.ic_access_time_grey, getString(R.string.postpone)),
            BasicGridItem(R.drawable.ic_edit_grey, getString(R.string.edit)),
            BasicGridItem(R.drawable.ic_content_copy_grey, getString(R.string.mcopy)),
            BasicGridItem(R.drawable.ic_share_grey, getString(R.string.share)),
            BasicGridItem(R.drawable.ic_delete_grey, getString(R.string.delete))
        )

        MaterialDialog(requireActivity(), BottomSheet()).show {
            gridItems(items) { _, index, item ->
                when (index) {
                    0 -> {
                        handleReminderDoneBehaviour(reminder)
                    }
                    1 -> {
                        postponeReminder(reminder)
                    }
                    2 -> {
                        editReminder(reminder)
                    }
                    3 -> {
                        copyToClipboard(reminder)
                    }
                    4 -> {
                        shareReminder(reminder)
                    }
                    5 -> {
                        deleteReminder(reminder)
                    }
                }
            }
        }
    }

    private fun deleteReminder(
        reminder: Reminder
    ) {
        disposable.add(viewModel.deleteReminder(reminder).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
            }, { error ->
                Timber.d("${error}")
            })
        )
    }

    private fun handleReminderDoneBehaviour(
        reminder: Reminder
    ) {
        disposable.add(
            viewModel.handleReminderDoneBehaviour(reminder)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //onComplete
                }, { //onError
                    Timber.d("${it}")
                })
        )
    }


    private fun shareReminder(reminder: Reminder) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, reminder.text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun copyToClipboard(reminder: Reminder) {
        val clipboard =
            requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", reminder.text)
        clipboard.setPrimaryClip(clip)
        MyUtils.showCustomToast(requireContext(), R.string.copied_to_clipboard)
    }

    private fun editReminder(reminder: Reminder) {
        (requireActivity() as? ICommunication)?.showReminderFragment(reminder)
    }


    private fun postponeReminder(
        reminder: Reminder
    ) {

        //those variables to reflect the change in pickers
        var minute = 0
        var hour = 0
        var day = 0


        val dialog = MaterialDialog(requireContext()).show {
            customView(R.layout.custom_postpone_dialog)
            positiveButton(R.string.confirm) {
                postponeReminder(reminder, day, hour, minute)
            }
            negativeButton(R.string.cancel)
            title(0, getString(R.string.select_time))
        }

        //get value of minutePicker
        dialog.getCustomView().findViewById<NumberPicker>(R.id.minutePicker).apply {
            maxValue = 59
            minValue = 0
            setOnValueChangedListener { picker, oldVal, newVal ->
                minute = newVal
            }
        }

        //get value of hourPicker
        dialog.getCustomView().findViewById<NumberPicker>(R.id.hourPicker).apply {
            maxValue = 23
            minValue = 0
            setOnValueChangedListener { picker, oldVal, newVal ->
                hour = newVal
            }
        }

        //get value of datePicker
        dialog.getCustomView().findViewById<NumberPicker>(R.id.dayPicker).apply {
            minValue = 0
            maxValue = 30
            setOnValueChangedListener { picker, oldVal, newVal ->
                day = newVal
            }
        }


    }

    private fun postponeReminder(
        reminder: Reminder,
        day: Int,
        hour: Int,
        minute: Int
    ) {
        disposable.add(
            viewModel.postponeReminder(reminder, day, hour, minute)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe({
                    //onComplete
                }, {//onError
                    Timber.d("${it}")
                })
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

}
