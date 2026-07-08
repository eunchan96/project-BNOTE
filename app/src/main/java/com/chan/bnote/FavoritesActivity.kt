package com.chan.bnote

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.ui.SimpleListAdapter
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_favorites)
		title = "즐겨찾기"

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_favorites)
		recyclerView.layoutManager = LinearLayoutManager(this)

		val db = BibleDatabase.getInstance(applicationContext)
		lifecycleScope.launch {
			val favorites = db.bookmarkDao().getFavoriteVerses()
			val labels = favorites.map { row ->
				"${BibleBooks.nameOf(row.bookId)} ${row.chapter}:${row.verse}  ${row.text}"
			}

			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				val row = favorites[position]
				val intent =
					android.content.Intent(this@FavoritesActivity, VerseListActivity::class.java)
				intent.putExtra("bookId", row.bookId)
				intent.putExtra("bookName", BibleBooks.nameOf(row.bookId))
				intent.putExtra("chapter", row.chapter)
				startActivity(intent)
			}
		}
	}
}