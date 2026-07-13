package com.chan.bnote.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortOptionBottomSheet(
	private val title: String,
	private val options: List<String>
) : BottomSheetDialogFragment() {

	var onSelected: ((position: Int) -> Unit)? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_sort_option, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_sort_title).text = title

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_sort_options)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())
		recyclerView.adapter = SimpleListAdapter(options) { position ->
			onSelected?.invoke(position)
			dismiss()
		}
	}
}