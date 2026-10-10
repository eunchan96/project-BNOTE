package com.chan.bnote.ui.mypage.gratitude

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R

/**
 * 감사 노트 작성 화면의 "순서 변경" 모드 목록. 항목 글자만 보여주고(수정 불가), 왼쪽 ≡ 손잡이를 누르는
 * 순간 바로 드래그가 시작된다(카테고리 관리 화면과 같은 방식, DragReorderHelper 사용).
 */
class GratitudeReorderAdapter(
	initialTexts: List<String>,
	private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<GratitudeReorderAdapter.ViewHolder>() {

	private val texts = initialTexts.toMutableList()

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val dragHandle: TextView = view.findViewById(R.id.text_drag_handle)
		val text: TextView = view.findViewById(R.id.text_gratitude_reorder)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val v = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_gratitude_reorder_row, parent, false)
		return ViewHolder(v)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		holder.text.text = texts[position]
		holder.dragHandle.setOnTouchListener { _, event ->
			if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(holder)
			false
		}
	}

	override fun getItemCount() = texts.size

	fun moveItem(from: Int, to: Int) {
		if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return
		val item = texts.removeAt(from)
		texts.add(to, item)
		notifyItemMoved(from, to)
	}

	fun currentTexts(): List<String> = texts.toList()
}