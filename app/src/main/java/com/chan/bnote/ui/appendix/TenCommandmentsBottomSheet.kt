package com.chan.bnote.ui.appendix

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.appendix.AppendixLoader
import com.chan.bnote.ui.DraggableBottomSheet

class TenCommandmentsBottomSheet : DraggableBottomSheet() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_ten_commandments, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val content = AppendixLoader.loadTenCommandments(requireContext())
		val container = view.findViewById<LinearLayout>(R.id.container_commandments)

		content.intro.forEach { line ->
			container.addView(textView(line, textSize = 15f, bottomMargin = 4))
		}

		container.addView(spacer(12))

		content.commandments.forEach { item ->
			val line = TextView(requireContext()).apply {
				text = buildOrdinalPrefixSpan(item.text)
				textSize = 16f
				setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
				setLineSpacing(dp(3).toFloat(), 1f)
				setPadding(0, 0, 0, dp(10))
			}
			container.addView(line)
		}

		container.addView(
			textView(
				"(${content.reference})",
				textSize = 13f,
				isHint = true,
				bottomMargin = 16
			)
		)

		container.addView(divider())
		container.addView(spacer(12))

		container.addView(textView(content.summary.text, textSize = 15f, bottomMargin = 4))
		container.addView(textView("(${content.summary.reference})", textSize = 13f, isHint = true))
	}

	private fun textView(
		text: String,
		textSize: Float,
		isHint: Boolean = false,
		bottomMargin: Int = 0
	): TextView {
		return TextView(requireContext()).apply {
			this.text = text
			this.textSize = textSize
			setTextColor(
				ContextCompat.getColor(
					requireContext(),
					if (isHint) R.color.text_hint else R.color.text_primary
				)
			)
			setLineSpacing(dp(3).toFloat(), 1f)
			setPadding(0, 0, 0, dp(bottomMargin))
		}
	}

	private fun divider(): View {
		return View(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
			)
			setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider_light))
		}
	}

	private fun spacer(heightDp: Int): View {
		return View(requireContext()).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
			)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private fun buildOrdinalPrefixSpan(text: String): SpannableString {
		val prefixLength = 4.coerceAtMost(text.length)
		return SpannableString(text).apply {
			setSpan(
				ForegroundColorSpan(
					ContextCompat.getColor(
						requireContext(),
						R.color.brown_primary
					)
				),
				0,
				prefixLength,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
	}
}