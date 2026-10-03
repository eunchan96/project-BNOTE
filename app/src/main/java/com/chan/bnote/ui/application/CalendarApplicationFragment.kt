package com.chan.bnote.ui.application

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
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.application.Application
import com.chan.bnote.ui.FabAddHandler
import com.chan.bnote.ui.SubtabRefreshable
import com.chan.bnote.ui.application.addapplication.AddApplicationActivity
import com.chan.bnote.ui.sermon.SermonSortableFragment
import com.chan.bnote.ui.sermon.SortButtonHelper
import com.chan.bnote.ui.sermon.bycalendar.CalendarDayCell
import com.chan.bnote.ui.sermon.bycalendar.CalendarGridAdapter
import com.chan.bnote.ui.sermon.bycalendar.MonthYearPickerBottomSheet
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarApplicationFragment : Fragment(), FabAddHandler, SubtabRefreshable,
	SermonSortableFragment {

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
	private var sortMode = "ADDED"

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

		sortMode = AppSettings.getApplicationSortMode(requireContext())

		monthYearText.setOnClickListener {
			val picker = MonthYearPickerBottomSheet(currentYear, currentMonth0)
			picker.onSelected = { year, month0 ->
				currentYear = year
				currentMonth0 = month0
				loadCalendarGrid()
			}
			picker.show(parentFragmentManager, "month_year_picker")
		}

		view.findViewById<TextView>(R.id.btn_month_prev).setOnClickListener { goToPrevMonth() }
		view.findViewById<TextView>(R.id.btn_calendar_today).setOnClickListener {
			val cal = Calendar.getInstance()
			currentYear = cal.get(Calendar.YEAR)
			currentMonth0 = cal.get(Calendar.MONTH)
			selectedDate = DateUtils.normalizeToDayStart(cal.timeInMillis)
			loadCalendarGrid()
			loadApplicationsForSelectedDate()
		}
		view.findViewById<TextView>(R.id.btn_month_next).setOnClickListener { goToNextMonth() }

		val swipeIntercept =
			view.findViewById<com.chan.bnote.ui.common.HorizontalSwipeInterceptLayout>(
				R.id.swipe_intercept_calendar
			)
		swipeIntercept.onSwipeRight = { goToPrevMonth() }
		swipeIntercept.onSwipeLeft = { goToNextMonth() }

		SortButtonHelper.setup(view.findViewById(R.id.btn_application_sort), this)

		loadCalendarGrid()
		loadApplicationsForSelectedDate()
	}

	private fun goToPrevMonth() {
		currentMonth0 -= 1
		if (currentMonth0 < 0) {
			currentMonth0 = 11; currentYear -= 1
		}
		loadCalendarGrid()
	}

	private fun goToNextMonth() {
		currentMonth0 += 1
		if (currentMonth0 > 11) {
			currentMonth0 = 0; currentYear += 1
		}
		loadCalendarGrid()
	}

	override fun onFabAddClicked() {
		addLauncher.launch(
			AddApplicationActivity.createIntent(
				requireContext(),
				initialDateMillis = selectedDate
			)
		)
	}

	/** 다른 하단 탭(마이페이지 등)에 갔다가 이 탭으로 돌아왔을 때(또는 설교 탭의 다른
	 * 서브탭에서 돌아왔을 때) 다시 불러온다. */
	override fun onSubtabBecameVisible() {
		if (!::monthYearText.isInitialized) return
		loadCalendarGrid()
		loadApplicationsForSelectedDate()
	}

	private fun loadCalendarGrid() {
		monthYearText.text = DateUtils.formatYearMonth(currentYear, currentMonth0)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val (startMillis, endMillis) = DateUtils.getMonthGridRangeMillis(
				currentYear,
				currentMonth0
			)
			// 날짜 칸의 색깔 막대 순서도 아래 목록의 정렬과 맞춘다(설교노트 캘린더와 같은 방식).
			// 쿼리 결과가 이미 추가순(createdAt)이라, 카테고리순일 때만 안정 정렬로 다시 줄 세운다.
			val rawMarkers = db.applicationDao().getMarkersInRange(startMillis, endMillis)
			val markers = if (sortMode == "CATEGORY") {
				val categoryOrder = loadCategoryOrderMap(db)
				rawMarkers.sortedBy { categoryOrder[it.categoryId] ?: Int.MAX_VALUE }
			} else {
				rawMarkers
			}

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
			// getByDate는 추가순(createdAt)으로 오므로, 카테고리순일 때만 안정 정렬로 다시 줄 세운다
			// (같은 카테고리 안에서는 추가순 유지).
			val applications = db.applicationDao().getByDate(selectedDate)
			val sorted = if (sortMode == "CATEGORY") {
				val categoryOrder = loadCategoryOrderMap(db)
				applications.sortedBy { categoryOrder[it.categoryId] ?: Int.MAX_VALUE }
			} else {
				applications
			}
			renderList(sorted)
		}
	}

	/** categoryId -> 적용 카테고리 관리 화면에서 보이는 순서(0부터). 미분류(null)도 관리 화면에서 옮겨둔
	 * 자리(AppSettings.getApplicationUncategorizedPosition, 기본은 맨 끝)를 그대로 따른다. */
	private suspend fun loadCategoryOrderMap(db: BibleDatabase): Map<Long?, Int> {
		val orderedIds: MutableList<Long?> = db.applicationCategoryDao().getAll()
			.sortedBy { it.sortOrder }
			.map { it.id as Long? }
			.toMutableList()
		val uncategorizedPosition =
			AppSettings.getApplicationUncategorizedPosition(requireContext())
				.coerceIn(0, orderedIds.size)
		orderedIds.add(uncategorizedPosition, null)
		return orderedIds.withIndex().associate { (index, id) -> id to index }
	}

	override fun getSortOptions() = listOf(
		"CATEGORY" to "카테고리순",
		"ADDED" to "추가순"
	)

	override fun getCurrentSortMode() = sortMode

	override fun setSortMode(mode: String) {
		sortMode = mode
		AppSettings.setApplicationSortMode(requireContext(), mode)
		loadCalendarGrid()
		loadApplicationsForSelectedDate()
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