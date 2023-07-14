package id.aaaabima.squats_analysis_app.fragment.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import id.aaaabima.squats_analysis_app.R
import id.aaaabima.squats_analysis_app.databinding.FragmentScheduleBinding


class ScheduleFragment : Fragment(), View.OnClickListener {

    private var binding: FragmentScheduleBinding? = null
    private lateinit var alarmReceiver: AlarmReceiver


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentScheduleBinding.inflate(inflater, container, false)

        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btnScheduleDate?.setOnClickListener(this)
        binding?.btnScheduleTime?.setOnClickListener(this)
        binding?.btnSetSchedule?.setOnClickListener(this)
        binding?.btnDailyTime?.setOnClickListener(this)
        binding?.btnSetDaily?.setOnClickListener(this)

        alarmReceiver = AlarmReceiver()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_schedule_date -> {
                val datePickerFragment = DatePickerFragment()
                datePickerFragment.show(childFragmentManager, DATE_PICKER_TAG)
            }

            R.id.btn_schedule_time -> {
                val timePickerFragment = TimePickerFragment()
                timePickerFragment.show(childFragmentManager, TIME_PICKER_ONCE_TAG)
            }

            R.id.btn_set_schedule -> {
                val scheduleDate = binding?.tvScheduleDate?.text.toString()
                val scheduleTime = binding?.tvScheduleTime?.text.toString()
                val scheduleMessage = binding?.edtScheduleDetails?.text.toString()

                alarmReceiver.setOneTimeAlarm(
                    requireContext(),
                    AlarmReceiver.TYPE_ONE_TIME,
                    scheduleDate,
                    scheduleTime,
                    scheduleMessage
                )
            }

            R.id.btn_daily_time -> {
                val timePickerFragmentRepeat = TimePickerFragment()
                timePickerFragmentRepeat.show(childFragmentManager, TIME_PICKER_REPEAT_TAG)
            }

            R.id.btn_set_daily -> {
                val dailyTime = binding?.tvDailyTime?.text.toString()
                val dailyMessage = binding?.edtDailyMessage?.text.toString()
                alarmReceiver.setRepeatingAlarm(
                    requireContext(),
                    AlarmReceiver.TYPE_REPEATING,
                    dailyTime,
                    dailyMessage
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    companion object {
        private const val DATE_PICKER_TAG = "DatePicker"
        private const val TIME_PICKER_ONCE_TAG = "TimePickerOnce"
        private const val TIME_PICKER_REPEAT_TAG = "TimePickerRepeat"
    }
}