package com.chan.bnote.ui.sermon.addsermon

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CategoryPickerBottomSheet : BottomSheetDialogFragment() {

	// null = 선택 안 함
	var onCategorySelected: ((SermonCategory?) -> Unit)? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_category_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_categories_picker)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val categories = db.sermonCategoryDao().getAll()
			val items: List<SermonCategory?> = categories + listOf(null) // 맨 끝에 "선택 안 함"

			recyclerView.adapter = object : RecyclerView.Adapter<PickerViewHolder>() {
				override fun onCreateViewHolder(
					parent: ViewGroup,
					viewType: Int
				): PickerViewHolder {
					val v = LayoutInflater.from(parent.context)
						.inflate(R.layout.item_category, parent, false)
					return PickerViewHolder(v)
				}

				override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
					val category = items[position]
					if (category == null) {
						holder.dot.visibility = View.GONE
						holder.name.text = "선택 안 함"
					} else {
						holder.dot.visibility = View.VISIBLE
						val drawable = GradientDrawable()
						drawable.shape = GradientDrawable.OVAL
						drawable.setColor(Color.parseColor(category.colorHex))
						holder.dot.background = drawable
						holder.name.text = category.name
					}
					holder.itemView.setOnClickListener {
						onCategorySelected?.invoke(category)
						dismiss()
					}
				}

				override fun getItemCount() = items.size
			}
		}
	}

	class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val dot: View = view.findViewById(R.id.color_dot)
		val name: TextView = view.findViewById(R.id.text_category_name)
	}
}