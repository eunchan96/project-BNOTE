package com.chan.bnote.ui.mypage.readingplan

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.readingplan.ReadingGoal
import com.chan.bnote.data.mypage.readingplan.ReadingGoalStore
import com.chan.bnote.ui.common.BookGrid
import com.chan.bnote.ui.common.UnsavedChangesDialog
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/**
 * 성경읽기표의 "읽기 목표" 설정 화면 — 기간(시작일·종료일)과 읽을 책(범위)을 정한다.
 * 저장하기를 눌러야 반영되고, 바꾼 게 있는데 뒤로 나가면 나갈지 물어본다.
 */
class ReadingGoalActivity : AppCompatActivity() {

	private lateinit var originalGoal: ReadingGoal
	private var startMillis = 0L
	private var endMillis = 0L
	private val selectedBooks = mutableSetOf<Int>()

	// 책별 장 수(선택한 범위가 몇 장인지 보여줄 때 씀). 불러오기 전엔 비어 있다.
	private var maxChapterByBook: Map<Int, Int> = emptyMap()
	private val bookCells = mutableMapOf<Int, Pair<LinearLayout, List<TextView>>>()

	private lateinit var btnStartDate: TextView
	private lateinit var btnEndDate: TextView
	private lateinit var textPeriodSummary: TextView
	private lateinit var textBooksSummary: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_reading_goal)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reading_goal_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		originalGoal = ReadingGoalStore.load(this)
		startMillis = originalGoal.startMillis
		endMillis = originalGoal.endMillis
		selectedBooks.addAll(originalGoal.bookIds)

		btnStartDate = findViewById(R.id.btn_goal_start_date)
		btnEndDate = findViewById(R.id.btn_goal_end_date)
		textPeriodSummary = findViewById(R.id.text_goal_period_summary)
		textBooksSummary = findViewById(R.id.text_goal_books_summary)

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() = handleBackPress()
		})

		btnStartDate.setOnClickListener {
			pickDate(startMillis) { picked ->
				startMillis = picked
				// 시작일을 종료일보다 뒤로 옮기면 종료일도 같이 맞춘다.
				if (endMillis < startMillis) endMillis = startMillis
				renderPeriod()
			}
		}
		btnEndDate.setOnClickListener {
			pickDate(endMillis) { picked ->
				endMillis = picked
				if (startMillis > endMillis) startMillis = endMillis
				renderPeriod()
			}
		}

		findViewById<TextView>(R.id.btn_goal_all).setOnClickListener {
			setSelection(ReadingGoalStore.ALL_BOOKS)
		}
		findViewById<TextView>(R.id.btn_goal_old).setOnClickListener {
			setSelection(ReadingGoalStore.OLD_TESTAMENT)
		}
		findViewById<TextView>(R.id.btn_goal_new).setOnClickListener {
			setSelection(ReadingGoalStore.NEW_TESTAMENT)
		}
		findViewById<TextView>(R.id.btn_goal_none).setOnClickListener { setSelection(emptySet()) }

		findViewById<TextView>(R.id.btn_reading_goal_default).setOnClickListener {
			val def = ReadingGoalStore.defaultGoal()
			startMillis = def.startMillis
			endMillis = def.endMillis
			renderPeriod()
			setSelection(def.bookIds)
		}
		findViewById<TextView>(R.id.btn_save_reading_goal).setOnClickListener { save() }

		renderPeriod()

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			maxChapterByBook = (1..66).associateWith { db.bibleDao().getMaxChapter("NKRV", it) }
			renderBookGrid()
			renderBooksSummary()
		}
	}

	private fun pickDate(initial: Long, onPicked: (Long) -> Unit) {
		val cal = Calendar.getInstance().apply { timeInMillis = initial }
		DatePickerDialog(
			this,
			{ _, year, month, day ->
				val picked = Calendar.getInstance()
				picked.set(year, month, day, 0, 0, 0)
				onPicked(DateUtils.normalizeToDayStart(picked.timeInMillis))
			},
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
		).show()
	}

	private fun underlined(text: String): SpannableString =
		SpannableString(text).apply {
			setSpan(UnderlineSpan(), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		}

	private fun renderPeriod() {
		btnStartDate.text = underlined(DateUtils.formatDate(startMillis))
		btnEndDate.text = underlined(DateUtils.formatDate(endMillis))
		textPeriodSummary.text = "총 ${ReadingPace.daysInclusive(startMillis, endMillis)}일"
	}

	private fun setSelection(books: Set<Int>) {
		selectedBooks.clear()
		selectedBooks.addAll(books)
		bookCells.keys.forEach { styleCell(it) }
		renderBooksSummary()
	}

	private fun renderBooksSummary() {
		val chapters = selectedBooks.sumOf { maxChapterByBook[it] ?: 0 }
		textBooksSummary.text = when {
			selectedBooks.isEmpty() -> "선택한 책 없음"
			maxChapterByBook.isEmpty() -> "${selectedBooks.size}권"
			else -> String.format(Locale.KOREA, "%d권 · %,d장", selectedBooks.size, chapters)
		}
	}

	private fun renderBookGrid() {
		val gridContainer = findViewById<LinearLayout>(R.id.container_goal_book_grid)
		gridContainer.removeAllViews()
		bookCells.clear()

		val gridColumns = BookGrid.columns(this)
		for (group in BibleBookGroups.groupsOf(gridColumns)) {
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(8) }
			}
			for (bookId in group) {
				val cell = LinearLayout(this).apply {
					orientation = LinearLayout.VERTICAL
					gravity = Gravity.CENTER
					setPadding(dp(4), dp(12), dp(4), dp(12))
					isClickable = true
					isFocusable = true
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						if (!selectedBooks.add(bookId)) selectedBooks.remove(bookId)
						styleCell(bookId)
						renderBooksSummary()
					}
				}
				val nameView = TextView(this).apply {
					text = BibleBooks.gridDisplayName(bookId)
					textSize = 13f
					maxLines = 3
					gravity = Gravity.CENTER
				}
				val countView = TextView(this).apply {
					text = "${maxChapterByBook[bookId] ?: 0}장"
					textSize = 10f
					gravity = Gravity.CENTER
					alpha = 0.8f
				}
				cell.addView(nameView)
				cell.addView(countView)
				row.addView(cell)
				bookCells[bookId] = cell to listOf(nameView, countView)
				styleCell(bookId)
			}
			repeat(gridColumns - group.size) {
				row.addView(View(this).apply {
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
				})
			}
			gridContainer.addView(row)
		}
	}

	/** 고른 책은 읽기표의 "다 읽은 책"처럼 진한 갈색, 안 고른 책은 기본 회색 칸으로 보여준다. */
	private fun styleCell(bookId: Int) {
		val (cell, texts) = bookCells[bookId] ?: return
		val selected = bookId in selectedBooks
		cell.background = ContextCompat.getDrawable(
			this,
			if (selected) R.drawable.bg_book_progress_done else R.drawable.bg_book_progress_none
		)
		val color = ContextCompat.getColor(
			this, if (selected) R.color.white else R.color.book_progress_none_text
		)
		texts.forEach { it.setTextColor(color) }
	}

	private fun currentGoal() = ReadingGoal(selectedBooks.toSet(), startMillis, endMillis)

	private fun hasChanges(): Boolean = currentGoal() != originalGoal

	private fun save() {
		if (selectedBooks.isEmpty()) {
			Toast.makeText(this, "읽을 책을 한 권 이상 골라주세요", Toast.LENGTH_SHORT).show()
			return
		}
		ReadingGoalStore.save(this, currentGoal())
		setResult(RESULT_OK)
		finish()
	}

	private fun handleBackPress() {
		if (!hasChanges()) {
			finish()
			return
		}
		UnsavedChangesDialog.show(this, onDiscard = { finish() })
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}