package com.chan.bnote.ui.mypage.readingplan

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

class ReadingPlanChapterGridAdapter(
	private val maxChapter: Int,
	private var readChapters: Set<Int>,
	private val onToggle: (chapter: Int) -> Unit,
	private val onNavigate: (chapter: Int) -> Unit
) : RecyclerView.Adapter<ReadingPlanChapterGridAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val number: TextView = view.findViewById(R.id.text_chapter_number)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_reading_plan_chapter_cell, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = maxChapter

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val chapter = position + 1
		val isRead = chapter in readChapters

		holder.number.text = chapter.toString()
		holder.number.setTextColor(
			ContextCompat.getColor(
				holder.itemView.context,
				if (isRead) R.color.white else R.color.text_primary
			)
		)
		holder.itemView.background = ContextCompat.getDrawable(
			holder.itemView.context,
			if (isRead) R.drawable.bg_book_progress_done else R.drawable.bg_book_progress_none
		)

		holder.itemView.setOnClickListener { onToggle(chapter) }
		holder.itemView.setOnLongClickListener {
			onNavigate(chapter)
			true
		}
	}

	/** 읽음 상태가 바뀐 뒤 목록 전체를 다시 그린다. */
	fun updateReadChapters(newReadChapters: Set<Int>) {
		readChapters = newReadChapters
		notifyDataSetChanged()
	}
}