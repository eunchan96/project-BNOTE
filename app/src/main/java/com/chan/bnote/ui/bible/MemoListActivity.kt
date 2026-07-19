package com.chan.bnote.ui.bible

import android.app.Activity
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.memo.VerseMemo
import com.chan.bnote.data.memo.WordMemo
import kotlinx.coroutines.launch

class MemoListActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_INITIAL_TAB = "extra_initial_tab"
		private const val EXTRA_OPEN_VERSE_MEMO_ID = "extra_open_verse_memo_id"
		private const val EXTRA_OPEN_WORD_MEMO_ID = "extra_open_word_memo_id"
		private const val TAB_VERSE = 0
		private const val TAB_WORD = 1

		fun verseMemoIntent(context: Context): Intent =
			Intent(context, MemoListActivity::class.java).putExtra(EXTRA_INITIAL_TAB, TAB_VERSE)

		fun wordMemoIntent(context: Context): Intent =
			Intent(context, MemoListActivity::class.java).putExtra(EXTRA_INITIAL_TAB, TAB_WORD)

		/** 목록을 거치지 않고 특정 구절 메모의 편집 화면으로 바로 들어간다 (예: 마이페이지 최근 활동 칩). */
		fun verseMemoEditIntent(context: Context, memoId: Long): Intent =
			verseMemoIntent(context).putExtra(EXTRA_OPEN_VERSE_MEMO_ID, memoId)

		/** 목록을 거치지 않고 특정 단어 메모의 편집 화면으로 바로 들어간다. */
		fun wordMemoEditIntent(context: Context, memoId: Long): Intent =
			wordMemoIntent(context).putExtra(EXTRA_OPEN_WORD_MEMO_ID, memoId)
	}

	private lateinit var tabBarContainer: LinearLayout
	private lateinit var container: LinearLayout
	private lateinit var emptyText: TextView

	private var currentTab = TAB_VERSE

	private val verseMemoEditorLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			lifecycleScope.launch { loadCurrentTab() }
		}
	}

	private val wordMemoEditorLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			lifecycleScope.launch { loadCurrentTab() }
		}
	}

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

		openRequestedMemoDirectly()
	}

	private fun openRequestedMemoDirectly() {
		val verseMemoId = intent.getLongExtra(EXTRA_OPEN_VERSE_MEMO_ID, -1L)
		val wordMemoId = intent.getLongExtra(EXTRA_OPEN_WORD_MEMO_ID, -1L)
		if (verseMemoId == -1L && wordMemoId == -1L) return

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			if (verseMemoId != -1L) {
				db.verseMemoDao().getById(verseMemoId)?.let { openVerseMemoEditor(it) }
			} else if (wordMemoId != -1L) {
				db.wordMemoDao().getById(wordMemoId)?.let { openWordMemoEditor(it, db) }
			}
		}
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
						onClickEdit = { openVerseMemoEditor(memo) },
						onClickGoToVerse = { navigateToBible(memo.bookId, memo.chapter) }
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
					val suffix =
						if (memo.sourceLabel != null) "${memo.text} (from ${memo.sourceLabel})" else memo.text
					val row = addRow(
						label = buildStyledLabel("${memo.chapter}:${memo.verse}", suffix),
						onClickEdit = { openWordMemoEditor(memo, db) },
						onClickGoToVerse = { navigateToBible(memo.bookId, memo.chapter) }
					)
					lifecycleScope.launch {
						val word = fetchWordMemoWord(db, memo)
						if (word.isNotEmpty()) {
							row.text =
								buildStyledLabel("${memo.chapter}:${memo.verse} $word", suffix)
						}
					}
				}
			}
		}
	}

	private suspend fun fetchWordMemoWord(db: BibleDatabase, memo: WordMemo): String {
		val verseText = db.bibleDao().getVerses(memo.translation, memo.bookId, memo.chapter)
			.find { it.verse == memo.verse }?.text ?: return ""
		if (memo.startOffset < 0 || memo.endOffset > verseText.length || memo.startOffset >= memo.endOffset) return ""
		return verseText.substring(memo.startOffset, memo.endOffset)
	}

	private fun openVerseMemoEditor(memo: VerseMemo) {
		verseMemoEditorLauncher.launch(
			VerseMemoEditorActivity.createIntent(
				context = this@MemoListActivity,
				bookId = memo.bookId,
				chapter = memo.chapter,
				verse = memo.verse
			)
		)
	}

	private fun openWordMemoEditor(memo: WordMemo, db: BibleDatabase) {
		wordMemoEditorLauncher.launch(
			WordMemoEditorActivity.createIntent(
				context = this@MemoListActivity,
				translation = memo.translation,
				bookId = memo.bookId,
				chapter = memo.chapter,
				verse = memo.verse,
				startOffset = memo.startOffset,
				endOffset = memo.endOffset
			)
		)
	}

	/** 앞부분([styledPrefix])만 굵게 + 강조색으로 표시하고, 뒷부분은 평범하게 이어붙인다. */
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
			ForegroundColorSpan(ContextCompat.getColor(this, R.color.brown_primary)),
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
		val header = TextView(this).apply {
			text = bookName
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@MemoListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	/** 행 전체를 누르면 메모 수정, 오른쪽의 작은 버튼을 누르면 해당 구절로 이동. 콘텐츠 TextView를 반환해서 나중에 텍스트를 바꿔 넣을 수 있게 한다. */
	private fun addRow(
		label: CharSequence,
		onClickEdit: () -> Unit,
		onClickGoToVerse: () -> Unit
	): TextView {
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
			setPadding(dp(16), dp(10), dp(8), dp(10))
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		}

		val goToVerseBtn = ImageView(this).apply {
			setImageResource(R.drawable.ic_book_open)
			imageTintList =
				ContextCompat.getColorStateList(this@MemoListActivity, R.color.icon_action_tint)
			contentDescription = "구절로 이동"
			background = ContextCompat.getDrawable(
				this@MemoListActivity, android.R.drawable.list_selector_background
			)
			setPadding(dp(10), dp(10), dp(10), dp(10))
			isClickable = true
			isFocusable = true
			layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(8) }
			setOnClickListener { onClickGoToVerse() }
		}

		row.addView(contentView)
		row.addView(goToVerseBtn)
		container.addView(row)
		return contentView
	}

	private fun navigateToBible(bookId: Int, chapter: Int) {
		val intent = Intent(this, MainActivity::class.java).apply {
			putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
			putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
		}
		startActivity(intent)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}