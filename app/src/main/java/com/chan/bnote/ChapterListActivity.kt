package com.chan.bnote

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.ui.SimpleListAdapter
import kotlinx.coroutines.launch

class ChapterListActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_chapter_list)

		val bookId = intent.getIntExtra("bookId", -1)
		val bookName = intent.getStringExtra("bookName") ?: ""
		title = bookName // 액션바 타이틀에 책 이름 표시

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_chapters)
		recyclerView.layoutManager = LinearLayoutManager(this)

		val db = BibleDatabase.getInstance(applicationContext)
		lifecycleScope.launch {
			val chapters = db.bibleDao().getChapters(bookId)
			val labels = chapters.map { "${it}장" }

			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				val chapter = chapters[position]
				val intent = Intent(this@ChapterListActivity, VerseListActivity::class.java)
				intent.putExtra("bookId", bookId)
				intent.putExtra("bookName", bookName)
				intent.putExtra("chapter", chapter)
				startActivity(intent)
			}
		}
	}
}