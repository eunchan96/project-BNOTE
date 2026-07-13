package com.chan.bnote.ui.scrap

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.scrap.Scrap
import com.chan.bnote.data.scrap.ScrapGroup

sealed class ScrapListItem {
	data class Header(val group: ScrapGroup) : ScrapListItem()
	data class VerseItem(val scrap: Scrap) : ScrapListItem()
}

class ScrapListAdapter(
	private val items: List<ScrapListItem>,
	private val onScrapClick: (Scrap) -> Unit,
	private val onScrapDelete: (Scrap) -> Unit,
	private val onGroupEdit: (ScrapGroup) -> Unit,
	private val onGroupDelete: (ScrapGroup) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	companion object {
		const val TYPE_HEADER = 0
		const val TYPE_VERSE = 1
	}

	class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_group_name)
		val editBtn: TextView = view.findViewById(R.id.btn_edit_group)
		val deleteBtn: TextView = view.findViewById(R.id.btn_delete_group)
	}

	class VerseHolder(view: View) : RecyclerView.ViewHolder(view) {
		val ref: TextView = view.findViewById(R.id.text_scrap_ref)
		val text: TextView = view.findViewById(R.id.text_scrap_text)
		val deleteBtn: TextView = view.findViewById(R.id.btn_delete_scrap)
	}

	override fun getItemViewType(position: Int): Int {
		return when (items[position]) {
			is ScrapListItem.Header -> TYPE_HEADER
			is ScrapListItem.VerseItem -> TYPE_VERSE
		}
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return if (viewType == TYPE_HEADER) {
			HeaderHolder(
				LayoutInflater.from(parent.context)
					.inflate(R.layout.item_scrap_group_header, parent, false)
			)
		} else {
			VerseHolder(
				LayoutInflater.from(parent.context)
					.inflate(R.layout.item_scrap_verse, parent, false)
			)
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		when (val item = items[position]) {
			is ScrapListItem.Header -> {
				holder as HeaderHolder
				holder.name.text = item.group.name
				holder.editBtn.setOnClickListener { onGroupEdit(item.group) }
				holder.deleteBtn.setOnClickListener { onGroupDelete(item.group) }
			}

			is ScrapListItem.VerseItem -> {
				holder as VerseHolder
				val scrap = item.scrap
				val bookName = BibleBooks.nameOf(scrap.bookId)
				holder.ref.text = if (scrap.startVerse == scrap.endVerse) {
					"$bookName ${scrap.chapter}:${scrap.startVerse}"
				} else {
					"$bookName ${scrap.chapter}:${scrap.startVerse}~${scrap.endVerse}"
				}
				holder.text.text = scrap.verseText
				holder.itemView.setOnClickListener { onScrapClick(scrap) }
				holder.deleteBtn.setOnClickListener { onScrapDelete(scrap) }
			}
		}
	}

	override fun getItemCount() = items.size
}