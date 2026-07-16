package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.view.Gravity
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
import com.chan.bnote.data.mypage.ReadingProgress
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
			val percent = if (totalChapters > 0) (totalRead * 100 / totalChapters) else 0
			overallText.text = "전체 $totalRead / $totalChapters 장 읽음 ($percent%)"
			progressBar.progress = percent

			renderBookGrid(maxChapterByBook, readByBook)
		}
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
				).apply { bottomMargin = dp(4) }
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
					layoutParams =
						LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						val sheet = ReadingPlanChapterBottomSheet(bookId)
						sheet.onDismissed = { loadProgress() }
						sheet.show(supportFragmentManager, "reading_plan_chapter")
					}
				}

				val nameView = TextView(this).apply {
					text = BibleBooks.nameOf(bookId)
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
			gridContainer.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}