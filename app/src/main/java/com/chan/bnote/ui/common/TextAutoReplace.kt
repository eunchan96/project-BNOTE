package com.chan.bnote.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * 메모 작성 중 "->"를 치면 실시간으로 "→"로 바꿔주는 도우미. 여러 메모 화면(구절/단어 메모,
 * 설교 메모)에서 공통으로 쓸 수 있게 분리해뒀다.
 */
object TextAutoReplace {

	fun attachArrowReplacement(editText: EditText) {
		editText.addTextChangedListener(object : TextWatcher {
			// 우리가 직접 글자를 바꿀 때도 이 리스너가 또 불리는데, 그때 또 처리하면 무한루프에
			// 빠질 수 있어서 그동안은 무시하도록 막아둔다.
			private var isReplacing = false

			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

			override fun afterTextChanged(s: Editable?) {
				if (isReplacing || s == null) return
				val cursor = editText.selectionStart
				if (cursor < 2) return
				if (s[cursor - 2] == '-' && s[cursor - 1] == '>') {
					isReplacing = true
					s.replace(cursor - 2, cursor, "→")
					isReplacing = false
				}
			}
		})
	}
}