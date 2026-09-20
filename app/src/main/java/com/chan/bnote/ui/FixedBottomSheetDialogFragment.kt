package com.chan.bnote.ui

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 앱 안의 bottom sheet들은 대부분 "손가락으로 끌어 내려서 닫는" 동작을 실제로 쓰지 않는다 —
 * 항목을 고르면 알아서 닫히거나, 상단 바깥(스크림)을 눌러서 닫으니까. 그런데 기본
 * BottomSheetDialogFragment는 드래그가 항상 켜져 있어서, 시트 안에서 위로 스크롤하려는
 * 제스처가 스크롤 대신 시트 자체를 끌어올리거나(또는 내려서 닫아버리는) 것으로 오인식되는
 * 문제가 있었다.
 *
 * 이 클래스를 상속하면: 드래그(끌어서 닫기 포함)를 완전히 끄고, 시트를 펼쳐진 높이로 고정한다.
 * 닫는 방법은 그대로 유지된다 — 항목 선택 후 dismiss() 호출, 바깥 스크림 탭, 시스템 뒤로가기.
 */
abstract class FixedBottomSheetDialogFragment : BottomSheetDialogFragment() {

	override fun onStart() {
		super.onStart()
		val bottomSheet = dialog?.findViewById<View>(
			com.google.android.material.R.id.design_bottom_sheet
		) ?: return

		val behavior = BottomSheetBehavior.from(bottomSheet)
		behavior.isDraggable = false
		behavior.skipCollapsed = true
		behavior.state = BottomSheetBehavior.STATE_EXPANDED
	}
}