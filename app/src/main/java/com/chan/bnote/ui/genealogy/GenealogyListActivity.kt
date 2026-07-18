package com.chan.bnote.ui.genealogy

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
import com.chan.bnote.data.genealogy.GenealogyChart
import com.chan.bnote.data.genealogy.GenealogyRepository
import kotlinx.coroutines.launch

class GenealogyListActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_genealogy_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.genealogy_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "족보"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		lifecycleScope.launch {
			val charts = GenealogyRepository.getAll(applicationContext)
			renderList(charts)
		}
	}

	private fun renderList(charts: List<GenealogyChart>) {
		val container = findViewById<LinearLayout>(R.id.container_list)
		container.removeAllViews()
		for (chart in charts) {
			addRow(container, chart)
		}
	}

	private fun addRow(container: LinearLayout, chart: GenealogyChart) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(14), dp(16), dp(14))
			background = ContextCompat.getDrawable(
				this@GenealogyListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(
					GenealogyDetailActivity.createIntent(
						this@GenealogyListActivity,
						chart.id
					)
				)
			}
		}
		val titleView = TextView(this).apply {
			text = chart.title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@GenealogyListActivity, R.color.text_primary))
		}
		val countView = TextView(this).apply {
			text = "${chart.entries.size}명"
			textSize = 13f
			setTextColor(ContextCompat.getColor(this@GenealogyListActivity, R.color.text_secondary))
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(titleView)
		row.addView(countView)
		container.addView(row)

		val divider = android.view.View(this).apply {
			layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
			setBackgroundColor(
				ContextCompat.getColor(
					this@GenealogyListActivity,
					R.color.divider_light
				)
			)
		}
		container.addView(divider)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}