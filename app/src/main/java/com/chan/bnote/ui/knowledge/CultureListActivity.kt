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
import com.chan.bnote.data.knowledge.CultureRepository
import com.chan.bnote.data.knowledge.CultureTopic
import kotlinx.coroutines.launch

class CultureListActivity : AppCompatActivity() {

	// 목록에서 이 순서대로 카테고리 섹션을 보여준다.
	private val categoryOrder = listOf("가정", "신앙생활", "일상생활", "경제생활", "정치/사회")

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_culture_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.culture_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "당시 문화"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		lifecycleScope.launch {
			val topics = CultureRepository.getAll(applicationContext)
			renderList(topics)
		}
	}

	private fun renderList(topics: List<CultureTopic>) {
		val container = findViewById<LinearLayout>(R.id.container_list)
		container.removeAllViews()

		val grouped = topics.groupBy { it.category }
		val orderedCategories = categoryOrder.filter { grouped.containsKey(it) } +
				grouped.keys.filter { it !in categoryOrder }

		for (category in orderedCategories) {
			addHeader(container, category)
			grouped[category]?.forEach { topic -> addRow(container, topic) }
		}
	}

	private fun addHeader(container: LinearLayout, category: String) {
		val header = TextView(this).apply {
			text = category
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@CultureListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addRow(container: LinearLayout, topic: CultureTopic) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@CultureListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(
					CultureDetailActivity.createIntent(
						this@CultureListActivity,
						topic.id
					)
				)
			}
		}
		val titleView = TextView(this).apply {
			text = topic.title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@CultureListActivity, R.color.text_primary))
		}
		val summaryView = TextView(this).apply {
			text = topic.summary
			textSize = 13f
			setTextColor(ContextCompat.getColor(this@CultureListActivity, R.color.text_secondary))
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(titleView)
		row.addView(summaryView)
		container.addView(row)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}