package com.chan.bnote.ui.mypage.notice

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.notice.Notice

/** 알림 목록·상세 화면 위쪽의 작은 표시들: [타입] [고정] [해결됨]. */
object NoticeBadges {

	fun fill(container: LinearLayout, notice: Notice) {
		container.removeAllViews()
		val context = container.context
		container.addView(
			badge(
				context,
				notice.type.displayName,
				Color.parseColor(notice.type.colorHex),
				filled = true
			)
		)
		if (notice.isPinned) {
			container.addView(
				badge(
					context,
					"고정",
					ContextCompat.getColor(context, R.color.brown_text),
					filled = false
				)
			)
		}
		if (notice.isResolved) {
			container.addView(badge(context, "해결됨", Color.parseColor("#2E7D32"), filled = false))
		}
	}

	/** [filled]면 색 배경 + 흰 글자, 아니면 색 테두리 + 색 글자. */
	private fun badge(context: Context, text: String, color: Int, filled: Boolean): TextView {
		val density = context.resources.displayMetrics.density
		return TextView(context).apply {
			this.text = text
			textSize = 11f
			setTextColor(if (filled) Color.WHITE else color)
			setPadding(
				(7 * density).toInt(),
				(2 * density).toInt(),
				(7 * density).toInt(),
				(2 * density).toInt()
			)
			background = GradientDrawable().apply {
				cornerRadius = 4 * density
				if (filled) setColor(color) else setStroke((1 * density).toInt(), color)
			}
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { marginEnd = (6 * density).toInt() }
		}
	}
}