package com.chan.bnote.ui.bible

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 한 책의 하이라이트만 모아서 장별로 보여준다. HighlightListActivity(책 목록)에서 책을 누르면 열린다. */
class HighlightBookDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_BOOK_ID = "extra_book_id"

		fun createIntent(context: Context, bookId: Int): Intent =
			Intent(context, HighlightBookDetailActivity::class.java)
				.putExtra(EXTRA_BOOK_ID, bookId)
	}

	private var bookId: Int = 1
	private lateinit var container: LinearLayout
	private lateinit var emptyText: TextView
	private var loadJob: Job? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_highlight_list)

		bookId = intent.getIntExtra(EXTRA_BOOK_ID, 1)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.highlight_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = BibleBooks.nameOf(bookId)
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		container = findViewById(R.id.container_list)
		emptyText = findViewById(R.id.text_empty)
		emptyText.text = "이 책엔 하이라이트한 구절이 없어요."
		// onResume이 onCreate 직후에도 항상 한 번 불리므로, 목록은 거기서만 불러온다
		// (여기서도 부르면 두 번 겹쳐 불러오게 된다).
	}

	override fun onResume() {
		super.onResume()
		loadHighlights()
	}

	/** 화면을 열 때마다(혹은 다시 돌아올 때마다) 이전 로딩이 아직 끝나지 않았으면 취소하고 새로
	 * 시작한다 — 두 번 겹쳐 불려서 목록이 이상하게 섞이는 걸 막기 위한 안전장치. */
	private fun loadHighlights() {
		loadJob?.cancel()
		loadJob = lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val highlights = db.partialHighlightDao().getAll().filter { it.bookId == bookId }

			if (highlights.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				container.removeAllViews()
				return@launch
			}
			emptyText.visibility = View.GONE

			val unit = BibleBooks.chapterUnit(bookId)
			val verseTextCache = mutableMapOf<Pair<Int, Int>, List<BibleVerse>>()
			container.removeAllViews()

			// chapter별로 확실하게 묶는다(정렬 순서에 기대지 않고, 진짜로 같은 장인 것끼리 모은다).
			val byChapter = highlights.groupBy { it.chapter }.toSortedMap()
			for ((chapter, chapterHighlights) in byChapter) {
				addHeader("$chapter$unit")

				// 절이 소제목으로 둘로 나뉘는 예외 구절은 같은 절에 하이라이트가 segment별로 2개
				// 생기는데(둘 다 덮으려고), 목록에서는 한 절이니까 한 줄로 합쳐서 보여준다.
				val byVerse = chapterHighlights.groupBy { it.verse }.toSortedMap()
				for ((verseNum, verseHighlights) in byVerse) {
					val sorted = verseHighlights.sortedBy { it.segment }
					val previewParts = sorted.map { highlight ->
						val verses =
							verseTextCache.getOrPut(highlight.bookId to highlight.chapter) {
								db.bibleDao().getVerses(
									highlight.translation,
									highlight.bookId,
									highlight.chapter
								)
							}
						val verseRow = verses.find { it.verse == highlight.verse }
						val fullText =
							if (highlight.segment == 1) verseRow?.text2 else verseRow?.text
						val safeText = fullText ?: ""
						if (
							highlight.startOffset in 0..safeText.length &&
							highlight.endOffset in highlight.startOffset..safeText.length
						) {
							safeText.substring(highlight.startOffset, highlight.endOffset)
						} else {
							safeText
						}
					}

					addRow(
						label = "$verseNum" + "절  " + previewParts.joinToString(" "),
						colorHex = sorted.first().colorHex,
						highlight = sorted.first()
					)
				}
			}
		}
	}

	private fun addHeader(label: String) {
		val header = TextView(this).apply {
			text = label
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(
				ContextCompat.getColor(
					this@HighlightBookDetailActivity,
					R.color.brown_primary
				)
			)
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addRow(label: String, colorHex: String, highlight: PartialHighlight) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@HighlightBookDetailActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
		}

		val swatch = View(this).apply {
			layoutParams = LinearLayout.LayoutParams(dp(12), dp(12))
			background = GradientDrawable().apply {
				shape = GradientDrawable.OVAL
				setColor(Color.parseColor(colorHex))
			}
		}

		val text = TextView(this).apply {
			text = label
			textSize = 14f
			setTextColor(
				ContextCompat.getColor(
					this@HighlightBookDetailActivity,
					R.color.text_primary
				)
			)
			maxLines = 2
			ellipsize = TextUtils.TruncateAt.END
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
				.apply { marginStart = dp(10) }
		}

		row.addView(swatch)
		row.addView(text)
		row.setOnClickListener {
			navigateToBible(
				highlight.bookId,
				highlight.chapter,
				highlight.verse
			)
		}
		container.addView(row)
	}

	private fun navigateToBible(bookId: Int, chapter: Int, verse: Int) {
		val intent = Intent(this, MainActivity::class.java).apply {
			putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
			putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			putExtra(MainActivity.EXTRA_NAVIGATE_VERSE, verse)
			flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		startActivity(intent)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}