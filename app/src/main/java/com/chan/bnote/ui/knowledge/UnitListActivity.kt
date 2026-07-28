package com.chan.bnote.ui.knowledge

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
import com.chan.bnote.R
import com.chan.bnote.data.knowledge.BibleUnit
import com.chan.bnote.data.knowledge.UnitRepository
import kotlinx.coroutines.launch

class UnitListActivity : AppCompatActivity() {

	// 목록에서 이 순서대로 카테고리 섹션을 보여준다.
	private val categoryOrder = listOf("길이", "무게·화폐", "부피", "시간")

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_unit_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.unit_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "단위"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		lifecycleScope.launch {
			val units = UnitRepository.getAll(applicationContext)
			renderList(units)
		}
	}

	private fun renderList(units: List<BibleUnit>) {
		val container = findViewById<LinearLayout>(R.id.container_list)
		container.removeAllViews()

		val grouped = units.groupBy { it.category }
		val orderedCategories = categoryOrder.filter { grouped.containsKey(it) } +
				grouped.keys.filter { it !in categoryOrder }

		for (category in orderedCategories) {
			addHeader(container, category)
			grouped[category]?.forEach { unit -> addRow(container, unit) }
		}
	}

	private fun addHeader(container: LinearLayout, category: String) {
		val header = TextView(this).apply {
			text = category
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@UnitListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addRow(container: LinearLayout, unit: BibleUnit) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@UnitListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(UnitDetailActivity.createIntent(this@UnitListActivity, unit.id))
			}
		}
		val titleView = TextView(this).apply {
			text = unit.title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@UnitListActivity, R.color.text_primary))
		}
		val summaryView = TextView(this).apply {
			text = unit.summary
			textSize = 13f
			setTextColor(ContextCompat.getColor(this@UnitListActivity, R.color.text_secondary))
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(titleView)
		row.addView(summaryView)
		container.addView(row)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}