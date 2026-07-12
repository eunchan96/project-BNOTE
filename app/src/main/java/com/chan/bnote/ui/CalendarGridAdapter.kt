package com.chan.bnote.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

data class CalendarDayCell(
	val dateMillis: Long,
	val dayOfMonth: Int,
	val isCurrentMonth: Boolean,
	val isToday: Boolean,
	val colors: List<String>
)

class CalendarGridAdapter(
	private val days: List<CalendarDayCell>,
	private val selectedDate: Long, // 추가
	private val onDayClick: (CalendarDayCell) -> Unit
) : RecyclerView.Adapter<CalendarGridAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val dayNumber: TextView = view.findViewById(R.id.text_day_number)
		val barsContainer: LinearLayout = view.findViewById(R.id.container_bars)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val cell = days[position]
		holder.dayNumber.text = cell.dayOfMonth.toString()

		val isSelected = cell.dateMillis == selectedDate

		holder.dayNumber.setTextColor(
			when {
				isSelected -> Color.WHITE
				!cell.isCurrentMonth -> Color.parseColor("#D0D0D0")
				cell.isToday -> Color.parseColor("#795548")
				else -> Color.parseColor("#333333")
			}
		)
		holder.dayNumber.setTypeface(
			null,
			if (cell.isToday || isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
		)

		// 선택된 날짜는 동그란 갈색 배경으로 강조
		if (isSelected) {
			val drawable = android.graphics.drawable.GradientDrawable()
			drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
			drawable.setColor(Color.parseColor("#795548"))
			holder.dayNumber.background = drawable
			val pad = (4 * holder.itemView.resources.displayMetrics.density).toInt()
			holder.dayNumber.setPadding(pad, pad, pad, pad)
		} else {
			holder.dayNumber.background = null
			holder.dayNumber.setPadding(0, 0, 0, 0)
		}

		holder.barsContainer.removeAllViews()
		val density = holder.itemView.resources.displayMetrics.density
		for (colorHex in cell.colors.take(3)) {
			val bar = View(holder.itemView.context)
			bar.layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, (3 * density).toInt()
			).apply { topMargin = (2 * density).toInt() }
			bar.setBackgroundColor(
				try {
					Color.parseColor(colorHex)
				} catch (e: Exception) {
					Color.GRAY
				}
			)
			holder.barsContainer.addView(bar)
		}

		holder.itemView.setOnClickListener { onDayClick(cell) }
	}

	override fun getItemCount() = days.size
}