package com.chan.bnote.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SermonSearchBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio = 0.7f

	private lateinit var editSearch: EditText
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView
	private var searchJob: Job? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_sermon_search, container, false)
	}

	override fun onStart() {
		super.onStart()
		dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		editSearch = view.findViewById(R.id.edit_sermon_search)
		recyclerView = view.findViewById(R.id.recycler_sermon_search_results)
		emptyText = view.findViewById(R.id.text_sermon_search_empty)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		editSearch.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				onQueryChanged(s?.toString().orEmpty())
			}
		})

		editSearch.requestFocus()
		editSearch.postDelayed({
			val imm =
				requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
						as android.view.inputmethod.InputMethodManager
			imm.showSoftInput(editSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
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
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
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
				val detail = SermonDetailBottomSheet(sermon)
				detail.show(parentFragmentManager, "sermon_detail")
				dismiss()
			}
		}
	}
}