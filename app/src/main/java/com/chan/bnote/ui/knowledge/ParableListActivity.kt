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
import com.chan.bnote.data.knowledge.ParableOrMiracle
import com.chan.bnote.data.knowledge.ParableRepository
import kotlinx.coroutines.launch

class ParableListActivity : AppCompatActivity() {

	private val typeOrder = listOf("비유", "이적")

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_parable_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.parable_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "예수님의 비유와 이적"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		lifecycleScope.launch {
			val items = ParableRepository.getAll(applicationContext)
			renderList(items)
		}
	}

	private fun renderList(items: List<ParableOrMiracle>) {
		val container = findViewById<LinearLayout>(R.id.container_list)
		container.removeAllViews()

		val grouped = items.groupBy { it.type }
		val orderedTypes = typeOrder.filter { grouped.containsKey(it) } +
				grouped.keys.filter { it !in typeOrder }

		for (type in orderedTypes) {
			addHeader(container, type)
			grouped[type]?.forEach { item -> addRow(container, item) }
		}
	}

	private fun addHeader(container: LinearLayout, type: String) {
		val header = TextView(this).apply {
			text = type
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@ParableListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addRow(container: LinearLayout, item: ParableOrMiracle) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@ParableListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(ParableDetailActivity.createIntent(this@ParableListActivity, item.id))
			}
		}
		val titleView = TextView(this).apply {
			text = item.title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@ParableListActivity, R.color.text_primary))
		}
		val summaryView = TextView(this).apply {
			text = item.summary
			textSize = 13f
			setTextColor(ContextCompat.getColor(this@ParableListActivity, R.color.text_secondary))
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(titleView)
		row.addView(summaryView)
		container.addView(row)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}