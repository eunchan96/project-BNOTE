package com.chan.bnote.ui.appendix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.appendix.AppendixLoader
import com.chan.bnote.ui.DraggableBottomSheet
import com.chan.bnote.ui.common.SimpleListAdapter

class ResponsiveReadingListBottomSheet : DraggableBottomSheet() {

	override val peekHeightRatio: Float = 0.8f

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_appendix_reading_list, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val readings = AppendixLoader.loadResponsiveReadings(requireContext())
		val labels = readings.map { "${it.number}. ${it.title}" }

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_responsive_readings)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())
		recyclerView.adapter = SimpleListAdapter(labels) { position ->
			val reading = readings[position]
			ResponsiveReadingDetailBottomSheet(reading.number)
				.show(parentFragmentManager, "responsive_reading_detail")
		}
	}
}