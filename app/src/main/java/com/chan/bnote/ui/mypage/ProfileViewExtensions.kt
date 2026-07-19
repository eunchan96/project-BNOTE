package com.chan.bnote.ui.mypage

import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import com.chan.bnote.R

/**
 * 프로필 사진을 로드한다. 사진이 없으면 기본 아이콘을 연한 회색으로 톤다운해서 보여준다.
 * (실제 사진일 때는 tint를 지워야 사진 색이 왜곡되지 않는다.)
 */
fun ImageView.loadProfilePhoto(path: String?) {
	if (!path.isNullOrBlank()) {
		imageTintList = null
		load(path) { placeholder(R.drawable.ic_person) }
	} else {
		imageTintList = ColorStateList.valueOf(
			ContextCompat.getColor(context, R.color.text_hint)
		)
		setImageResource(R.drawable.ic_person_inset)
	}
}

/** "이름 직분" 형태로, 직분 부분만 작고 연하게 표시한다. 직분이 없으면 이름만 표시한다. */
fun TextView.setNameWithPosition(name: String, position: String) {
	if (position.isBlank()) {
		text = name
		return
	}
	val builder = SpannableStringBuilder(name).append(" ").append(position)
	val start = name.length + 1
	val end = builder.length
	builder.setSpan(
		ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_secondary)),
		start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
	)
	builder.setSpan(
		RelativeSizeSpan(0.8f),
		start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
	)
	text = builder
}