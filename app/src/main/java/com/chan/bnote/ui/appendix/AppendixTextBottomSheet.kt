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
import com.chan.bnote.data.appendix.TextVersion
import com.chan.bnote.data.appendix.VersionedTextContent
import com.chan.bnote.ui.DraggableBottomSheet

enum class AppendixTextType {
	LORDS_PRAYER,
	APOSTLES_CREED
}

/**
 * 주기도문 / 사도신경처럼 여러 번역본(version)을 탭으로 전환하며 보여주는 공용 바텀시트.
 */
class AppendixTextBottomSheet(
	private val type: AppendixTextType
) : DraggableBottomSheet() {

	private lateinit var content: VersionedTextContent
	private var selectedIndex = 0

	private lateinit var tabContainer: LinearLayout
	private lateinit var linesContainer: LinearLayout

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_appendix_text, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		content = when (type) {
			AppendixTextType.LORDS_PRAYER -> AppendixLoader.loadLordsPrayer(requireContext())
			AppendixTextType.APOSTLES_CREED -> AppendixLoader.loadApostlesCreed(requireContext())
		}

		view.findViewById<TextView>(R.id.text_appendix_title).text = content.title
		tabContainer = view.findViewById(R.id.container_version_tabs)
		linesContainer = view.findViewById(R.id.container_appendix_lines)

		buildTabs()
		renderVersion(selectedIndex)
	}

	private fun buildTabs() {
		tabContainer.removeAllViews()
		content.versions.forEachIndexed { index, version ->
			val tab = TextView(requireContext()).apply {
				text = version.label
				textSize = 14f
				gravity = android.view.Gravity.CENTER
				setPadding(dp(14), dp(12), dp(14), dp(12))
				layoutParams = LinearLayout.LayoutParams(
					0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
				)
				background = ContextCompat.getDrawable(
					requireContext(), android.R.drawable.list_selector_background
				)
				isClickable = true
				isFocusable = true
				setOnClickListener {
					selectedIndex = index
					renderVersion(index)
					updateTabStyles()
				}
			}
			tabContainer.addView(tab)
		}
		updateTabStyles()
	}

	private fun updateTabStyles() {
		for (i in 0 until tabContainer.childCount) {
			val tab = tabContainer.getChildAt(i) as TextView
			if (i == selectedIndex) {
				tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary))
				tab.setTypeface(null, android.graphics.Typeface.BOLD)
			} else {
				tab.setTextColor(
					ContextCompat.getColor(
						requireContext(),
						R.color.bottom_nav_unselected
					)
				)
				tab.setTypeface(null, android.graphics.Typeface.NORMAL)
			}
		}
	}

	private fun renderVersion(index: Int) {
		val version: TextVersion = content.versions[index]
		linesContainer.removeAllViews()
		version.lines.forEach { line ->
			val lineView = TextView(requireContext()).apply {
				text = line
				textSize = 16f
				setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
				setPadding(0, 0, 0, dp(8))
				setLineSpacing(dp(4).toFloat(), 1f)
			}
			linesContainer.addView(lineView)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}