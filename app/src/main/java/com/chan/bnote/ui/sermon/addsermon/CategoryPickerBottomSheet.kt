package com.chan.bnote.ui.sermon.addsermon

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.chan.bnote.ui.common.ColorPickerBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CategoryPickerBottomSheet : FixedBottomSheetDialogFragment() {

	// null = 선택 안 함
	var onCategorySelected: ((SermonCategory?) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private var categories: List<SermonCategory> = emptyList()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_category_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_categories_picker)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		view.findViewById<TextView>(R.id.btn_add_category_inline).setOnClickListener {
			showAddDialog()
		}

		loadCategories()
	}

	private fun loadCategories() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			categories = db.sermonCategoryDao().getAll()
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

	private fun showAddDialog() {
		val editText = EditText(requireContext()).apply {
			hint = "카테고리 이름"
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = androidx.core.content.ContextCompat.getDrawable(
				requireContext(),
				R.drawable.bg_book_button
			)
		}
		val container = FrameLayout(requireContext()).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}

		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 카테고리 추가")
			.setView(container)
			.setPositiveButton("색상 선택 및 저장") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isEmpty()) return@setPositiveButton
				val colorPicker = ColorPickerBottomSheet()
				colorPicker.onColorSelected = { color ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						val newId = db.sermonCategoryDao().insert(
							SermonCategory(
								name = name,
								colorHex = color,
								sortOrder = categories.size
							)
						)
						onCategorySelected?.invoke(
							SermonCategory(
								id = newId,
								name = name,
								colorHex = color
							)
						)
						dismiss()
					}
				}
				colorPicker.show(parentFragmentManager, "color_picker")
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val dot: View = view.findViewById(R.id.color_dot)
		val name: TextView = view.findViewById(R.id.text_category_name)
	}
}