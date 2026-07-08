package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.chan.bnote.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BibleMenuBottomSheet(
	private val isReadingPlanEnabled: Boolean,
	private val isAutoScrolling: Boolean
) : BottomSheetDialogFragment() {

	var onAppendixClicked: (() -> Unit)? = null
	var onReadingPlanToggled: ((Boolean) -> Unit)? = null
	var onAutoScrollToggled: ((Boolean) -> Unit)? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_bible_menu, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.menu_hymn).setOnClickListener {
			Toast.makeText(requireContext(), "찬송 (추후 구현)", Toast.LENGTH_SHORT).show()
		}
		view.findViewById<TextView>(R.id.menu_appendix).setOnClickListener {
			onAppendixClicked?.invoke()
			dismiss()
		}

		view.findViewById<Switch>(R.id.switch_reading_plan).apply {
			isChecked = isReadingPlanEnabled
			setOnCheckedChangeListener { _, checked -> onReadingPlanToggled?.invoke(checked) }
		}
		view.findViewById<Switch>(R.id.switch_auto_scroll).apply {
			isChecked = isAutoScrolling
			setOnCheckedChangeListener { _, checked -> onAutoScrollToggled?.invoke(checked) }
		}
	}
}