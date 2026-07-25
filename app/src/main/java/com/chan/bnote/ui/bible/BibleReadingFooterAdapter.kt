package com.chan.bnote.ui.bible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

/**
 * 성경 탭 RecyclerView 맨 끝(장 다 읽고 스크롤 끝까지 내렸을 때 보이는 여백)에 붙는 footer.
 * "성경읽기표 체크를 하단 버튼으로 표시" 설정을 켰을 때만, 그 여백 안 맨 아래에 읽음 표시 버튼을 보여준다.
 */
class BibleReadingFooterAdapter(
	private val footerHeightPx: Int,
	private val onButtonClick: () -> Unit
) : RecyclerView.Adapter<BibleReadingFooterAdapter.ViewHolder>() {

	private var showButton = false
	private var isChapterRead = false

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val button: TextView = view.findViewById(R.id.btn_reading_check_bottom)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_bible_reading_footer, parent, false)
		view.layoutParams =
			ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, footerHeightPx)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.button.visibility = if (showButton) View.VISIBLE else View.GONE
		holder.button.text = if (isChapterRead) "읽은 장이에요 (탭하면 취소)" else "읽음 표시하기"
		holder.button.setOnClickListener { onButtonClick() }
	}

	override fun getItemCount() = 1

	fun update(showButton: Boolean, isChapterRead: Boolean) {
		this.showButton = showButton
		this.isChapterRead = isChapterRead
		notifyItemChanged(0)
	}
}