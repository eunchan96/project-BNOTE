package com.chan.bnote.ui.bible

import android.content.Context
import android.content.Intent
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
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import kotlinx.coroutines.launch

/** 하이라이트를 책별로 모아서 보여준다. 책을 누르면 그 책의 하이라이트만 보이는 화면으로 넘어간다. */
class HighlightListActivity : AppCompatActivity() {

	private lateinit var container: LinearLayout
	private lateinit var emptyText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_highlight_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.highlight_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "하이라이트"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		container = findViewById(R.id.container_list)
		emptyText = findViewById(R.id.text_empty)
		emptyText.text = "아직 하이라이트한 구절이 없어요."

		loadBooks()
	}

	override fun onResume() {
		super.onResume()
		loadBooks() // 책 상세에서 하이라이트를 지우고 돌아왔을 수도 있으니 다시 확인한다.
	}

	private fun loadBooks() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val highlights = db.partialHighlightDao().getAll()

			if (highlights.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				container.removeAllViews()
				return@launch
			}
			emptyText.visibility = View.GONE

			// bookId 순으로 이미 정렬돼서 오므로, 순서대로 세면서 묶으면 책별 개수가 자연스럽게 나온다.
			val countsByBook = highlights.groupingBy { it.bookId }.eachCount()

			container.removeAllViews()
			for ((bookId, count) in countsByBook) {
				addBookRow(bookId, count)
			}
		}
	}

	private fun addBookRow(bookId: Int, count: Int) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = Gravity.CENTER_VERTICAL
			setPadding(dp(16), dp(14), dp(16), dp(14))
			background = ContextCompat.getDrawable(
				this@HighlightListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
		}

		val name = TextView(this).apply {
			text = BibleBooks.nameOf(bookId)
			textSize = 16f
			setTextColor(ContextCompat.getColor(this@HighlightListActivity, R.color.text_primary))
			ellipsize = TextUtils.TruncateAt.END
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		}

		val countText = TextView(this).apply {
			text = "${count}개"
			textSize = 14f
			setTextColor(ContextCompat.getColor(this@HighlightListActivity, R.color.text_hint))
		}

		row.addView(name)
		row.addView(countText)
		row.setOnClickListener {
			startActivity(HighlightBookDetailActivity.createIntent(this, bookId))
		}
		container.addView(row)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	companion object {
		fun createIntent(context: Context): Intent =
			Intent(context, HighlightListActivity::class.java)
	}
}