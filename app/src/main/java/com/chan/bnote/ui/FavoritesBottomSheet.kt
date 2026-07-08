package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class FavoritesBottomSheet : BottomSheetDialogFragment() {

	var onVerseSelected: ((bookId: Int, chapter: Int) -> Unit)? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_favorites, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_favorites)
		val emptyText = view.findViewById<View>(R.id.text_empty)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val favorites = db.bookmarkDao().getFavoriteVerses()

			if (favorites.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
				return@launch
			}

			val labels = favorites.map { row ->
				"${BibleBooks.nameOf(row.bookId)} ${row.chapter}:${row.verse}  ${row.text}"
			}

			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				val row = favorites[position]
				onVerseSelected?.invoke(row.bookId, row.chapter)
				dismiss()
			}
		}
	}
}