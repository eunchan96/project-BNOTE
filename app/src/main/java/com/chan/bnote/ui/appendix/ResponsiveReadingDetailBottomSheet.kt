package com.chan.bnote.ui.appendix

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.appendix.AppendixLoader
import com.chan.bnote.data.appendix.ReadingSpeaker
import com.chan.bnote.ui.DraggableBottomSheet

/**
 * 교독문 한 항목의 상세 화면.
 * speaker가 CONGREGATION 또는 UNISON이면 굵게, LEADER는 일반체로 표시한다.
 */
class ResponsiveReadingDetailBottomSheet(
	private val readingNumber: Int
) : DraggableBottomSheet() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_appendix_reading_detail, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val reading = AppendixLoader.loadResponsiveReadings(requireContext())
			.first { it.number == readingNumber }

		view.findViewById<TextView>(R.id.text_reading_title).text =
			"${reading.number}. ${reading.title}"

		val container = view.findViewById<LinearLayout>(R.id.container_reading_lines)
		reading.lines.forEach { line ->
			val isBold = line.speaker == ReadingSpeaker.CONGREGATION ||
					line.speaker == ReadingSpeaker.UNISON

			val lineView = TextView(requireContext()).apply {
				text = line.text
				textSize = 16f
				setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
				setTypeface(
					null,
					if (isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
				)
				setLineSpacing(dp(4).toFloat(), 1f)
				setPadding(0, 0, 0, dp(10))
			}
			container.addView(lineView)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}