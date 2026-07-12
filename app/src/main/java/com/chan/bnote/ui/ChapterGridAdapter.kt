package com.chan.bnote.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

data class ChapterCell(
	val chapter: Int,
	val colors: List<String>
)

class ChapterGridAdapter(
	private val chapters: List<ChapterCell>,
	private val selectedChapter: Int, // 추가
	private val onChapterClick: (ChapterCell) -> Unit
) : RecyclerView.Adapter<ChapterGridAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val number: TextView = view.findViewById(R.id.text_day_number)
		val barsContainer: LinearLayout = view.findViewById(R.id.container_bars)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val cell = chapters[position]
		holder.number.text = cell.chapter.toString()

		val isSelected = cell.chapter == selectedChapter

		if (isSelected) {
			val drawable = android.graphics.drawable.GradientDrawable()
			drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
			drawable.setColor(Color.parseColor("#795548"))
			holder.number.background = drawable
			val pad = (4 * holder.itemView.resources.displayMetrics.density).toInt()
			holder.number.setPadding(pad, pad, pad, pad)
			holder.number.setTextColor(Color.WHITE)
			holder.number.setTypeface(null, android.graphics.Typeface.BOLD)
		} else {
			holder.number.background = null
			holder.number.setPadding(0, 0, 0, 0)
			holder.number.setTextColor(Color.parseColor("#333333"))
			holder.number.setTypeface(null, android.graphics.Typeface.NORMAL)
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

		holder.itemView.setOnClickListener { onChapterClick(cell) }
	}

	override fun getItemCount() = chapters.size
}