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

		holder.dayNumber.setTextColor(
			when {
				!cell.isCurrentMonth -> Color.parseColor("#D0D0D0")
				cell.isToday -> Color.parseColor("#795548")
				else -> Color.parseColor("#333333")
			}
		)
		if (cell.isToday) {
			holder.dayNumber.setTypeface(null, android.graphics.Typeface.BOLD)
		}

		holder.barsContainer.removeAllViews()
		val density = holder.itemView.resources.displayMetrics.density
		for (colorHex in cell.colors.take(3)) { // 한 칸에 최대 3개까지만 표시 (그 이상은 겹쳐서 안 보이니까)
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