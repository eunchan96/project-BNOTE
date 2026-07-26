package com.chan.bnote.ui.common

import android.content.Context
import com.chan.bnote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** 메모/설교 작성 화면에서 뒤로가기를 눌렀을 때, 저장 안 된 내용이 있으면 확인을 받는다. */
object UnsavedChangesDialog {

	fun show(context: Context, onSaveAndExit: () -> Unit, onDiscard: () -> Unit) {
		MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("나가시겠어요?")
			.setMessage("지금 나가면 작성한 내용이 사라져요.")
			.setPositiveButton("저장하고 나가기") { _, _ -> onSaveAndExit() }
			.setNegativeButton("그냥 나가기") { _, _ -> onDiscard() }
			.setNeutralButton("취소", null)
			.show()
	}
}