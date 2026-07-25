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

		loadHighlights()
	}

	override fun onResume() {
		super.onResume()
		loadHighlights()
	}

	private fun loadHighlights() {
		lifecycleScope.launch {
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

			var currentChapter = -1
			for (highlight in highlights) {
				if (highlight.chapter != currentChapter) {
					currentChapter = highlight.chapter
					addHeader("$currentChapter$unit")
				}

				val verses = verseTextCache.getOrPut(highlight.bookId to highlight.chapter) {
					db.bibleDao()
						.getVerses(highlight.translation, highlight.bookId, highlight.chapter)
				}
				val fullText = verses.find { it.verse == highlight.verse }?.text ?: ""
				val preview = if (
					highlight.startOffset in 0..fullText.length &&
					highlight.endOffset in highlight.startOffset..fullText.length
				) {
					fullText.substring(highlight.startOffset, highlight.endOffset)
				} else {
					fullText
				}

				addRow(
					label = "${highlight.verse}절  $preview",
					colorHex = highlight.colorHex,
					highlight = highlight
				)
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