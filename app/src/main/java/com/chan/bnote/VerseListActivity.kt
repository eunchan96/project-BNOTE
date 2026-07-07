package com.chan.bnote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.ui.VerseAdapter
import kotlinx.coroutines.launch

class VerseListActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_verse_list)

		val bookId = intent.getIntExtra("bookId", -1)
		val bookName = intent.getStringExtra("bookName") ?: ""
		val chapter = intent.getIntExtra("chapter", -1)
		title = "$bookName ${chapter}장"

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_verses)
		recyclerView.layoutManager = LinearLayoutManager(this)

		val db = BibleDatabase.getInstance(applicationContext)
		lifecycleScope.launch {
			val verses = db.bibleDao().getVerses(bookId, chapter)
			recyclerView.adapter = VerseAdapter(verses)
		}
	}
}