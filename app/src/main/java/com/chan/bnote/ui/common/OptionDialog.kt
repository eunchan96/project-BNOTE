package com.chan.bnote.ui.common

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 문의하기 창(dialog_contact)과 같은 모양의 선택 창. 항목마다 밝은 박스(bg_book_button)에 제목과
 * (있으면) 한 줄 설명을 넣어, 창 배경(베이지) 위에 글자가 바로 놓이던 기본 목록 창(setItems)보다 깔끔하게 보인다.
 * 사진 추가(설교 · 프로필), 링크 목록, 자동 내보내기 주기 등에서 쓴다.
 */
object OptionDialog {

	/** [selected]면 오른쪽에 › 대신 체크가 나오고 제목이 갈색 굵은 글씨가 된다(지금 고른 값 표시용). */
	data class Option(
		val title: String,
		val subtitle: String? = null,
		val selected: Boolean = false
	)

	fun show(
		activity: Activity,
		title: String?,
		options: List<Option>,
		negativeText: String? = "취소",
		onPick: (index: Int) -> Unit
	): AlertDialog {
		val density = activity.resources.displayMetrics.density
		val list = LinearLayout(activity).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(
				(20 * density).toInt(),
				(8 * density).toInt(),
				(20 * density).toInt(),
				(4 * density).toInt()
			)
		}
		// 링크처럼 항목이 많아질 수 있어서 스크롤되게 감싼다.
		val scroll = ScrollView(activity).apply { addView(list) }

		val builder =
			MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_BNOTE_Dialog).setView(scroll)
		if (title != null) builder.setTitle(title)
		if (negativeText != null) builder.setNegativeButton(negativeText, null)
		val dialog = builder.create()

		options.forEachIndexed { index, option ->
			val row = activity.layoutInflater.inflate(R.layout.item_dialog_option, list, false)
			row.findViewById<TextView>(R.id.text_option_title).apply {
				text = option.title
				if (option.selected) {
					setTypeface(null, Typeface.BOLD)
					setTextColor(ContextCompat.getColor(activity, R.color.brown_text))
				}
			}
			row.findViewById<TextView>(R.id.text_option_subtitle).apply {
				text = option.subtitle
				visibility = if (option.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
			}
			row.findViewById<ImageView>(R.id.icon_option_end).apply {
				if (option.selected) {
					setImageResource(R.drawable.ic_check)
					setColorFilter(ContextCompat.getColor(activity, R.color.brown_text))
				}
			}
			row.setOnClickListener {
				dialog.dismiss()
				onPick(index)
			}
			list.addView(row)
		}
		dialog.show()
		return dialog
	}
}