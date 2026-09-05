package com.chan.bnote.ui.mypage.gratitude

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
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

	/** 편집 화면과 같은 체크 아이콘을 각 줄 앞에 붙여서 미리보기를 만든다("✓ 내용" 대신).
	 * 일반 ImageSpan은 baseline/bottom 기준이라 텍스트 가운데에서 살짝 어긋나 보여서,
	 * 줄 가운데에 오도록 직접 그리는 [VerticalCenterImageSpan]을 쓴다.
	 *
	 * 항목 내용이 길어서 줄바꿈되면, 그 다음 줄이 아이콘 자리까지 침범해서 어색해 보이던 문제가
	 * 있었다 — [android.text.style.LeadingMarginSpan.Standard]로 "첫 줄은 그대로, 줄바꿈된 다음
	 * 줄부터는 아이콘 너비만큼 들여쓰기"를 적용해서, 아이콘/텍스트가 마치 두 개의 열처럼 보이게
	 * 한다. */
	private fun buildPreview(context: Context, texts: List<String>): CharSequence {
		val builder = SpannableStringBuilder()
		val iconColor = ContextCompat.getColor(context, R.color.brown_primary)
		val density = context.resources.displayMetrics.density
		val textSizePx = 14 * context.resources.displayMetrics.scaledDensity
		val iconSizePx = (14 * density).toInt()
		// 아이콘 다음의 공백 한 칸이 실제로 화면에서 차지하는 폭을 재서 쓴다(임의로 정한 dp값으로는
		// 텍스트 크기가 바뀌거나 폰트가 다르면 아이콘 폭과 안 맞을 수 있다).
		val spaceWidthPx = Paint().apply { textSize = textSizePx }.measureText(" ")
		val hangingIndentPx = iconSizePx + spaceWidthPx.toInt()

		texts.forEachIndexed { index, rawText ->
			if (index > 0) builder.append("\n")
			// LeadingMarginSpan.Standard의 "첫 줄" 판정은 스팬의 시작/끝이 아니라 실제 개행 문자로
			// 나뉘는 문단 단위로 이뤄진다. 항목 텍스트 안에(필터를 만들기 전에 저장된 옛날 데이터
			// 등으로) 개행 문자가 남아있으면 그 지점에서 또 "새 문단의 첫 줄"로 취급돼서 들여쓰기가
			// 풀려버리므로, 여기서 확실히 공백으로 바꿔 없앤다.
			val text = rawText.replace("\n", " ")

			val entryStart = builder.length
			// 아이콘이 들어갈 자리를 문자 하나로 확보해두고, 그 위치에 스팬을 씌운다.
			builder.append("\uFFFC")
			val drawable = ContextCompat.getDrawable(context, R.drawable.ic_check)?.mutate()
			if (drawable != null) {
				drawable.setBounds(0, 0, iconSizePx, iconSizePx)
				drawable.setTintList(ColorStateList.valueOf(iconColor))
				builder.setSpan(
					VerticalCenterImageSpan(drawable),
					entryStart,
					entryStart + 1,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
			builder.append(" ").append(text)
			val entryEnd = builder.length

			// 첫 줄(아이콘이 있는 줄)은 들여쓰기 없이 그대로, 이 항목이 길어서 줄바꿈된 다음
			// 줄부터는 아이콘 너비만큼 들여써서 텍스트 시작 위치와 맞춘다.
			builder.setSpan(
				LeadingMarginSpan.Standard(0, hangingIndentPx),
				entryStart,
				entryEnd,
				Spannable.SPAN_INCLUSIVE_EXCLUSIVE
			)
		}
		return builder
	}

	/** 아이콘을 baseline이 아니라 그 줄의 실제 세로 가운데(폰트 위/아래 여백 기준)에 맞춰 그린다.
	 * 기본 ImageSpan(ALIGN_BASELINE/ALIGN_BOTTOM)은 글자보다 살짝 아래로 치우쳐 보이는 경우가 많다. */
	private class VerticalCenterImageSpan(drawable: Drawable) : ImageSpan(drawable) {
		override fun draw(
			canvas: Canvas,
			text: CharSequence,
			start: Int,
			end: Int,
			x: Float,
			top: Int,
			y: Int,
			bottom: Int,
			paint: Paint
		) {
			val b = drawable
			canvas.save()
			val fontMetrics = paint.fontMetricsInt
			val fontHeight = fontMetrics.descent - fontMetrics.ascent
			val centerY = y + fontMetrics.descent - fontHeight / 2
			val transY = centerY - b.bounds.height() / 2
			canvas.translate(x, transY.toFloat())
			b.draw(canvas)
			canvas.restore()
		}
	}
}