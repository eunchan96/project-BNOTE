package com.chan.bnote.ui

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class DraggableBottomSheet : BottomSheetDialogFragment() {

	// 하위 클래스가 필요하면 "원래 위치" 높이를 다르게 지정할 수 있게 open
	open val peekHeightRatio: Float = 0.55f

	override fun onStart() {
		super.onStart()
		val bottomSheet = dialog?.findViewById<View>(
			com.google.android.material.R.id.design_bottom_sheet
		) ?: return

		val behavior = BottomSheetBehavior.from(bottomSheet)
		behavior.peekHeight = (resources.displayMetrics.heightPixels * peekHeightRatio).toInt()
		behavior.isDraggable = true
		behavior.state = BottomSheetBehavior.STATE_COLLAPSED
		behavior.skipCollapsed = false
	}
}