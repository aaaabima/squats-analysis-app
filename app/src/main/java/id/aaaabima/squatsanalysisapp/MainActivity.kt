package id.aaaabima.squatsanalysisapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import id.aaaabima.squatsanalysisapp.databinding.ActivityMainBinding
import id.aaaabima.squatsanalysisapp.fragment.schedule.DatePickerFragment
import id.aaaabima.squatsanalysisapp.fragment.schedule.TimePickerFragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), DatePickerFragment.DialogDateListener,
  TimePickerFragment.DialogTimeListener {
  private lateinit var binding: ActivityMainBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val navHostFragment =
      supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
    val navController = navHostFragment.navController
    val appBarConfiguration = AppBarConfiguration(
      setOf(
        R.id.permissionFragment,
        R.id.fragment_exercise,
        R.id.fragment_schedule,
        R.id.fragment_settings
      )
    )
    setupActionBarWithNavController(navController, appBarConfiguration)
    binding.bottomNavMain.setupWithNavController(navController)
  }

  override fun onDialogDateSet(tag: String?, year: Int, month: Int, dayOfMonth: Int) {
    // Set date formatter
    val calendar = Calendar.getInstance()
    calendar.set(year, month, dayOfMonth)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Set text from schedule text
    val tvScheduleDate: TextView = findViewById(R.id.tv_schedule_date)
    tvScheduleDate.text = dateFormat.format(calendar.time)
  }

  override fun onDialogTimeSet(tag: String?, hourOfDay: Int, minute: Int) {
    // Set date formatter
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
    calendar.set(Calendar.MINUTE, minute)

    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Set text from schedule time
    val tvScheduleTime: TextView = findViewById(R.id.tv_schedule_time)
    val tvDailyTime: TextView = findViewById(R.id.tv_daily_time)
    when (tag) {
      TIME_PICKER_ONCE_TAG -> tvScheduleTime.text = dateFormat.format(calendar.time)
      TIME_PICKER_REPEAT_TAG -> tvDailyTime.text = dateFormat.format(calendar.time)
      else -> {}
    }
  }

  @Deprecated("Deprecated in Java", ReplaceWith("finish()"))
  override fun onBackPressed() {
    finish()
  }

  companion object {
    private const val DATE_PICKER_TAG = "DatePicker"
    private const val TIME_PICKER_ONCE_TAG = "TimePickerOnce"
    private const val TIME_PICKER_REPEAT_TAG = "TimePickerRepeat"
  }
}