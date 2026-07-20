package com.chan.bnote.ui.mypage

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.mypage.PrayerRequest
import com.google.android.material.checkbox.MaterialCheckBox

class PrayerRequestAdapter(
	private val items: List<PrayerRequest>,
	private val isManageMode: Boolean,
	private val onToggleAnswered: (PrayerRequest) -> Unit,
	private val onEdit: (PrayerRequest) -> Unit,
	private val onDelete: (PrayerRequest) -> Unit
) : RecyclerView.Adapter<PrayerRequestAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val checkbox: MaterialCheckBox = view.findViewById(R.id.checkbox_answered)
		val content: TextView = view.findViewById(R.id.text_prayer_content)
		val date: TextView = view.findViewById(R.id.text_prayer_date)
		val editBtn: ImageView = view.findViewById(R.id.btn_edit_prayer)
		val deleteBtn: ImageView = view.findViewById(R.id.btn_delete_prayer)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_prayer_request, parent, false)
		return ViewHolder(view)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val item = items[position]
		val context = holder.itemView.context

		// 리사이클된 뷰에 이전 아이템의 리스너가 남아있지 않도록 먼저 해제
		holder.checkbox.setOnCheckedChangeListener(null)
		holder.checkbox.isChecked = item.isAnswered

		holder.content.text = item.content
		if (item.isAnswered) {
			holder.content.paintFlags = holder.content.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
			holder.content.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
			holder.date.text = "응답됨 · ${DateUtils.formatDate(item.answeredAt ?: item.createdAt)}"
		} else {
			holder.content.paintFlags =
				holder.content.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
			holder.content.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
			holder.date.text = DateUtils.formatDate(item.createdAt)
		}

		holder.editBtn.visibility = if (isManageMode) View.VISIBLE else View.GONE
		holder.deleteBtn.visibility = if (isManageMode) View.VISIBLE else View.GONE

		holder.checkbox.setOnCheckedChangeListener { _, _ -> onToggleAnswered(item) }
		holder.editBtn.setOnClickListener { onEdit(item) }
		holder.deleteBtn.setOnClickListener { onDelete(item) }
	}

	override fun getItemCount() = items.size
}