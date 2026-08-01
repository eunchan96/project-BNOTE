package com.chan.bnote.ui.bible.picker

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleBookGroups
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.ui.FixedBottomSheetDialogFragment

class BookOnlyPickerBottomSheet : FixedBottomSheetDialogFragment() {

	var onBookSelected: ((Int) -> Unit)? = null

	// 지금 이 화면에서 이미 고른 책이 있으면 미리 선택된 상태로 보여준다.
	var selectedBookId: Int = -1

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_book_only, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val gridContainer = view.findViewById<LinearLayout>(R.id.container_book_grid)

		for (group in BibleBookGroups.groups) {
			val row = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(4) }
			}
			for (bookId in group) {
				val isSelected = bookId == selectedBookId
				val button = TextView(requireContext()).apply {
					text = BibleBooks.nameOf(bookId)
					gravity = Gravity.CENTER
					textSize = 13f
					maxLines = 2
					setPadding(dp(4), 0, dp(4), 0)
					background = ContextCompat.getDrawable(
						requireContext(),
						if (isSelected) R.drawable.bg_book_button_selected else R.drawable.bg_book_button
					)
					setTextColor(
						ContextCompat.getColor(
							requireContext(),
							if (isSelected) R.color.white else R.color.text_primary
						)
					)
					isClickable = true
					isFocusable = true
					// 이름이 길어서(예: "데살로니가전서") 두 줄로 줄바꿈되는 책이 있으면, 높이가
					// wrap_content일 때 그 칸만 같은 줄의 다른 칸보다 커져서 줄이 들쭉날쭉해 보였다.
					// 높이를 2줄 기준으로 고정해두면 한 줄짜리 이름은 가운데 정렬(gravity=center)로
					// 보이고, 두 줄짜리 이름도 그 안에 딱 맞아서 줄 전체 높이가 항상 똑같아진다.
					layoutParams =
						LinearLayout.LayoutParams(0, dp(56), 1f)
							.apply { marginStart = dp(4); marginEnd = dp(4) }
					setOnClickListener {
						onBookSelected?.invoke(bookId)
						dismiss()
					}
				}
				row.addView(button)
			}
			// 행에 4개 미만이면(구약 마지막 줄 등) 남는 칸만큼 빈 스페이서를 넣어서 늘어나지 않게 한다.
			repeat(4 - group.size) {
				row.addView(View(requireContext()).apply {
					layoutParams =
						LinearLayout.LayoutParams(0, 0, 1f).apply {
							marginStart = dp(4)
							marginEnd = dp(4)
						}
				})
			}
			gridContainer.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}