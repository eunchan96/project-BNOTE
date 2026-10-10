package com.chan.bnote.ui.common

import android.app.Activity
import android.graphics.Paint
import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings

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

/** 서식(굵게·밑줄·색)을 지원하는 화면이 키보드 바에 넘겨주는 동작들. */
interface KeyboardFormatHandler {
	/** 이 입력칸에 커서가 있을 때 서식 버튼을 보여줄지. */
	fun supportsFormatting(editText: EditText): Boolean
	fun onBold()
	fun onUnderline()
	fun onColor()
}

/**
 * 키보드가 올라와 있을 때만 키보드 바로 위에 보이는 편집 바(include_keyboard_bar).
 * - 실행 취소 / 다시 실행: 지금 커서가 있는 입력칸에 적용된다.
 * - 굵게 · 밑줄 · 색: [formatHandler]를 넘긴 화면에서, 서식을 지원하는 칸(설교 노트 메모, 적용 노트 묵상하기 · 기도하기 · 적용하기)에
 *   커서가 있을 때만 보인다.
 *   드래그로 선택한 부분에 적용되는 건 예전 메모 박스 안 버튼과 같다. 서식 버튼이 보일 땐 자리가 모자라지 않게
 *   실행 취소/다시 실행은 아이콘만 보여준다.
 * - 접기(˅)를 누르면 바가 접히고 오른쪽에 작은 "편집 도구 ˄" 탭만 남는다. 탭을 누르면 다시 펼쳐진다.
 *   접힘 상태는 기억해 둬서 다른 작성 화면이나 다음에 열 때도 그대로다(AppSettings).
 * 버튼들이 포커스를 가져가지 않아서(터치 모드에서 focusable 아님) 눌러도 키보드가 내려가거나 커서·선택이 풀리지 않는다.
 *
 * 화면의 WindowInsets 리스너에서 [onInsetsChanged]를 불러주면 키보드 표시 여부에 맞춰 보이고 숨는다.
 */
class KeyboardBar(
	private val activity: Activity,
	private val bar: View,
	private val formatHandler: KeyboardFormatHandler? = null
) {

	private val formatGroup: View = bar.findViewById(R.id.group_keyboard_format)
	private val undoLabel: TextView = bar.findViewById(R.id.text_keyboard_undo)
	private val redoLabel: TextView = bar.findViewById(R.id.text_keyboard_redo)
	private val body: View = bar.findViewById(R.id.keyboard_bar_body)
	private val expandTab: View = bar.findViewById(R.id.btn_keyboard_bar_expand)

	// 다른 입력칸으로 커서가 옮겨갈 때마다 서식 버튼을 보일지 다시 정한다.
	private val focusListener =
		ViewTreeObserver.OnGlobalFocusChangeListener { _, _ -> updateFormatGroup() }

	init {
		bar.findViewById<View>(R.id.btn_keyboard_undo).setOnClickListener {
			focusedEditText()?.let { TextUndo.undo(it) }
		}
		bar.findViewById<View>(R.id.btn_keyboard_redo).setOnClickListener {
			focusedEditText()?.let { TextUndo.redo(it) }
		}

		val underline = bar.findViewById<TextView>(R.id.btn_keyboard_underline)
		underline.paintFlags = underline.paintFlags or Paint.UNDERLINE_TEXT_FLAG
		if (formatHandler != null) {
			bar.findViewById<View>(R.id.btn_keyboard_bold)
				.setOnClickListener { formatHandler.onBold() }
			underline.setOnClickListener { formatHandler.onUnderline() }
			bar.findViewById<View>(R.id.btn_keyboard_color)
				.setOnClickListener { formatHandler.onColor() }
			bar.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
		}
		bar.findViewById<View>(R.id.btn_keyboard_bar_collapse)
			.setOnClickListener { setCollapsed(true) }
		expandTab.setOnClickListener { setCollapsed(false) }
		applyCollapsed(AppSettings.isKeyboardBarCollapsed(activity))

		updateFormatGroup()
	}

	private fun setCollapsed(collapsed: Boolean) {
		AppSettings.setKeyboardBarCollapsed(activity, collapsed)
		applyCollapsed(collapsed)
	}

	private fun applyCollapsed(collapsed: Boolean) {
		body.isVisible = !collapsed
		expandTab.isVisible = collapsed
	}

	fun onInsetsChanged(insets: WindowInsetsCompat) {
		val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
		if (bar.isVisible != imeVisible) bar.isVisible = imeVisible
		updateFormatGroup()
	}

	private fun updateFormatGroup() {
		val editText = focusedEditText()
		val showFormat = formatHandler != null && editText != null &&
				formatHandler.supportsFormatting(editText)
		if (formatGroup.isVisible != showFormat) formatGroup.isVisible = showFormat
		undoLabel.isVisible = !showFormat
		redoLabel.isVisible = !showFormat
	}

	private fun focusedEditText(): EditText? = activity.currentFocus as? EditText
}