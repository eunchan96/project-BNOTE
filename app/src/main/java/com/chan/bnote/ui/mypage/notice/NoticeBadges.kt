package com.chan.bnote.ui.mypage.notice

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.notice.Notice

/**
 * 알림 목록·상세 화면 위쪽의 작은 표시들: [타입] [고정] [해결됨].
 * 타입은 파스텔 배경 + 같은 계열 진한 글자(NoticeType 색), 고정·해결됨은 배경 없이 얇은 테두리만 둬서
 * 한 줄에 여러 개가 있어도 알록달록해 보이지 않게 한다.
 */
object NoticeBadges {

	fun fill(container: LinearLayout, notice: Notice) {
		container.removeAllViews()
		val context = container.context
		container.addView(
			badge(
				context,
				notice.type.displayName,
				textColor = ContextCompat.getColor(context, notice.type.textColorRes),
				backgroundColor = ContextCompat.getColor(context, notice.type.backgroundColorRes)
			)
		)
		if (notice.isPinned) {
			container.addView(
				badge(
					context,
					"고정",
					textColor = ContextCompat.getColor(context, R.color.text_secondary)
				)
			)
		}
		if (notice.isResolved) {
			container.addView(
				badge(
					context,
					"해결됨",
					textColor = ContextCompat.getColor(context, R.color.notice_type_tip_text)
				)
			)
		}
	}

	/** [backgroundColor]가 있으면 그 배경색, 없으면 글자색으로 얇은 테두리. */
	private fun badge(
		context: Context,
		text: String,
		textColor: Int,
		backgroundColor: Int? = null
	): TextView {
		val density = context.resources.displayMetrics.density
		return TextView(context).apply {
			this.text = text
			textSize = 11f
			setTextColor(textColor)
			setPadding(
				(7 * density).toInt(),
				(2 * density).toInt(),
				(7 * density).toInt(),
				(2 * density).toInt()
			)
			background = GradientDrawable().apply {
				cornerRadius = 4 * density
				if (backgroundColor != null) setColor(backgroundColor)
				else setStroke((1 * density).toInt(), textColor)
			}
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { marginEnd = (6 * density).toInt() }
		}
	}
}