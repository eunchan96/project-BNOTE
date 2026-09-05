package com.chan.bnote.ui.mypage.gratitude

import android.content.Context
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.gratitude.GratitudeNote

object GratitudeRowBuilder {
	suspend fun build(
		context: Context,
		db: BibleDatabase,
		notes: List<GratitudeNote>
	): List<GratitudeRowData> {
		return notes.map { note ->
			val entries = db.gratitudeEntryDao().getByNote(note.id)
				.filter { it.text.isNotBlank() }
			GratitudeRowData(
				note = note,
				previewText = buildPreview(context, entries.map { it.text })
			)
		}
	}

	/** 편집 화면과 같은 하트 아이콘을 각 줄 앞에 붙여서 미리보기를 만든다("✓ 내용" 대신). */
	private fun buildPreview(context: Context, texts: List<String>): CharSequence {
		val builder = SpannableStringBuilder()
		val iconColor = ContextCompat.getColor(context, R.color.brown_primary)

		texts.forEachIndexed { index, text ->
			if (index > 0) builder.append("\n")

			val iconStart = builder.length
			// 아이콘이 들어갈 자리를 문자 하나로 확보해두고, 그 위치에 ImageSpan을 씌운다.
			builder.append("\u2764")
			val drawable = ContextCompat.getDrawable(context, R.drawable.ic_check_circle)?.mutate()
			if (drawable != null) {
				val size = (16 * context.resources.displayMetrics.density).toInt()
				drawable.setBounds(0, 0, size, size)
				drawable.setTintList(ColorStateList.valueOf(iconColor))
				builder.setSpan(
					ImageSpan(drawable, ImageSpan.ALIGN_BASELINE),
					iconStart,
					iconStart + 1,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
			builder.append(" ").append(text)
		}
		return builder
	}
}