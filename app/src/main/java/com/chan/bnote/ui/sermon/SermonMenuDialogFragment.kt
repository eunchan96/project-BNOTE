package com.chan.bnote.ui.sermon

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.chan.bnote.R

class SermonMenuDialogFragment : DialogFragment() {

	var onCategoryManageClicked: (() -> Unit)? = null

	/** 지금 보이는 서브탭(캘린더/성경별/설교자별)이 SermonSortableFragment라면 SermonFragment가
	 * 채워준다. null이면(정렬을 지원 안 하는 화면이면) 정렬 섹션 자체를 숨긴다. */
	var sortTarget: SermonSortableFragment? = null

	override fun onStart() {
		super.onStart()

		dialog?.window?.apply {
			setGravity(Gravity.END)
			setLayout(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT
			)
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val dialog = Dialog(requireContext(), R.style.RightPanelDialog)
		val view = LayoutInflater.from(requireContext()).inflate(R.layout.panel_sermon_menu, null)
		dialog.setContentView(view)

		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
			v.updatePadding(top = top)
			insets
		}

		dialog.window?.apply {
			setGravity(Gravity.END)
			setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
			setDimAmount(0.4f)
		}

		buildSortSection(view)

		view.findViewById<TextView>(R.id.menu_category_manage).setOnClickListener {
			onCategoryManageClicked?.invoke()
			dismiss()
		}

		return dialog
	}

	private fun buildSortSection(view: View) {
		val target = sortTarget
		val label = view.findViewById<TextView>(R.id.text_sort_section_label)
		val container = view.findViewById<LinearLayout>(R.id.container_sort_options)
		val divider = view.findViewById<View>(R.id.divider_sort_options)

		if (target == null) {
			label.visibility = View.GONE
			container.visibility = View.GONE
			divider.visibility = View.GONE
			return
		}

		label.visibility = View.VISIBLE
		container.visibility = View.VISIBLE
		divider.visibility = View.VISIBLE
		container.removeAllViews()

		val currentMode = target.getCurrentSortMode()
		for ((code, name) in target.getSortOptions()) {
			val row = TextView(requireContext()).apply {
				text = if (code == currentMode) "✓ $name" else name
				textSize = 15f
				setPadding(dp(16), dp(12), dp(16), dp(12))
				setTextColor(
					ContextCompat.getColor(
						requireContext(),
						if (code == currentMode) R.color.brown_primary else R.color.text_primary
					)
				)
				background = ContextCompat.getDrawable(
					requireContext(), android.R.drawable.list_selector_background
				)
				isClickable = true
				isFocusable = true
				setOnClickListener {
					target.setSortMode(code)
					dismiss()
				}
			}
			container.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}