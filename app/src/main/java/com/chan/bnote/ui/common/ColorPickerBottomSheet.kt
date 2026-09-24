package com.chan.bnote.ui.common

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.ui.FixedBottomSheetDialogFragment

class ColorPickerBottomSheet(
	private val includeNoneOption: Boolean = false
) : FixedBottomSheetDialogFragment() {

	/** "없음" 선택 시 빈 문자열("")이 전달된다. */
	var onColorSelected: ((String) -> Unit)? = null

	companion object {
		// 색상환 순서(빨강→주황→노랑→초록→파랑→보라)로 자연스럽게 이어지도록 정렬하고,
		// 끝에 갈색·회색 계열 무채색을 덧붙였다. 기존보다 6개 늘려 더 다양하게 골라 쓸 수 있다.
		val palette = listOf(
			"#E53935", "#D81B60", "#F06292", "#8E24AA", "#5E35B1", "#3949AB",
			"#1E88E5", "#039BE5", "#00ACC1", "#00897B", "#43A047", "#7CB342",
			"#C0CA33", "#FDD835", "#FFB300", "#FB8C00", "#F4511E", "#6D4C41",
			"#A1887F", "#757575", "#9E9E9E", "#546E7A", "#212121", "#B71C1C"
		)
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_color_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_colors)
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 6)
		recyclerView.adapter = object : RecyclerView.Adapter<ColorViewHolder>() {
			override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
				val swatch = View(parent.context)
				val density = resources.displayMetrics.density
				val size = (36 * density).toInt()
				val margin = (6 * density).toInt()
				swatch.layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
					setMargins(margin, margin, margin, margin)
				}
				return ColorViewHolder(swatch)
			}

			override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
				if (includeNoneOption && position == 0) {
					val drawable = android.graphics.drawable.GradientDrawable()
					drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
					drawable.setColor(Color.parseColor("#E0E0E0"))
					drawable.setStroke(
						(1 * resources.displayMetrics.density).toInt(),
						Color.parseColor("#9E9E9E")
					)
					holder.itemView.background = drawable
					holder.itemView.contentDescription = "없음"
					holder.itemView.setOnClickListener {
						onColorSelected?.invoke("")
						dismiss()
					}
					return
				}

				val colorHex = palette[position - if (includeNoneOption) 1 else 0]
				val drawable = android.graphics.drawable.GradientDrawable()
				drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
				drawable.setColor(Color.parseColor(colorHex))
				holder.itemView.background = drawable
				holder.itemView.setOnClickListener {
					onColorSelected?.invoke(colorHex)
					dismiss()
				}
			}

			override fun getItemCount() = palette.size + if (includeNoneOption) 1 else 0
		}
	}

	class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view)
}