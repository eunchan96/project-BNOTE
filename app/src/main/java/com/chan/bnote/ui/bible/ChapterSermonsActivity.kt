package com.chan.bnote.ui.bible

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.ui.sermon.SermonDetailActivity
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowData
import kotlinx.coroutines.launch

class ChapterSermonsActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_BOOK_ID = "extra_book_id"
		private const val EXTRA_CHAPTER = "extra_chapter"

		fun start(context: Context, bookId: Int, chapter: Int) {
			val intent = Intent(context, ChapterSermonsActivity::class.java)
			intent.putExtra(EXTRA_BOOK_ID, bookId)
			intent.putExtra(EXTRA_CHAPTER, chapter)
			context.startActivity(intent)
		}
	}

	private var bookId = 0
	private var chapter = 0
	private lateinit var recyclerView: RecyclerView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_chapter_sermons)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chapter_sermons_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		bookId = intent.getIntExtra(EXTRA_BOOK_ID, 1)
		chapter = intent.getIntExtra(EXTRA_CHAPTER, 1)

		findViewById<TextView>(R.id.text_top_bar_title).text =
			"${BibleBooks.nameOf(bookId)} ${chapter}${BibleBooks.chapterUnit(bookId)}의 설교"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		recyclerView = findViewById(R.id.recycler_chapter_sermons)
		recyclerView.layoutManager = LinearLayoutManager(this)
	}

	override fun onResume() {
		super.onResume()
		loadSermons()
	}

	private fun loadSermons() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val sermons = db.sermonDao().getByBookChapter(bookId, chapter)

			val rows = sermons.map { sermon ->
				val firstRef = db.sermonBibleRefDao().getFirstRef(sermon.id)
				val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
				SermonRowData(
					sermon = sermon,
					colorHex = category?.colorHex,
					dateLabel = DateUtils.formatDateShort(sermon.sermonDate),
					bibleRefLabel = firstRef?.toShortLabel() ?: ""
				)
			}

			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				SermonDetailActivity.start(this@ChapterSermonsActivity, sermon.id)
			}
		}
	}
}