package com.chan.bnote.ui.common

import android.content.Context
import android.widget.EditText
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.chan.bnote.R

/**
 * 카테고리·설교자·스크랩 그룹·암송 그룹 등 "이름 하나만 입력받는" 다이얼로그들이 전부 각자
 * 따로 EditText를 만들고 있었는데(테두리 없는 리플 배경 bg_book_button을 써서, 메모 편집
 * bottom sheet 등 다른 입력칸들과 느낌이 달랐다), 여기 한 곳에서 만들어서 테마를 통일한다.
 * bottom sheet까지 바꾸진 않고, 다이얼로그 안의 입력칸 모양만 같은 느낌(메모 편집 박스와 같은
 * bg_memo_box_outline 테두리 박스)으로 맞춘다.
 *
 * (참고: 이 테두리가 다이얼로그 배경 위에서 잘 안 보이는 문제가 있었는데, 원인은 테두리 색이
 * 아니라 다이얼로그 배경 자체가 크림색이었던 것이었다 — themes.xml의 ThemeOverlay.BNOTE.Dialog
 * colorSurface를 흰색으로 바꿔서 해결했다. 그래서 이 입력칸은 원래 스타일 그대로 둔다.)
 */
object DialogStyle {

	fun buildEditText(context: Context, hint: String, initialText: String = ""): EditText {
		return EditText(context).apply {
			this.hint = hint
			setText(initialText)
			textSize = 15f
			setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12))
			background = ContextCompat.getDrawable(context, R.drawable.bg_memo_box_outline)
		}
	}

	fun wrapInDialogContainer(context: Context, editText: EditText): FrameLayout {
		return FrameLayout(context).apply {
			setPadding(dp(context, 24), dp(context, 16), dp(context, 24), dp(context, 4))
			addView(editText)
		}
	}

	private fun dp(context: Context, value: Int): Int =
		(value * context.resources.displayMetrics.density).toInt()
}