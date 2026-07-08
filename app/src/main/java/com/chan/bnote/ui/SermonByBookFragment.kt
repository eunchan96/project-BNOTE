package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import kotlinx.coroutines.launch

class SermonByBookFragment : Fragment() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_grouped_list, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val bookIds = db.sermonDao().getBooksWithSermons()
			val emptyText = view.findViewById<TextView>(R.id.text_empty_grouped)
			val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_grouped)

			if (bookIds.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				return@launch
			}
			emptyText.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE
			recyclerView.layoutManager = LinearLayoutManager(requireContext())

			val labels = bookIds.map { BibleBooks.nameOf(it) }
			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				showSermonsForBook(bookIds[position], labels[position])
			}
		}
	}

	private fun showSermonsForBook(bookId: Int, bookName: String) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByBook(bookId)
			val labels = sermons.map { "${it.title}  (${it.preacher})" }

			val recyclerView =
				view?.findViewById<RecyclerView>(R.id.recycler_grouped) ?: return@launch
			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				SermonDetailBottomSheet(sermons[position]).show(
					parentFragmentManager,
					"sermon_detail"
				)
			}
		}
	}
}