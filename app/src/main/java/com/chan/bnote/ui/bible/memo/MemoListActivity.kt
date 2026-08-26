package com.chan.bnote.ui.bible.memo

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.ui.bible.picker.PickerTab
import com.chan.bnote.ui.bible.picker.renderPickerTabs
import kotlinx.coroutines.launch

class MemoListActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_INITIAL_TAB = "extra_initial_tab"
		private const val TAB_VERSE = 0
		private const val TAB_WORD = 1

		fun verseMemoIntent(context: Context): Intent =
			Intent(context, MemoListActivity::class.java).putExtra(EXTRA_INITIAL_TAB, TAB_VERSE)

		fun wordMemoIntent(context: Context): Intent =
			Intent(context, MemoListActivity::class.java).putExtra(EXTRA_INITIAL_TAB, TAB_WORD)
	}

	private lateinit var tabBarContainer: LinearLayout
	private lateinit var container: LinearLayout
	private lateinit var emptyText: TextView

	private var currentTab = TAB_VERSE

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memo_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memo_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "메모"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		tabBarContainer = findViewById(R.id.container_tab_bar)
		container = findViewById(R.id.container_list)
		emptyText = findViewById(R.id.text_empty)

		currentTab = intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_VERSE)
		renderTabs()
		loadCurrentTab()
	}

	private fun renderTabs() {
		val tabs = listOf(
			PickerTab(label = "구절 메모", enabled = true, selected = currentTab == TAB_VERSE) {
				currentTab = TAB_VERSE
				renderTabs()
				loadCurrentTab()
			},
			PickerTab(label = "단어 메모", enabled = true, selected = currentTab == TAB_WORD) {
				currentTab = TAB_WORD
				renderTabs()
				loadCurrentTab()
			}
		)
		renderPickerTabs(this, tabBarContainer, tabs)
	}

	private fun loadCurrentTab() {
		if (currentTab == TAB_VERSE) loadVerseMemos() else loadWordMemos()
	}

	private fun loadVerseMemos() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val memos = db.verseMemoDao().getAll()
			renderGrouped(memos.isEmpty()) {
				var currentBookId = -1
				for (memo in memos) {
					if (memo.bookId != currentBookId) {
						currentBookId = memo.bookId
						addHeader(BibleBooks.nameOf(currentBookId))
					}
					addRow(
						label = buildStyledLabel("${memo.chapter}:${memo.verse}", memo.text),
						onClickEdit = { openVerseMemoEditor(memo) }
					)
				}
			}
		}
	}

	private fun loadWordMemos() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val memos = db.wordMemoDao().getAll()
			renderGrouped(memos.isEmpty()) {
				var currentBookId = -1
				for (memo in memos) {
					if (memo.bookId != currentBookId) {
						currentBookId = memo.bookId
						addHeader(BibleBooks.nameOf(currentBookId))
					}
					// 실제로는 비동기 조회가 필요해서, 우선 뼈대만 넣고 뒤에서 단어를 채워 넣는다.
					val row = addRow(
						label = buildStyledLabel("${memo.chapter}:${memo.verse}", memo.text),
						onClickEdit = { openWordMemoEditor(memo, db) }
					)
					lifecycleScope.launch {
						val word = fetchWordMemoWord(db, memo)
						if (word.isNotEmpty()) {
							row.text =
								buildStyledLabel("${memo.chapter}:${memo.verse} $word", memo.text)
						}
					}
				}
			}
		}
	}

	private suspend fun fetchWordMemoWord(db: BibleDatabase, memo: WordMemo): String {
		val verseText = db.bibleDao().getVerses(memo.translation, memo.bookId, memo.chapter)
			.find { it.verse == memo.verse } ?: return ""
		if (memo.startOffset < 0 || memo.endOffset > verseText.text.length || memo.startOffset >= memo.endOffset) return ""
		return verseText.text.substring(memo.startOffset, memo.endOffset)
	}

	private fun openVerseMemoEditor(memo: VerseMemo) {
		navigateToBibleAndOpenVerseMemo(memo.bookId, memo.chapter, memo.verse)
	}

	private fun openWordMemoEditor(memo: WordMemo, db: BibleDatabase) {
		navigateToBibleAndOpenWordMemo(
			memo.bookId, memo.chapter, memo.verse, memo.startOffset, memo.endOffset, memo.segment
		)
	}

	/** 앞부분([styledPrefix])만 굵게로 표시하고, 뒷부분은 평범하게 이어붙인다. */
	private fun buildStyledLabel(styledPrefix: String, plainSuffix: String): CharSequence {
		val full = "$styledPrefix  $plainSuffix"
		val spannable = SpannableString(full)
		spannable.setSpan(
			StyleSpan(Typeface.BOLD),
			0,
			styledPrefix.length,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		spannable.setSpan(
			ForegroundColorSpan(ContextCompat.getColor(this, R.color.text_primary)),
			0, styledPrefix.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		return spannable
	}

	private inline fun renderGrouped(isEmpty: Boolean, build: () -> Unit) {
		emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
		container.removeAllViews()
		if (!isEmpty) build()
	}

	private fun addHeader(bookName: String) {
		if (container.childCount > 0) {
			val divider = View(this).apply {
				layoutParams =
					LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
						topMargin = dp(8)
					}
				setBackgroundColor(
					ContextCompat.getColor(
						this@MemoListActivity,
						R.color.divider_light
					)
				)
			}
			container.addView(divider)
		}

		val header = TextView(this).apply {
			text = bookName
			textSize = 14f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@MemoListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	/** 행 전체를 누르면 그 메모가 있는 구절로 이동해서 편집 시트가 자동으로 열린다. 콘텐츠
	 * TextView를 반환해서 나중에 텍스트를 바꿔 넣을 수 있게 한다. */
	private fun addRow(label: CharSequence, onClickEdit: () -> Unit): TextView {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			background = ContextCompat.getDrawable(
				this@MemoListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener { onClickEdit() }
		}

		val contentView = TextView(this).apply {
			text = label
			textSize = 14f
			setTextColor(ContextCompat.getColor(this@MemoListActivity, R.color.text_primary))
			maxLines = 2
			ellipsize = TextUtils.TruncateAt.END
			setPadding(dp(16), dp(10), dp(16), dp(10))
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		}

		row.addView(contentView)
		container.addView(row)
		return contentView
	}

	/** 행 전체를 눌렀을 때: 그 구절로 이동하고, 도착하면 구절 메모 편집 시트를 자동으로 띄운다. */
	private fun navigateToBibleAndOpenVerseMemo(bookId: Int, chapter: Int, verse: Int) {
		val intent = Intent(this, MainActivity::class.java).apply {
			putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
			putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			putExtra(MainActivity.EXTRA_NAVIGATE_VERSE, verse)
			putExtra(MainActivity.EXTRA_NAVIGATE_OPEN_VERSE_MEMO, true)
			flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		startActivity(intent)
	}

	/** 위와 같은 이유로, 단어 메모 버전. */
	private fun navigateToBibleAndOpenWordMemo(
		bookId: Int,
		chapter: Int,
		verse: Int,
		startOffset: Int,
		endOffset: Int,
		segment: Int
	) {
		val intent = Intent(this, MainActivity::class.java).apply {
			putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
			putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			putExtra(MainActivity.EXTRA_NAVIGATE_VERSE, verse)
			putExtra(MainActivity.EXTRA_NAVIGATE_OPEN_WORD_MEMO, true)
			putExtra(MainActivity.EXTRA_NAVIGATE_WORD_START, startOffset)
			putExtra(MainActivity.EXTRA_NAVIGATE_WORD_END, endOffset)
			putExtra(MainActivity.EXTRA_NAVIGATE_WORD_SEGMENT, segment)
			flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		startActivity(intent)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}