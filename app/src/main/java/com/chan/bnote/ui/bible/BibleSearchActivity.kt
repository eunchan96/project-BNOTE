package com.chan.bnote.ui.bible

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BibleSearchActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_TRANSLATION = "extra_translation"

		const val EXTRA_RESULT_BOOK_ID = "extra_result_book_id"
		const val EXTRA_RESULT_CHAPTER = "extra_result_chapter"
		const val EXTRA_RESULT_VERSE = "extra_result_verse"

		fun createIntent(context: Context, translation: String): Intent {
			return Intent(context, BibleSearchActivity::class.java).apply {
				putExtra(EXTRA_TRANSLATION, translation)
			}
		}
	}

	private lateinit var editSearch: EditText
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView
	private var searchJob: Job? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_bible_search)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bible_search_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			v.setPadding(
				systemBars.left,
				systemBars.top,
				systemBars.right,
				maxOf(systemBars.bottom, ime.bottom)
			)
			insets
		}

		val translation = intent.getStringExtra(EXTRA_TRANSLATION) ?: "NKRV"

		findViewById<TextView>(R.id.text_top_bar_title).text = "성경 검색"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		editSearch = findViewById(R.id.edit_search)
		recyclerView = findViewById(R.id.recycler_search_results)
		emptyText = findViewById(R.id.text_search_empty)
		recyclerView.layoutManager = LinearLayoutManager(this)

		editSearch.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				onQueryChanged(translation, s?.toString().orEmpty())
			}
		})

		editSearch.requestFocus()
		editSearch.postDelayed({
			val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
		}, 150)
	}

	private fun onQueryChanged(translation: String, keyword: String) {
		searchJob?.cancel()

		val normalizedLength = keyword.replace(" ", "").length

		if (keyword.isBlank()) {
			emptyText.text = "검색어를 입력해주세요"
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		if (normalizedLength < 2) {
			emptyText.text = "2글자 이상 입력해주세요"
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		searchJob = lifecycleScope.launch {
			delay(300)
			val db = BibleDatabase.getInstance(applicationContext)
			val results = db.bibleDao().searchVerses(translation, keyword.trim())

			if (results.isEmpty()) {
				emptyText.text = "검색 결과가 없어요"
				emptyText.visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
				return@launch
			}

			emptyText.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE

			val fontSize = AppSettings.getFontSize(this@BibleSearchActivity)
			recyclerView.adapter = SearchResultAdapter(results, fontSize) { verse ->
				val result = Intent()
					.putExtra(EXTRA_RESULT_BOOK_ID, verse.bookId)
					.putExtra(EXTRA_RESULT_CHAPTER, verse.chapter)
					.putExtra(EXTRA_RESULT_VERSE, verse.verse)
				setResult(Activity.RESULT_OK, result)
				finish()
			}
		}
	}
}