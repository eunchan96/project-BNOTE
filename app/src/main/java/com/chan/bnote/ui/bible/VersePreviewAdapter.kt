package com.chan.bnote.ui.bible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleVerse

/**
 * 스와이프로 장을 넘길 때, 다음/이전 장이 옆에서 따라 들어오는 것처럼 보여주기 위한 아주 단순한
 * 미리보기용 어댑터. 하이라이트 · 메모 · 선택 같은 인터랙션은 전혀 없고, 절 번호 + 본문 텍스트만
 * 보여준다(item_verse.xml을 재사용하되 필요한 것만 채운다).
 */
class VersePreviewAdapter(private val verses: List<BibleVerse>) :
	RecyclerView.Adapter<VersePreviewAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val number: TextView = view.findViewById(R.id.text_verse_number)
		val content: TextView = view.findViewById(R.id.text_verse_content)
		val title: TextView = view.findViewById(R.id.text_verse_title)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.item_verse, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val verse = verses[position]
		holder.number.text = verse.verse.toString()
		holder.content.text = verse.text
		if (!verse.title.isNullOrBlank()) {
			holder.title.text = "<${verse.title}>"
			holder.title.visibility = View.VISIBLE
		} else {
			holder.title.visibility = View.GONE
		}
	}

	override fun getItemCount() = verses.size
}