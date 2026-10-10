package com.chan.bnote.ui.common

import android.app.Activity
import android.view.View
import android.widget.EditText
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.chan.bnote.R

/**
 * 입력칸의 실행 취소/다시 실행.
 * 안드로이드 입력칸(EditText)은 원래 입력 기록을 갖고 있어서 블루투스 키보드에서는 Ctrl+Z로 되돌릴 수 있는데,
 * 핸드폰 키보드에는 그 버튼이 없다. 여기서는 Ctrl+Z/Ctrl+Shift+Z와 똑같은 기록을 그대로 호출한다
 * (글자 입력·삭제만 되돌려지고, 굵게·밑줄·색 같은 서식 변경은 되돌려지지 않는 것도 Ctrl+Z와 같다).
 */
object TextUndo {
	fun undo(editText: EditText) {
		editText.onTextContextMenuItem(android.R.id.undo)
	}

	fun redo(editText: EditText) {
		editText.onTextContextMenuItem(android.R.id.redo)
	}
}

/**
 * 키보드가 올라와 있을 때만 키보드 바로 위에 보이는 "실행 취소 / 다시 실행" 바(include_keyboard_undo_bar).
 * 버튼은 지금 커서가 있는 입력칸에 적용된다. 버튼이 포커스를 가져가지 않아서(터치 모드에서 focusable 아님)
 * 눌러도 키보드가 내려가거나 커서가 다른 칸으로 옮겨가지 않는다.
 *
 * 화면의 WindowInsets 리스너에서 [onInsetsChanged]를 불러주면 키보드 표시 여부에 맞춰 보이고 숨는다.
 */
class KeyboardUndoBar(private val activity: Activity, private val bar: View) {

	init {
		bar.findViewById<View>(R.id.btn_keyboard_undo).setOnClickListener {
			focusedEditText()?.let { TextUndo.undo(it) }
		}
		bar.findViewById<View>(R.id.btn_keyboard_redo).setOnClickListener {
			focusedEditText()?.let { TextUndo.redo(it) }
		}
	}

	fun onInsetsChanged(insets: WindowInsetsCompat) {
		val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
		if (bar.isVisible != imeVisible) bar.isVisible = imeVisible
	}

	private fun focusedEditText(): EditText? = activity.currentFocus as? EditText
}