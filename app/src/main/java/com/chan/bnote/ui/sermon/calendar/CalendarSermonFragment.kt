package com.chan.bnote.ui.sermon.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.ui.sermon.AddSermonBottomSheet
import com.chan.bnote.ui.sermon.SermonDetailBottomSheet
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarSermonFragment : Fragment() {

	private lateinit var monthYearText: TextView
	private lateinit var gridRecycler: RecyclerView

	private var currentYear: Int
	private var currentMonth0: Int // 0-indexed
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
		return inflater.inflate(R.layout.fragment_sermon_calendar, container, false)
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

		view.findViewById<TextView>(R.id.fab_add_sermon).setOnClickListener {
			val sheet = AddSermonBottomSheet(initialDateMillis = selectedDate)
			sheet.onSaved = { loadCalendarGrid(); loadSermonsForSelectedDate() }
			sheet.show(parentFragmentManager, "add_sermon")
		}

		loadCalendarGrid()
		loadSermonsForSelectedDate()
	}

	private fun loadCalendarGrid() {
		monthYearText.text = DateUtils.formatYearMonth(currentYear, currentMonth0)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val startMillis = DateUtils.getMonthStartMillis(currentYear, currentMonth0)
			val endMillis = DateUtils.getMonthEndMillisExclusive(currentYear, currentMonth0)
			val markers = db.sermonDao().getSermonMarkersInRange(startMillis, endMillis)

			val colorsByDate = markers.groupBy { it.sermonDate }
				.mapValues { entry -> entry.value.mapNotNull { it.colorHex } }

			val cells = buildMonthCells(currentYear, currentMonth0, colorsByDate)
			gridRecycler.adapter = CalendarGridAdapter(cells, selectedDate) { cell ->
				selectedDate = cell.dateMillis
				loadCalendarGrid() // 선택 표시 갱신을 위해 그리드 다시 그림
				loadSermonsForSelectedDate()
			}
		}
	}

	private fun buildMonthCells(
		year: Int, month0: Int, colorsByDate: Map<Long, List<String>>
	): List<CalendarDayCell> {
		val cal = Calendar.getInstance()
		cal.set(year, month0, 1, 0, 0, 0)
		cal.set(Calendar.MILLISECOND, 0)

		val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = 일요일
		val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
		val today = DateUtils.normalizeToDayStart(System.currentTimeMillis())

		val cells = mutableListOf<CalendarDayCell>()

		// 앞쪽 빈 칸: 이전 달 날짜로 채움
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

		// 이번 달 날짜
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

		// 뒤쪽 빈 칸: 다음 달 날짜로 채워서 마지막 주 완성 (7의 배수로만 맞춤 - 필요한 주만큼만)
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

	private fun loadSermonsForSelectedDate() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByDate(selectedDate)
			renderList(sermons)
		}
	}

	private fun renderList(sermons: List<Sermon>) {
		val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_sermons_by_date) ?: return
		val emptyText = view?.findViewById<TextView>(R.id.text_empty_calendar) ?: return

		if (sermons.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		emptyText.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val rows = SermonRowBuilder.build(db, sermons, useDateLabel = false)
			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				val detail = SermonDetailBottomSheet(sermon)
				detail.onChanged = { loadCalendarGrid(); loadSermonsForSelectedDate() }
				detail.show(parentFragmentManager, "sermon_detail")
			}
		}
	}
}