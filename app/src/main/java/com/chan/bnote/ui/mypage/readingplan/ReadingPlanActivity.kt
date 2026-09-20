package com.chan.bnote.ui.mypage.readingplan

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ReadingPlanActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_reading_plan)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reading_plan_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "성경읽기표"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.btn_reset_reading_progress).setOnClickListener {
			MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("성경읽기표 기록 초기화")
				.setMessage("지금까지 읽음 표시한 모든 기록이 사라져요. 계속할까요?")
				.setPositiveButton("초기화") { _, _ ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.readingProgressDao().resetAll()
						loadProgress()
					}
				}
				.setNegativeButton("취소", null)
				.show()
		}

		loadProgress()
	}

	private fun loadProgress() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			// 책별 총 장수 계산
			val maxChapterByBook = (1..66).associateWith { bookId ->
				db.bibleDao().getMaxChapter("NKRV", bookId)
			}
			val totalChapters = maxChapterByBook.values.sum()

			val readList = db.readingProgressDao().getAll()
			val readByBook = readList.groupBy { it.bookId }
			val totalRead = readList.size

			val overallText = findViewById<TextView>(R.id.text_overall_progress)
			val progressBar = findViewById<ProgressBar>(R.id.progress_overall)
			val percentValue = if (totalChapters > 0) (totalRead * 100.0 / totalChapters) else 0.0
			val percentText = String.format(java.util.Locale.KOREA, "%.1f", percentValue)
			overallText.text = "전체 $totalRead / $totalChapters 장 읽음 (${percentText}%)"
			progressBar.progress = percentValue.toInt()

			renderPaceGuide(totalChapters, totalRead)
			renderBookGrid(maxChapterByBook, readByBook)
		}
	}

	/** 올해 남은 날짜와, 그 안에 완독하려면 하루 몇 장씩 읽어야 하는지 보여준다. */
	private fun renderPaceGuide(totalChapters: Int, totalRead: Int) {
		val paceText = findViewById<TextView>(R.id.text_pace_guide)
		val remaining = totalChapters - totalRead

		if (remaining <= 0) {
			paceText.text = "축하해요, 전체 성경을 다 읽으셨어요!"
			return
		}

		val today = java.util.Calendar.getInstance()
		val dayOfYear = today.get(java.util.Calendar.DAY_OF_YEAR)
		val daysInYear = today.getActualMaximum(java.util.Calendar.DAY_OF_YEAR)
		val daysLeft = (daysInYear - dayOfYear + 1).coerceAtLeast(1) // 오늘 포함

		val dailyPace = remaining.toDouble() / daysLeft
		val dailyPaceText = String.format(java.util.Locale.KOREA, "%.1f", dailyPace)
		paceText.text = "올해 남은 날짜 ${daysLeft}일 · 다 읽으려면 하루 ${dailyPaceText}장씩"
	}

	private fun renderBookGrid(
		maxChapterByBook: Map<Int, Int>,
		readByBook: Map<Int, List<ReadingProgress>>
	) {
		val gridContainer = findViewById<LinearLayout>(R.id.container_book_progress_grid)
		gridContainer.removeAllViews()

		for (group in BibleBookGroups.groups) {
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(8) }
			}
			for (bookId in group) {
				val maxChapter = maxChapterByBook[bookId] ?: 1
				val readCount = readByBook[bookId]?.size ?: 0

				val bgRes = when {
					readCount == 0 -> R.drawable.bg_book_progress_none
					readCount >= maxChapter -> R.drawable.bg_book_progress_done
					else -> R.drawable.bg_book_progress_partial
				}
				val textColorRes =
					if (readCount >= maxChapter && readCount > 0) R.color.white else R.color.book_progress_none_text
				val textColor = ContextCompat.getColor(this, textColorRes)

				val container = LinearLayout(this).apply {
					orientation = LinearLayout.VERTICAL
					gravity = Gravity.CENTER
					setPadding(dp(4), dp(12), dp(4), dp(12))
					background = ContextCompat.getDrawable(this@ReadingPlanActivity, bgRes)
					isClickable = true
					isFocusable = true
					// 대부분의 책 이름은 한 줄이라 칸이 작지만, "데살로니가전서"처럼 두 줄이 되는
					// 이름이 있는 줄(row)은 그 줄만 자연스럽게 커진다(전체 그리드가 다 같이 커지지
					// 않도록, MATCH_PARENT로 같은 줄의 제일 큰 칸에 맞춰지게 한다).
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						val sheet = ReadingPlanChapterBottomSheet(bookId)
						sheet.onDismissed = { loadProgress() }
						sheet.show(supportFragmentManager, "reading_plan_chapter")
					}
				}

				val nameView = TextView(this).apply {
					text = BibleBooks.gridDisplayName(bookId)
					textSize = 13f
					maxLines = 2
					gravity = Gravity.CENTER
					setTextColor(textColor)
				}
				val countView = TextView(this).apply {
					text = "$readCount/$maxChapter"
					textSize = 10f
					gravity = Gravity.CENTER
					setTextColor(textColor)
					alpha = 0.8f
				}

				container.addView(nameView)
				container.addView(countView)
				row.addView(container)
			}
			// 행에 4개 미만이면(구약 마지막 줄 등), 남는 칸만큼 빈 스페이서를 넣어서
			// 실제 칸들이 4등분 폭 그대로 유지되고 늘어나지 않게 한다.
			repeat(4 - group.size) {
				row.addView(View(this).apply {
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
							.apply {
								marginStart = dp(4)
								marginEnd = dp(4)
							}
				})
			}
			gridContainer.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}