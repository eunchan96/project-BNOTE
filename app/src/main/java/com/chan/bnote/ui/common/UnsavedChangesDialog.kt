package com.chan.bnote.ui.common

import android.content.Context
import com.chan.bnote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** 메모/설교 작성 화면에서 뒤로가기를 눌렀을 때, 저장 안 된 내용이 있으면 확인을 받는다.
 * 버튼이 3개(저장하고 나가기 · 그냥 나가기 · 취소)일 때 작은 화면에서 가로로 안 들어가고
 * 세로로 쌓이는 문제가 있어서, "저장하고 나가기"는 없애고 두 버튼만 남겼다. */
object UnsavedChangesDialog {

	fun show(context: Context, onDiscard: () -> Unit) {
		MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("나가시겠어요?")
			.setMessage("지금 나가면 작성한 내용이 사라져요.")
			.setPositiveButton("그냥 나가기") { _, _ -> onDiscard() }
			.setNegativeButton("취소", null)
			.show()
	}
}