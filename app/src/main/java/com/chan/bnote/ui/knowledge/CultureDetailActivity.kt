package com.chan.bnote.ui.knowledge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.knowledge.CultureRepository
import kotlinx.coroutines.launch

class CultureDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_TOPIC_ID = "extra_topic_id"

		fun createIntent(context: Context, topicId: String): Intent =
			Intent(context, CultureDetailActivity::class.java).putExtra(EXTRA_TOPIC_ID, topicId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_culture_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.culture_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		loadTopic()
	}

	private fun loadTopic() {
		val topicId = intent.getStringExtra(EXTRA_TOPIC_ID)
		if (topicId == null) {
			finish()
			return
		}
		lifecycleScope.launch {
			val topic = CultureRepository.getById(applicationContext, topicId)
			if (topic == null) {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_top_bar_title).text = topic.title
			findViewById<TextView>(R.id.text_category).text = topic.category
			findViewById<TextView>(R.id.text_title).text = topic.title
			findViewById<TextView>(R.id.text_summary).text = topic.summary
			findViewById<TextView>(R.id.text_description).text = topic.description

			findViewById<TextView>(R.id.btn_key_verse).apply {
				text = "${topic.keyVerseLabel} 보러 가기"
				setOnClickListener {
					val navIntent =
						Intent(this@CultureDetailActivity, MainActivity::class.java).apply {
							putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, topic.keyBookId)
							putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, topic.keyChapter)
							flags =
								Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
						}
					startActivity(navIntent)
				}
			}
		}
	}
}