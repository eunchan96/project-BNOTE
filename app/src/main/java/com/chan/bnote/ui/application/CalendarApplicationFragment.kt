package com.chan.bnote.ui.application.bycalendar

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.application.Application
import com.chan.bnote.ui.application.ApplicationDetailActivity
import com.chan.bnote.ui.application.ApplicationRowAdapter
import com.chan.bnote.ui.application.ApplicationRowBuilder
import com.chan.bnote.ui.application.addapplication.AddApplicationActivity
import com.chan.bnote.ui.sermon.bycalendar.CalendarDayCell
import com.chan.bnote.ui.sermon.bycalendar.CalendarGridAdapter
import com.chan.bnote.ui.sermon.bycalendar.MonthYearPickerBottomSheet
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarApplicationFragment : Fragment() {

	private lateinit var monthYearText: TextView
	private lateinit var gridRecycler: RecyclerView

	private val addLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadCalendarGrid()
			loadApplicationsForSelectedDate()
		}
	}

	private val detailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadCalendarGrid()
			loadApplicationsForSelectedDate()
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

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_application_calendar, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		monthYearText = view.findViewById(R.id.text_month_year)
		gridRecycler = view.findViewById(R.id.recycler_calendar_grid)
		gridRecycler.layoutManager = GridLayoutManager(requireContext(), 7)

		monthYearText.setOnClickListener {
			val picker = MonthYearPickerBottomSheet(currentYear, currentMonth0)
			picker.onSelected = { year, month0 ->
				currentYear = year
				currentMonth0 = month0
				loadCalendarGrid()
			}
			picker.show(parentFragmentManager, "month_year_picker")
		}

		view.findViewById<TextView>(R.id.btn_month_prev).setOnClickListener {
			currentMonth0 -= 1
			if (currentMonth0 < 0) {
				currentMonth0 = 11; currentYear -= 1
			}
			loadCalendarGrid()
		}
		view.findViewById<TextView>(R.id.btn_month_next).setOnClickListener {
			currentMonth0 += 1
			if (currentMonth0 > 11) {
				currentMonth0 = 0; currentYear += 1
			}
			loadCalendarGrid()
		}

		view.findViewById<TextView>(R.id.fab_add_application).setOnClickListener {
			addLauncher.launch(
				AddApplicationActivity.createIntent(
					requireContext(),
					initialDateMillis = selectedDate
				)
			)
		}

		loadCalendarGrid()
		loadApplicationsForSelectedDate()
	}

	private fun loadCalendarGrid() {
		monthYearText.text = DateUtils.formatYearMonth(currentYear, currentMonth0)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val startMillis = DateUtils.getMonthStartMillis(currentYear, currentMonth0)
			val endMillis = DateUtils.getMonthEndMillisExclusive(currentYear, currentMonth0)
			val markers = db.applicationDao().getMarkersInRange(startMillis, endMillis)

			val fallbackColorHex = String.format(
				"#%06X", 0xFFFFFF and androidx.core.content.ContextCompat.getColor(
					requireContext(), R.color.category_none
				)
			)
			val colorsByDate = markers.groupBy { it.applicationDate }
				.mapValues { entry -> entry.value.map { it.colorHex ?: fallbackColorHex } }

			val cells = buildMonthCells(currentYear, currentMonth0, colorsByDate)
			gridRecycler.adapter = CalendarGridAdapter(cells, selectedDate) { cell ->
				selectedDate = cell.dateMillis
				loadCalendarGrid()
				loadApplicationsForSelectedDate()
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

	private fun loadApplicationsForSelectedDate() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val applications = db.applicationDao().getByDate(selectedDate)
			renderList(applications)
		}
	}

	private fun renderList(applications: List<Application>) {
		val recyclerView =
			view?.findViewById<RecyclerView>(R.id.recycler_applications_by_date) ?: return
		val emptyText = view?.findViewById<TextView>(R.id.text_empty_application_calendar) ?: return

		if (applications.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		emptyText.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val rows = ApplicationRowBuilder.build(db, applications, useDateLabel = false)
			recyclerView.adapter = ApplicationRowAdapter(rows) { application ->
				detailLauncher.launch(
					ApplicationDetailActivity.createIntent(requireContext(), application.id)
				)
			}
		}
	}
}