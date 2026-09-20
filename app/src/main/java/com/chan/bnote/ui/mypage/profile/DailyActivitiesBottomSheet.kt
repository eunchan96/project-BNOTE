package com.chan.bnote.ui.mypage.profile

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chan.bnote.R
import com.chan.bnote.ui.FixedBottomSheetDialogFragment

/** 내 정보 화면 캘린더에서 날짜를 눌렀을 때, 그날 쓴 설교노트·적용·감사 노트·기도제목을
 * 목록으로 보여준다. 각 항목은 캘린더 색깔 점과 같은 색으로 구분되고, 누르면 해당 화면으로
 * 이동한 뒤 이 시트는 닫힌다. */
class DailyActivitiesBottomSheet : FixedBottomSheetDialogFragment() {

	/** onClick은 이 시트를 만든 쪽(ProfileActivity)의 context를 클로저로 이미 들고 있으므로,
	 * 여기서는 그냥 호출만 해주면 된다. */
	class Entry(
		val typeLabel: String,
		val title: String,
		val colorHex: String,
		val onClick: () -> Unit
	)

	var dateLabel: String = ""
	var entries: List<Entry> = emptyList()

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_daily_activities, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_sheet_title).text = dateLabel
		val rowsContainer = view.findViewById<LinearLayout>(R.id.container_activity_rows)

		entries.forEachIndexed { index, entry ->
			val row = LayoutInflater.from(requireContext())
				.inflate(R.layout.item_daily_activity_row, rowsContainer, false)

			row.findViewById<View>(R.id.dot_activity_color).background.setColorFilter(
				Color.parseColor(entry.colorHex), PorterDuff.Mode.SRC_IN
			)
			row.findViewById<TextView>(R.id.text_activity_type).text = entry.typeLabel
			row.findViewById<TextView>(R.id.text_activity_title).text = entry.title
			row.setOnClickListener {
				entry.onClick()
				dismiss()
			}
			rowsContainer.addView(row)

			if (index < entries.size - 1) {
				val divider = View(requireContext()).apply {
					layoutParams = LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
					).apply { marginStart = dp(20); marginEnd = dp(20) }
					setBackgroundColor(
						ContextCompat.getColor(requireContext(), R.color.divider_light)
					)
				}
				rowsContainer.addView(divider)
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}