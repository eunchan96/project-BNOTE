package com.chan.bnote.ui.sermon

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.chan.bnote.data.DateUtils
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SermonSearchActivity : AppCompatActivity() {

	private lateinit var editSearch: EditText
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView
	private lateinit var historyContainer: View
	private lateinit var historyItemsContainer: LinearLayout
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
		historyContainer = findViewById(R.id.container_sermon_search_history)
		historyItemsContainer = findViewById(R.id.container_sermon_search_history_items)
		recyclerView.layoutManager = LinearLayoutManager(this)

		findViewById<TextView>(R.id.btn_clear_sermon_search_history).setOnClickListener {
			com.google.android.material.dialog.MaterialAlertDialogBuilder(
				this, R.style.ThemeOverlay_BNOTE_Dialog
			)
				.setTitle("최근 검색어 전체 삭제")
				.setMessage("최근 검색어를 전부 지울까요?")
				.setPositiveButton("삭제") { _, _ ->
					AppSettings.clearSermonSearchHistory(this)
					showSearchHistory()
				}
				.setNegativeButton("취소", null)
				.show()
		}

		editSearch.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				onQueryChanged(s?.toString().orEmpty())
			}
		})
		editSearch.setOnEditorActionListener { _, actionId, _ ->
			if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
				commitSearchHistory(editSearch.text.toString())
				true
			} else {
				false
			}
		}

		showSearchHistory()

		editSearch.requestFocus()
		editSearch.postDelayed({
			val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
			imm.showSoftInput(editSearch, InputMethodManager.SHOW_IMPLICIT)
		}, 150)
	}

	private fun onQueryChanged(keyword: String) {
		searchJob?.cancel()

		if (keyword.isBlank()) {
			showSearchHistory()
			return
		}

		historyContainer.visibility = View.GONE

		searchJob = lifecycleScope.launch {
			delay(300)
			val db = BibleDatabase.getInstance(applicationContext)
			val trimmedKeyword = keyword.trim()
			val results = db.sermonDao().search(trimmedKeyword)

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
				// 실제로 결과를 골라 이동했을 때만 "완료된 검색"으로 보고 기록한다 — 타이핑
				// 중간에 잠깐 멈췄을 때마다 뜨는 라이브 검색 결과 하나하나는 기록하지 않는다.
				commitSearchHistory(trimmedKeyword)
				SermonDetailActivity.start(this@SermonSearchActivity, sermon.id)
				finish()
			}
		}
	}

	/** 키보드의 "검색" 버튼을 눌렀거나, 검색 결과를 골랐을 때만 최근 검색어에 기록한다. */
	private fun commitSearchHistory(keyword: String) {
		val trimmed = keyword.trim()
		if (trimmed.isEmpty()) return
		AppSettings.addSermonSearchHistory(this, trimmed)
	}

	/** 검색 전 상태: 결과/안내 문구는 숨기고, 검색 기록이 있으면 보여준다. */
	private fun showSearchHistory() {
		searchJob?.cancel()
		recyclerView.visibility = View.GONE

		val history = AppSettings.getSermonSearchHistory(this)
		if (history.isEmpty()) {
			emptyText.text = "검색어를 입력해주세요"
			emptyText.visibility = View.VISIBLE
			historyContainer.visibility = View.GONE
			return
		}

		emptyText.visibility = View.GONE
		historyContainer.visibility = View.VISIBLE
		renderSearchHistory(history)
	}

	private fun renderSearchHistory(history: List<String>) {
		historyItemsContainer.removeAllViews()
		for (keyword in history) {
			val row = LayoutInflater.from(this)
				.inflate(R.layout.item_search_history_row, historyItemsContainer, false)
			row.findViewById<TextView>(R.id.text_history_keyword).text = keyword
			row.setOnClickListener {
				editSearch.setText(keyword)
				editSearch.setSelection(keyword.length)
			}
			row.findViewById<ImageView>(R.id.btn_remove_history).setOnClickListener {
				AppSettings.removeSermonSearchHistory(this, keyword)
				showSearchHistory()
			}
			historyItemsContainer.addView(row)
		}
	}
}