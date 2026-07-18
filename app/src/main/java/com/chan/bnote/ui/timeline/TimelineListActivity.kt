package com.chan.bnote.ui.timeline

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
import com.chan.bnote.data.timeline.TimelineEvent
import com.chan.bnote.data.timeline.TimelineRepository
import kotlinx.coroutines.launch

class TimelineListActivity : AppCompatActivity() {

	private lateinit var container: LinearLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_timeline_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.timeline_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "연대표"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		container = findViewById(R.id.container_list)

		lifecycleScope.launch {
			val events = TimelineRepository.getAll(applicationContext)
			renderList(events)
		}
	}

	private fun renderList(events: List<TimelineEvent>) {
		container.removeAllViews()
		var currentEra = ""
		for (event in events) {
			if (event.era != currentEra) {
				currentEra = event.era
				addEraHeader(currentEra)
			}
			addEventRow(event)
		}
	}

	private fun addEraHeader(era: String) {
		val header = TextView(this).apply {
			text = era
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@TimelineListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addEventRow(event: TimelineEvent) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@TimelineListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener { navigateToBible(event.keyBookId, event.keyChapter) }
		}
		val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
		val titleView = TextView(this).apply {
			text = event.title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@TimelineListActivity, R.color.text_primary))
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		}
		val periodView = TextView(this).apply {
			text = event.period
			textSize = 11f
			setTextColor(ContextCompat.getColor(this@TimelineListActivity, R.color.text_hint))
		}
		titleRow.addView(titleView)
		titleRow.addView(periodView)

		val descView = TextView(this).apply {
			text = event.description
			textSize = 13f
			setTextColor(ContextCompat.getColor(this@TimelineListActivity, R.color.text_secondary))
			setPadding(0, dp(4), 0, 0)
		}

		row.addView(titleRow)
		row.addView(descView)
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