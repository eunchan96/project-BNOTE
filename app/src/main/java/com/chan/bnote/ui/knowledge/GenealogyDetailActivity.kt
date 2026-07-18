package com.chan.bnote.ui.knowledge

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
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
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.knowledge.GenealogyEntry
import com.chan.bnote.data.knowledge.GenealogyRepository
import kotlinx.coroutines.launch

class GenealogyDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CHART_ID = "extra_chart_id"

		fun createIntent(context: Context, chartId: String): Intent =
			Intent(context, GenealogyDetailActivity::class.java).putExtra(EXTRA_CHART_ID, chartId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_genealogy_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.genealogy_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		loadChart()
	}

	private fun loadChart() {
		val chartId = intent.getStringExtra(EXTRA_CHART_ID)
		if (chartId == null) {
			finish()
			return
		}
		lifecycleScope.launch {
			val chart = GenealogyRepository.getById(applicationContext, chartId)
			if (chart == null) {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_top_bar_title).text = chart.title
			findViewById<TextView>(R.id.text_description).text = chart.description

			val chainContainer = findViewById<LinearLayout>(R.id.container_chain)
			chainContainer.removeAllViews()
			chart.entries.forEachIndexed { index, entry ->
				addEntry(chainContainer, entry)
				if (index != chart.entries.lastIndex) addArrow(chainContainer)
			}

			findViewById<TextView>(R.id.btn_key_verse).apply {
				text = "${chart.keyVerseLabel} 보러 가기"
				setOnClickListener {
					val navIntent =
						Intent(this@GenealogyDetailActivity, MainActivity::class.java).apply {
							putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, chart.keyBookId)
							putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chart.keyChapter)
							flags =
								Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
						}
					startActivity(navIntent)
				}
			}
		}
	}

	private fun addEntry(container: LinearLayout, entry: GenealogyEntry) {
		val card = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(14), dp(12), dp(14), dp(12))
			background =
				ContextCompat.getDrawable(this@GenealogyDetailActivity, R.drawable.bg_stat_card)
		}
		val nameView = TextView(this).apply {
			text = entry.name
			textSize = 16f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(
				ContextCompat.getColor(
					this@GenealogyDetailActivity,
					R.color.brown_primary
				)
			)
		}
		val relationView = TextView(this).apply {
			text = entry.relation
			textSize = 13f
			setTextColor(
				ContextCompat.getColor(
					this@GenealogyDetailActivity,
					R.color.text_secondary
				)
			)
			setPadding(0, dp(2), 0, 0)
		}
		card.addView(nameView)
		card.addView(relationView)
		if (entry.note.isNotBlank()) {
			val noteView = TextView(this).apply {
				text = entry.note
				textSize = 13f
				setTextColor(
					ContextCompat.getColor(
						this@GenealogyDetailActivity,
						R.color.text_primary
					)
				)
				setPadding(0, dp(6), 0, 0)
			}
			card.addView(noteView)
		}
		container.addView(card)
	}

	private fun addArrow(container: LinearLayout) {
		val arrow = TextView(this).apply {
			text = "↓"
			textSize = 18f
			gravity = Gravity.CENTER
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
			setTextColor(ContextCompat.getColor(this@GenealogyDetailActivity, R.color.text_hint))
			setPadding(0, dp(4), 0, dp(4))
		}
		container.addView(arrow)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}