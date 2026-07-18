package com.chan.bnote.ui.bible

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
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
					addRow("${memo.chapter}:${memo.verse}  ${memo.text}") {
						navigateToBible(memo.bookId, memo.chapter)
					}
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
					addRow("${memo.chapter}:${memo.verse}  ${memo.text}") {
						navigateToBible(memo.bookId, memo.chapter)
					}
				}
			}
		}
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

	private fun addRow(label: String, onClick: () -> Unit) {
		val row = TextView(this).apply {
			text = label
			textSize = 14f
			setTextColor(ContextCompat.getColor(this@MemoListActivity, R.color.text_primary))
			maxLines = 2
			ellipsize = TextUtils.TruncateAt.END
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@MemoListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener { onClick() }
		}
		container.addView(row)
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