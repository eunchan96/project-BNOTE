package com.chan.bnote.ui.mypage.gratitude

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.mypage.gratitude.GratitudeNote
import com.chan.bnote.ui.sermon.bycalendar.CalendarDayCell
import com.chan.bnote.ui.sermon.bycalendar.CalendarGridAdapter
import com.chan.bnote.ui.sermon.bycalendar.MonthYearPickerBottomSheet
import kotlinx.coroutines.launch
import java.util.Calendar

class GratitudeActivity : AppCompatActivity() {

	private lateinit var monthYearText: TextView
	private lateinit var gridRecycler: RecyclerView

	private val addLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadCalendarGrid()
			loadNotesForSelectedDate()
		}
	}

	private val editLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadCalendarGrid()
			loadNotesForSelectedDate()
		}
	}

	private var currentYear: Int
	private var currentMonth0: Int
	private var selectedDate: Long

	init {
		val cal = Calendar.getInstance()
		currentYear = cal.get(Calendar.YEAR)
		currentMonth0 = cal.get(Calendar.MONTH)
		selectedDate = DateUtils.normalizeToDayStart(cal.timeInMillis)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_gratitude)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gratitude_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		monthYearText = findViewById(R.id.text_month_year)
		gridRecycler = findViewById(R.id.recycler_calendar_grid)
		gridRecycler.layoutManager = GridLayoutManager(this, 7)

		monthYearText.setOnClickListener {
			val picker = MonthYearPickerBottomSheet(currentYear, currentMonth0)
			picker.onSelected = { year, month0 ->
				currentYear = year
				currentMonth0 = month0
				loadCalendarGrid()
			}
			picker.show(supportFragmentManager, "month_year_picker")
		}

		findViewById<TextView>(R.id.btn_month_prev).setOnClickListener {
			currentMonth0 -= 1
			if (currentMonth0 < 0) {
				currentMonth0 = 11; currentYear -= 1
			}
			loadCalendarGrid()
		}
		findViewById<TextView>(R.id.btn_calendar_today).setOnClickListener {
			val cal = Calendar.getInstance()
			currentYear = cal.get(Calendar.YEAR)
			currentMonth0 = cal.get(Calendar.MONTH)
			selectedDate = DateUtils.normalizeToDayStart(cal.timeInMillis)
			loadCalendarGrid()
			loadNotesForSelectedDate()
		}
		findViewById<TextView>(R.id.btn_month_next).setOnClickListener {
			currentMonth0 += 1
			if (currentMonth0 > 11) {
				currentMonth0 = 0; currentYear += 1
			}
			loadCalendarGrid()
		}

		findViewById<TextView>(R.id.fab_add_gratitude).setOnClickListener {
			addLauncher.launch(
				AddGratitudeActivity.createIntent(
					this,
					initialDateMillis = selectedDate
				)
			)
		}

		loadCalendarGrid()
		loadNotesForSelectedDate()
	}

	private fun loadCalendarGrid() {
		monthYearText.text = DateUtils.formatYearMonth(currentYear, currentMonth0)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val startMillis = DateUtils.getMonthStartMillis(currentYear, currentMonth0)
			val endMillis = DateUtils.getMonthEndMillisExclusive(currentYear, currentMonth0)
			val datesWithNotes =
				db.gratitudeNoteDao().getDatesInRange(startMillis, endMillis).toSet()

			val fallbackColorHex = String.format(
				"#%06X", 0xFFFFFF and androidx.core.content.ContextCompat.getColor(
					this@GratitudeActivity, R.color.brown_primary
				)
			)
			val colorsByDate = datesWithNotes.associateWith { listOf(fallbackColorHex) }

			val cells = buildMonthCells(currentYear, currentMonth0, colorsByDate)
			gridRecycler.adapter = CalendarGridAdapter(cells, selectedDate) { cell ->
				selectedDate = cell.dateMillis
				loadCalendarGrid()
				loadNotesForSelectedDate()
			}
		}
	}

	private fun buildMonthCells(
		year: Int, month0: Int, colorsByDate: Map<Long, List<String>>
	): List<CalendarDayCell> {
		val cal = Calendar.getInstance()
		cal.set(year, month0, 1, 0, 0, 0)
		cal.set(Calendar.MILLISECOND, 0)

		val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
		val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
		val today = DateUtils.normalizeToDayStart(System.currentTimeMillis())

		val cells = mutableListOf<CalendarDayCell>()

		if (firstDayOfWeek > 0) {
			val prevCal = cal.clone() as Calendar
			prevCal.add(Calendar.MONTH, -1)
			val prevMonthDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
			for (i in firstDayOfWeek - 1 downTo 0) {
				val dayNum = prevMonthDays - i
				val dateCal = prevCal.clone() as Calendar
				dateCal.set(Calendar.DAY_OF_MONTH, dayNum)
				val millis = DateUtils.normalizeToDayStart(dateCal.timeInMillis)
				cells.add(
					CalendarDayCell(
						millis,
						dayNum,
						false,
						millis == today,
						colorsByDate[millis].orEmpty()
					)
				)
			}
		}

		for (day in 1..daysInMonth) {
			val dateCal = cal.clone() as Calendar
			dateCal.set(Calendar.DAY_OF_MONTH, day)
			val millis = DateUtils.normalizeToDayStart(dateCal.timeInMillis)
			cells.add(
				CalendarDayCell(
					millis,
					day,
					true,
					millis == today,
					colorsByDate[millis].orEmpty()
				)
			)
		}

		val remainder = cells.size % 7
		if (remainder != 0) {
			val nextCal = cal.clone() as Calendar
			nextCal.add(Calendar.MONTH, 1)
			for (day in 1..(7 - remainder)) {
				val dateCal = nextCal.clone() as Calendar
				dateCal.set(Calendar.DAY_OF_MONTH, day)
				val millis = DateUtils.normalizeToDayStart(dateCal.timeInMillis)
				cells.add(
					CalendarDayCell(
						millis,
						day,
						false,
						millis == today,
						colorsByDate[millis].orEmpty()
					)
				)
			}
		}

		return cells
	}

	private fun loadNotesForSelectedDate() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val notes = db.gratitudeNoteDao().getByDate(selectedDate)
			renderList(notes)
		}
	}

	private fun renderList(notes: List<GratitudeNote>) {
		val recyclerView = findViewById<RecyclerView>(R.id.recycler_gratitude_by_date)
		val emptyText = findViewById<TextView>(R.id.text_empty_gratitude_calendar)

		if (notes.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		emptyText.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = LinearLayoutManager(this)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val rows = GratitudeRowBuilder.build(db, notes)
			recyclerView.adapter = GratitudeRowAdapter(rows) { note ->
				editLauncher.launch(
					AddGratitudeActivity.editIntent(
						this@GratitudeActivity,
						note.id
					)
				)
			}
		}
	}
}