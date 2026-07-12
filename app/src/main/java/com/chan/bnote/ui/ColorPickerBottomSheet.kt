package com.chan.bnote.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColorPickerBottomSheet : BottomSheetDialogFragment() {

	var onColorSelected: ((String) -> Unit)? = null

	companion object {
		val palette = listOf(
			"#FB8C00", "#FDD835", "#8E24AA", "#795548", "#212121", "#E53935",
			"#43A047", "#1E88E5", "#00897B", "#5E35B1", "#F06292", "#6D4C41"
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
				val size = (44 * resources.displayMetrics.density).toInt()
				swatch.layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
					setMargins(8, 8, 8, 8)
				}
				return ColorViewHolder(swatch)
			}

			override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
				val colorHex = palette[position]
				val drawable = android.graphics.drawable.GradientDrawable()
				drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
				drawable.setColor(Color.parseColor(colorHex))
				holder.itemView.background = drawable
				holder.itemView.setOnClickListener {
					onColorSelected?.invoke(colorHex)
					dismiss()
				}
			}

			override fun getItemCount() = palette.size
		}
	}

	class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view)
}