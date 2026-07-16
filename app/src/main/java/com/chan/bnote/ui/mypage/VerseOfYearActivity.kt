package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.VerseOfYear
import kotlinx.coroutines.launch
import java.util.Calendar

class VerseOfYearActivity : AppCompatActivity() {

	private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_verse_of_year)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verse_of_year_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.btn_add_entry).setOnClickListener {
			startActivity(VerseOfYearEditActivity.addIntent(this))
		}

		recyclerView = findViewById(R.id.recycler_verse_of_year)
		recyclerView.layoutManager = LinearLayoutManager(this)
		emptyText = findViewById(R.id.text_empty)
	}

	override fun onResume() {
		super.onResume()
		loadEntries()
	}

	private fun loadEntries() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val entries = db.verseOfYearDao().getAll()

			emptyText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
			recyclerView.adapter = VerseOfYearAdapter(
				entries = entries,
				currentYear = currentYear,
				onEdit = { entry ->
					startActivity(
						VerseOfYearEditActivity.editIntent(this@VerseOfYearActivity, entry.year)
					)
				}
			)
		}
	}
}

private class VerseOfYearAdapter(
	private val entries: List<VerseOfYear>,
	private val currentYear: Int,
	private val onEdit: (VerseOfYear) -> Unit
) : RecyclerView.Adapter<VerseOfYearAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val year: TextView = view.findViewById(R.id.text_row_year)
		val ref: TextView = view.findViewById(R.id.text_row_ref)
		val verse: TextView = view.findViewById(R.id.text_row_verse)
		val note: TextView = view.findViewById(R.id.text_row_note)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_verse_of_year_entry, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = entries.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val entry = entries[position]
		holder.year.text =
			if (entry.year == currentYear) "${entry.year}년 (올해)" else "${entry.year}년"
		holder.ref.text = "${BibleBooks.nameOf(entry.bookId)} ${entry.chapter}:${entry.verse}"
		holder.verse.text = entry.verseText
		if (entry.note.isNotBlank()) {
			holder.note.text = entry.note
			holder.note.visibility = View.VISIBLE
		} else {
			holder.note.visibility = View.GONE
		}
		holder.itemView.setOnClickListener { onEdit(entry) }
	}
}