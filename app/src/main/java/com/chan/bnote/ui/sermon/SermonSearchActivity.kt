package com.chan.bnote.ui.sermon

import android.content.Context
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
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SermonSearchActivity : AppCompatActivity() {

	private lateinit var editSearch: EditText
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView
	private var searchJob: Job? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_sermon_search)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sermon_search_root)) { v, insets ->
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

		findViewById<TextView>(R.id.text_top_bar_title).text = "설교 검색"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		editSearch = findViewById(R.id.edit_sermon_search)
		recyclerView = findViewById(R.id.recycler_sermon_search_results)
		emptyText = findViewById(R.id.text_sermon_search_empty)
		recyclerView.layoutManager = LinearLayoutManager(this)

		editSearch.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				onQueryChanged(s?.toString().orEmpty())
			}
		})

		editSearch.requestFocus()
		editSearch.postDelayed({
			val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
		}, 150)
	}

	private fun onQueryChanged(keyword: String) {
		searchJob?.cancel()

		if (keyword.isBlank()) {
			emptyText.text = "검색어를 입력해주세요"
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		searchJob = lifecycleScope.launch {
			delay(300)
			val db = BibleDatabase.getInstance(applicationContext)
			val results = db.sermonDao().search(keyword.trim())

			if (results.isEmpty()) {
				emptyText.text = "검색 결과가 없어요"
				emptyText.visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
				return@launch
			}

			emptyText.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE

			val rows = results.map { sermon ->
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
				SermonDetailActivity.start(this@SermonSearchActivity, sermon.id)
				finish()
			}
		}
	}
}