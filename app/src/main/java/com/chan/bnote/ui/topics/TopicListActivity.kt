package com.chan.bnote.ui.topics

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
import com.chan.bnote.data.topics.TopicalVerseGroup
import com.chan.bnote.data.topics.TopicalVerseRepository
import kotlinx.coroutines.launch

class TopicListActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_topic_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topic_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "상황에 따라 찾는 말씀"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		lifecycleScope.launch {
			val topics = TopicalVerseRepository.getAll(applicationContext)
			renderList(topics)
		}
	}

	private fun renderList(topics: List<TopicalVerseGroup>) {
		val container = findViewById<LinearLayout>(R.id.container_list)
		container.removeAllViews()
		for (topic in topics) {
			val row = TextView(this).apply {
				text = topic.title
				textSize = 15f
				setTextColor(ContextCompat.getColor(this@TopicListActivity, R.color.text_primary))
				setPadding(dp(16), dp(16), dp(16), dp(16))
				background = ContextCompat.getDrawable(
					this@TopicListActivity, android.R.drawable.list_selector_background
				)
				isClickable = true
				isFocusable = true
				setOnClickListener {
					startActivity(
						TopicDetailActivity.createIntent(
							this@TopicListActivity,
							topic.id
						)
					)
				}
			}
			container.addView(row)

			val divider = android.view.View(this).apply {
				layoutParams =
					LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
				setBackgroundColor(
					ContextCompat.getColor(
						this@TopicListActivity,
						R.color.divider_light
					)
				)
			}
			container.addView(divider)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}