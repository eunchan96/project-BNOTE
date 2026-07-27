package com.chan.bnote.ui.sermon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.chan.bnote.R

/**
 * "+" 버튼을 눌렀을 때, 그 버튼 바로 위로 버튼 너비만큼의 작은 둥근 팝업을 띄워서
 * 설교/적용 중 뭘 추가할지 고르게 한다.
 */
object AddTypePickerPopup {

	fun show(anchor: View, onSermonSelected: () -> Unit, onApplicationSelected: () -> Unit) {
		val context = anchor.context
		val contentView = LayoutInflater.from(context)
			.inflate(R.layout.popup_add_type_picker, anchor.parent as? ViewGroup, false)

		val popupWindow = PopupWindow(
			contentView,
			anchor.width,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			true
		).apply {
			isOutsideTouchable = true
			isFocusable = true
			elevation = 8f
		}

		contentView.findViewById<TextView>(R.id.btn_add_type_sermon).setOnClickListener {
			popupWindow.dismiss()
			onSermonSelected()
		}
		contentView.findViewById<TextView>(R.id.btn_add_type_application).setOnClickListener {
			popupWindow.dismiss()
			onApplicationSelected()
		}

		// 팝업 높이를 미리 재서, 앵커(+ 버튼) 바로 위에 딱 붙게 위치를 계산한다.
		contentView.measure(
			View.MeasureSpec.makeMeasureSpec(anchor.width, View.MeasureSpec.EXACTLY),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		)
		val popupHeight = contentView.measuredHeight

		popupWindow.showAsDropDown(
			anchor,
			0,
			-(anchor.height + popupHeight),
			android.view.Gravity.NO_GRAVITY
		)
	}
}