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
import com.chan.bnote.data.BibleDatabase
import kotlinx.coroutines.launch

class SermonByPreacherFragment : Fragment() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_grouped_list, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val preachers = db.sermonDao().getAllPreachers()
			val emptyText = view.findViewById<TextView>(R.id.text_empty_grouped)
			val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_grouped)

			if (preachers.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				return@launch
			}
			emptyText.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE
			recyclerView.layoutManager = LinearLayoutManager(requireContext())

			recyclerView.adapter = SimpleListAdapter(preachers) { position ->
				showSermonsForPreacher(preachers[position])
			}
		}
	}

	private fun showSermonsForPreacher(preacher: String) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByPreacher(preacher)
			val labels = sermons.map { it.title }

			val recyclerView =
				view?.findViewById<RecyclerView>(R.id.recycler_grouped) ?: return@launch
			recyclerView.adapter = SimpleListAdapter(labels) { position ->
				val detail = SermonDetailBottomSheet(sermons[position])
				detail.onChanged = { showSermonsForPreacher(preacher) }
				detail.show(parentFragmentManager, "sermon_detail")
			}
		}
	}
}