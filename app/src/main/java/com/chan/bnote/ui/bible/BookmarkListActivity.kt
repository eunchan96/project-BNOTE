package com.chan.bnote.ui.bible

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bookmark.BookmarkedVerseRow
import kotlinx.coroutines.launch

class BookmarkListActivity : AppCompatActivity() {

	companion object {
		const val EXTRA_RESULT_BOOK_ID = "extra_result_book_id"
		const val EXTRA_RESULT_CHAPTER = "extra_result_chapter"

		fun createIntent(context: Context): Intent =
			Intent(context, BookmarkListActivity::class.java)
	}

	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: View
	private lateinit var btnToggleEdit: TextView

	private var bookmarks: List<BookmarkedVerseRow> = emptyList()
	private var isEditMode = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_bookmark_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookmark_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		btnToggleEdit = findViewById(R.id.btn_toggle_edit)
		btnToggleEdit.setOnClickListener {
			isEditMode = !isEditMode
			btnToggleEdit.text = if (isEditMode) "완료" else "수정"
			renderList()
		}

		recyclerView = findViewById(R.id.recycler_bookmarks)
		emptyText = findViewById(R.id.text_empty)
		recyclerView.layoutManager = LinearLayoutManager(this)

		loadBookmarks()
	}

	private fun loadBookmarks() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			bookmarks = db.bookmarkDao().getBookmarkedVerses("NKRV")
			renderList()
		}
	}

	private fun renderList() {
		if (bookmarks.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			btnToggleEdit.visibility = View.GONE
			return
		}
		emptyText.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		btnToggleEdit.visibility = View.VISIBLE

		val fontSize = AppSettings.getFontSize(this)
		recyclerView.adapter = BookmarkAdapter(
			rows = bookmarks,
			fontSize = fontSize,
			isEditMode = isEditMode,
			onClick = { row ->
				val result = Intent()
					.putExtra(EXTRA_RESULT_BOOK_ID, row.bookId)
					.putExtra(EXTRA_RESULT_CHAPTER, row.chapter)
				setResult(Activity.RESULT_OK, result)
				finish()
			},
			onDelete = { row ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.bookmarkDao().removeBookmark(row.bookId, row.chapter, row.verse)
					loadBookmarks()
				}
			}
		)
	}
}

private class BookmarkAdapter(
	private val rows: List<BookmarkedVerseRow>,
	private val fontSize: Int,
	private val isEditMode: Boolean,
	private val onClick: (BookmarkedVerseRow) -> Unit,
	private val onDelete: (BookmarkedVerseRow) -> Unit
) : RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val label: TextView = view.findViewById(R.id.text_bookmark_label)
		val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_bookmark)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_bookmark_row, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = rows.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = rows[position]
		holder.label.text =
			"${BibleBooks.nameOf(row.bookId)} ${row.chapter}:${row.verse}  ${row.text}"
		holder.label.textSize = fontSize.toFloat()

		holder.deleteBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.itemView.setOnClickListener { if (!isEditMode) onClick(row) }
		holder.deleteBtn.setOnClickListener { onDelete(row) }
	}
}