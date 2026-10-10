package com.chan.bnote.ui.mypage.notice

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.notice.Notice

/**
 * 알림 목록·상세 화면 위쪽의 작은 표시들: [📌] [타입] [해결됨].
 * 고정은 글자 칩 대신 맨 앞의 작은 핀 아이콘으로 보여준다. 타입은 파스텔 배경 + 같은 계열 진한 글자
 * (NoticeType 색), 해결됨은 배경 없이 얇은 테두리만 둬서 한 줄에 여러 개가 있어도 알록달록해 보이지 않게 한다.
 */
object NoticeBadges {

	fun fill(container: LinearLayout, notice: Notice) {
		container.removeAllViews()
		val context = container.context
		if (notice.isPinned) container.addView(pinIcon(context))
		container.addView(
			badge(
				context,
				notice.type.displayName,
				textColor = ContextCompat.getColor(context, notice.type.textColorRes),
				backgroundColor = ContextCompat.getColor(context, notice.type.backgroundColorRes)
			)
		)
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

	/** 고정된 알림 표시. 칩 글자 높이(11sp)에 맞춘 작은 핀 아이콘. */
	private fun pinIcon(context: Context): ImageView {
		val density = context.resources.displayMetrics.density
		val size = (15 * density).toInt()
		return ImageView(context).apply {
			setImageResource(R.drawable.ic_push_pin)
			imageTintList =
				ColorStateList.valueOf(ContextCompat.getColor(context, R.color.text_secondary))
			contentDescription = "고정된 알림"
			layoutParams = LinearLayout.LayoutParams(size, size).apply {
				marginEnd = (5 * density).toInt()
			}
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