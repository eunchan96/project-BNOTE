package com.chan.bnote.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.Scrap

class ScrapVerseAdapter(
	private val scraps: List<Scrap>,
	private var isEditMode: Boolean,
	private val onClick: (Scrap) -> Unit,
	private val onDelete: (Scrap) -> Unit
) : RecyclerView.Adapter<ScrapVerseAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val ref: TextView = view.findViewById(R.id.text_scrap_ref)
		val text: TextView = view.findViewById(R.id.text_scrap_text)
		val deleteBtn: TextView = view.findViewById(R.id.btn_delete_scrap)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_scrap_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val scrap = scraps[position]
		val bookName = BibleBooks.nameOf(scrap.bookId)
		holder.ref.text = if (scrap.startVerse == scrap.endVerse) {
			"$bookName ${scrap.chapter}:${scrap.startVerse}"
		} else {
			"$bookName ${scrap.chapter}:${scrap.startVerse}~${scrap.endVerse}"
		}
		holder.text.text = scrap.verseText

		holder.deleteBtn.visibility = if (isEditMode) View.VISIBLE else View.GONE
		holder.deleteBtn.setOnClickListener { onDelete(scrap) }

		holder.itemView.setOnClickListener {
			if (!isEditMode) onClick(scrap)
		}

		holder.itemView.setOnLongClickListener {
			if (!isEditMode) {
				copyScrapToClipboard(holder.itemView.context, scrap)
			}
			true
		}
	}

	private fun copyScrapToClipboard(context: Context, scrap: Scrap) {
		val shortRef = if (scrap.startVerse == scrap.endVerse) {
			"(${BibleBooks.shortNameOf(scrap.bookId)} ${scrap.chapter}:${scrap.startVerse})"
		} else {
			"(${BibleBooks.shortNameOf(scrap.bookId)} ${scrap.chapter}:${scrap.startVerse}~${scrap.endVerse})"
		}
		val text = "$shortRef ${scrap.verseText.replace("\n", " ")}"

		val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		clipboard.setPrimaryClip(ClipData.newPlainText("scrap", text))
		Toast.makeText(context, "복사했어요", Toast.LENGTH_SHORT).show()
	}

	override fun getItemCount() = scraps.size

	fun setEditMode(enabled: Boolean) {
		isEditMode = enabled
		notifyDataSetChanged()
	}
}