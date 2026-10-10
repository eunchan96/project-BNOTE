package com.chan.bnote.ui.mypage.readingplan

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.readingplan.ReadingGoalStore
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import com.chan.bnote.ui.common.BookGrid
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ReadingPlanActivity : AppCompatActivity() {

	// 읽기 목표 화면에서 저장하고 돌아오면 진행률·하루 분량·그리드를 새 목표 기준으로 다시 그린다.
	private val goalLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == RESULT_OK) loadProgress()
	}

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
		findViewById<TextView>(R.id.btn_reading_goal).setOnClickListener {
			goalLauncher.launch(android.content.Intent(this, ReadingGoalActivity::class.java))
		}
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

			val readList = db.readingProgressDao().getAll()
			val readByBook = readList.groupBy { it.bookId }

			// 진행률·하루 분량·계획 대비는 "읽기 목표"(범위·기간) 기준으로 계산한다.
			// 목표를 안 바꿨으면 66권 전체 · 올해 1월 1일~12월 31일이라 예전과 같다.
			val goal = ReadingGoalStore.load(this@ReadingPlanActivity)
			val (progressText, progressPercent) =
				ReadingPace.progressLine(goal, maxChapterByBook, readList)
			findViewById<TextView>(R.id.text_overall_progress).text = progressText
			findViewById<ProgressBar>(R.id.progress_overall).progress = progressPercent
			findViewById<TextView>(R.id.text_pace_guide).text =
				ReadingPace.guideText(goal, maxChapterByBook, readList)

			renderBookGrid(maxChapterByBook, readByBook, goal.bookIds)
		}
	}

	private fun renderBookGrid(
		maxChapterByBook: Map<Int, Int>,
		readByBook: Map<Int, List<ReadingProgress>>,
		goalBookIds: Set<Int>
	) {
		val gridContainer = findViewById<LinearLayout>(R.id.container_book_progress_grid)
		gridContainer.removeAllViews()

		// 한 줄에 몇 권을 보여줄지(글꼴이 크거나 화면이 좁으면 3권)
		val gridColumns = BookGrid.columns(this)
		for (group in BibleBookGroups.groupsOf(gridColumns)) {
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
					// 읽기 목표 범위 밖의 책은 흐리게 보여준다(눌러서 체크하는 건 그대로 가능).
					alpha = if (bookId in goalBookIds) 1f else 0.35f
					setOnClickListener {
						val sheet = ReadingPlanChapterBottomSheet(bookId)
						sheet.onDismissed = { loadProgress() }
						sheet.show(supportFragmentManager, "reading_plan_chapter")
					}
				}

				val nameView = TextView(this).apply {
					text = BibleBooks.gridDisplayName(bookId)
					textSize = 13f
					maxLines = 3
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
			// 행이 다 안 차면(구약 마지막 줄 등), 남는 칸만큼 빈 스페이서를 넣어서
			// 실제 칸들이 한 줄 칸 수만큼 등분한 폭 그대로 유지되고 늘어나지 않게 한다.
			repeat(gridColumns - group.size) {
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