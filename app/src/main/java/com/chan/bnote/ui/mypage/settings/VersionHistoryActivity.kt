package com.chan.bnote.ui.mypage.settings

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
import com.chan.bnote.R

class VersionHistoryActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_version_history)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.version_history_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "업데이트 내역"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val container = findViewById<LinearLayout>(R.id.container_version_history)
		val reversed = VersionHistory.entries.reversed()
		for ((index, entry) in reversed.withIndex()) {
			// 최신 버전(맨 위)만 기본으로 펼쳐두고, 나머지는 접어둔다.
			addEntry(container, entry, addTopDivider = index != 0, initiallyExpanded = index == 0)
		}
	}

	private fun addEntry(
		container: LinearLayout,
		entry: VersionHistory.Entry,
		addTopDivider: Boolean,
		initiallyExpanded: Boolean
	) {
		if (addTopDivider) {
			val divider = android.view.View(this).apply {
				layoutParams =
					LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
				setBackgroundColor(
					ContextCompat.getColor(
						this@VersionHistoryActivity,
						R.color.divider_light
					)
				)
			}
			container.addView(divider)
		}

		val block = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
		}

		var isExpanded = initiallyExpanded

		val header = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			gravity = android.view.Gravity.CENTER_VERTICAL
			setPadding(dp(16), dp(16), dp(16), dp(16))
			isClickable = true
			isFocusable = true
			background = ContextCompat.getDrawable(
				this@VersionHistoryActivity,
				android.R.drawable.list_selector_background
			)
		}

		val versionView = TextView(this).apply {
			text = "v${entry.version}"
			textSize = 16f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@VersionHistoryActivity, R.color.brown_primary))
			layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
		}
		val arrowView = TextView(this).apply {
			text = if (isExpanded) "▾" else "▸"
			textSize = 16f
			setTextColor(ContextCompat.getColor(this@VersionHistoryActivity, R.color.text_hint))
		}
		header.addView(versionView)
		header.addView(arrowView)

		val changesContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), 0, dp(16), dp(16))
			visibility = if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
		}
		for (change in entry.changes) {
			val changeView = TextView(this).apply {
				text = "· $change"
				textSize = 14f
				setTextColor(
					ContextCompat.getColor(
						this@VersionHistoryActivity,
						R.color.text_primary
					)
				)
				setPadding(0, dp(6), 0, 0)
				setLineSpacing(dp(2).toFloat(), 1f)
			}
			changesContainer.addView(changeView)
		}

		header.setOnClickListener {
			isExpanded = !isExpanded
			changesContainer.visibility =
				if (isExpanded) android.view.View.VISIBLE else android.view.View.GONE
			arrowView.text = if (isExpanded) "▾" else "▸"
		}

		block.addView(header)
		block.addView(changesContainer)
		container.addView(block)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}