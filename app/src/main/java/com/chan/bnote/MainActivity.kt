package com.chan.bnote

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.BibleSeeder
import com.chan.bnote.ui.SimpleListAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val recyclerView =
			findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_books)
		recyclerView.layoutManager = LinearLayoutManager(this)

		val db = BibleDatabase.getInstance(applicationContext)
		lifecycleScope.launch {
			BibleSeeder.seedIfEmpty(applicationContext, db)

			val bookIds = db.bibleDao().getBookIds()
			val bookNames = bookIds.map { BibleBooks.nameOf(it) }

			recyclerView.adapter = SimpleListAdapter(bookNames) { position ->
				val bookId = bookIds[position]
				val bookName = bookNames[position]
				val intent = Intent(this@MainActivity, ChapterListActivity::class.java)
				intent.putExtra("bookId", bookId)
				intent.putExtra("bookName", bookName)
				startActivity(intent)
			}
		}
	}
}