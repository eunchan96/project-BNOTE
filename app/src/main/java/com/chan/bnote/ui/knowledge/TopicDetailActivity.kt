package com.chan.bnote.ui.knowledge

import android.content.Context
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
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.knowledge.TopicalVerseRepository
import com.chan.bnote.data.knowledge.VerseRef
import kotlinx.coroutines.launch

class TopicDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_TOPIC_ID = "extra_topic_id"

		fun createIntent(context: Context, topicId: String): Intent =
			Intent(context, TopicDetailActivity::class.java).putExtra(EXTRA_TOPIC_ID, topicId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_topic_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.topic_detail_root)) { v, insets ->
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
			val topic = TopicalVerseRepository.getById(applicationContext, topicId)
			if (topic == null) {
				finish()
				return@launch
			}
			findViewById<TextView>(R.id.text_top_bar_title).text = topic.title

			val container = findViewById<LinearLayout>(R.id.container_verses)
			container.removeAllViews()

			for (ref in topic.verses) {
				val verseText = fetchVerseText(applicationContext, ref)
				addVerseCard(container, ref, verseText)
			}
		}
	}

	private suspend fun fetchVerseText(context: Context, ref: VerseRef): String {
		val db = BibleDatabase.getInstance(context)
		val verses = db.bibleDao().getVerses("NKRV", ref.bookId, ref.chapter)
		return verses.filter { it.verse in ref.verseStart..ref.verseEnd }
			.joinToString(" ") { it.text }
	}

	private fun addVerseCard(container: LinearLayout, ref: VerseRef, verseText: String) {
		val unit = BibleBooks.chapterUnit(ref.bookId)
		val label = if (ref.verseStart == ref.verseEnd) {
			"${BibleBooks.nameOf(ref.bookId)} ${ref.chapter}${unit} ${ref.verseStart}절"
		} else {
			"${BibleBooks.nameOf(ref.bookId)} ${ref.chapter}${unit} ${ref.verseStart}~${ref.verseEnd}절"
		}

		val card = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(14), dp(16), dp(14))
			background =
				ContextCompat.getDrawable(this@TopicDetailActivity, R.drawable.bg_stat_card)
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { bottomMargin = dp(10) }
			isClickable = true
			isFocusable = true
			setOnClickListener { navigateToBible(ref.bookId, ref.chapter) }
		}
		val labelView = TextView(this).apply {
			text = label
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@TopicDetailActivity, R.color.brown_primary))
		}
		val textView = TextView(this).apply {
			text = verseText
			textSize = 15f
			setTextColor(ContextCompat.getColor(this@TopicDetailActivity, R.color.text_primary))
			setPadding(0, dp(6), 0, 0)
		}
		card.addView(labelView)
		card.addView(textView)
		container.addView(card)
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