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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchBottomSheet(
	private val translation: String
) : BottomSheetDialogFragment() {

	// bookId, chapter, verse 순서로 전달
	var onResultSelected: ((bookId: Int, chapter: Int, verse: Int) -> Unit)? = null

	private lateinit var editSearch: EditText
	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView
	private var searchJob: Job? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_search, container, false)
	}

	override fun onStart() {
		super.onStart()
		// 키보드가 검색창을 가리지 않도록
		dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		editSearch = view.findViewById(R.id.edit_search)
		recyclerView = view.findViewById(R.id.recycler_search_results)
		emptyText = view.findViewById(R.id.text_search_empty)
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
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val results = db.bibleDao().searchVerses(translation, keyword.trim())

			if (results.isEmpty()) {
				emptyText.text = "검색 결과가 없어요"
				emptyText.visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
				return@launch
			}

			emptyText.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE

			val fontSize = com.chan.bnote.data.AppSettings.getFontSize(requireContext())
			recyclerView.adapter = SearchResultAdapter(results, fontSize) { verse ->
				onResultSelected?.invoke(verse.bookId, verse.chapter, verse.verse)
				dismiss()
			}
		}
	}
}