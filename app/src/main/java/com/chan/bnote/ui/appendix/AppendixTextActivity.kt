package com.chan.bnote.ui.appendix

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R
import com.chan.bnote.data.appendix.AppendixLoader
import com.chan.bnote.data.appendix.TextVersion
import com.chan.bnote.data.appendix.VersionedTextContent

class AppendixTextActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_TYPE = "extra_appendix_text_type"

		fun start(context: Context, type: AppendixTextType) {
			val intent = Intent(context, AppendixTextActivity::class.java)
			intent.putExtra(EXTRA_TYPE, type.name)
			context.startActivity(intent)
		}
	}

	private lateinit var content: VersionedTextContent
	private var selectedIndex = 0

	private lateinit var tabContainer: LinearLayout
	private lateinit var linesContainer: LinearLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_appendix_text)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appendix_text_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val type = AppendixTextType.valueOf(
			intent.getStringExtra(EXTRA_TYPE) ?: AppendixTextType.LORDS_PRAYER.name
		)
		content = when (type) {
			AppendixTextType.LORDS_PRAYER -> AppendixLoader.loadLordsPrayer(this)
			AppendixTextType.APOSTLES_CREED -> AppendixLoader.loadApostlesCreed(this)
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = content.title
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		tabContainer = findViewById(R.id.container_version_tabs)
		linesContainer = findViewById(R.id.container_appendix_lines)

		buildTabs()
		renderVersion(selectedIndex)
	}

	private fun buildTabs() {
		tabContainer.removeAllViews()
		content.versions.forEachIndexed { index, version ->
			val tab = TextView(this).apply {
				text = version.label
				textSize = 14f
				gravity = Gravity.CENTER
				setPadding(dp(14), dp(12), dp(14), dp(12))
				layoutParams = LinearLayout.LayoutParams(
					0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
				)
				background = ContextCompat.getDrawable(
					this@AppendixTextActivity, android.R.drawable.list_selector_background
				)
				isClickable = true
				isFocusable = true
				setOnClickListener {
					selectedIndex = index
					renderVersion(index)
					updateTabStyles()
				}
			}
			tabContainer.addView(tab)
		}
		updateTabStyles()
	}

	private fun updateTabStyles() {
		for (i in 0 until tabContainer.childCount) {
			val tab = tabContainer.getChildAt(i) as TextView
			if (i == selectedIndex) {
				tab.setTextColor(ContextCompat.getColor(this, R.color.brown_primary))
				tab.setTypeface(null, android.graphics.Typeface.BOLD)
			} else {
				tab.setTextColor(ContextCompat.getColor(this, R.color.bottom_nav_unselected))
				tab.setTypeface(null, android.graphics.Typeface.NORMAL)
			}
		}
	}

	private fun renderVersion(index: Int) {
		val version: TextVersion = content.versions[index]
		linesContainer.removeAllViews()
		version.lines.forEach { line ->
			val lineView = TextView(this).apply {
				text = line
				textSize = 16f
				setTextColor(
					ContextCompat.getColor(
						this@AppendixTextActivity,
						R.color.text_primary
					)
				)
				setPadding(0, 0, 0, dp(8))
				setLineSpacing(dp(4).toFloat(), 1f)
			}
			linesContainer.addView(lineView)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}