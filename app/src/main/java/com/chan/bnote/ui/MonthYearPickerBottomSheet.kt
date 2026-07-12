package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MonthYearPickerBottomSheet(
	initialYear: Int,
	private val initialMonth0: Int // 0-indexed
) : BottomSheetDialogFragment() {

	var onSelected: ((year: Int, month0: Int) -> Unit)? = null

	private var year = initialYear
	private lateinit var yearText: TextView
	private lateinit var recyclerView: RecyclerView

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_month_year, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		yearText = view.findViewById(R.id.text_year)
		recyclerView = view.findViewById(R.id.recycler_months)
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

		yearText.text = "${year}년"
		renderMonths()

		view.findViewById<TextView>(R.id.btn_year_prev).setOnClickListener {
			year -= 1
			yearText.text = "${year}년"
			renderMonths()
		}
		view.findViewById<TextView>(R.id.btn_year_next).setOnClickListener {
			year += 1
			yearText.text = "${year}년"
			renderMonths()
		}
	}

	private fun renderMonths() {
		val months = (1..12).toList()
		recyclerView.adapter = GridNumberAdapter(months) { position ->
			onSelected?.invoke(year, position) // position = 0~11 = month0
			dismiss()
		}
	}
}