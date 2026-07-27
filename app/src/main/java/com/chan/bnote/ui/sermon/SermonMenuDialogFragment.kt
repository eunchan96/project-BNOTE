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
	var onApplicationClicked: (() -> Unit)? = null

	/** 지금 보이는 서브탭(캘린더/성경별/설교자별)이 SermonSortableFragment라면 SermonFragment가
	 * 채워준다. null이면(정렬을 지원 안 하는 화면이면) 정렬 섹션 자체를 숨긴다. */
	var sortTarget: SermonSortableFragment? = null

	/** "캘린더", "성경별", "설교자별"처럼 지금 어느 탭의 정렬인지 구분해서 보여주기 위한 이름. */
	var sortTabName: String? = null

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

		view.findViewById<TextView>(R.id.menu_application).setOnClickListener {
			onApplicationClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_category_manage).setOnClickListener {
			onCategoryManageClicked?.invoke()
			dismiss()
		}

		return dialog
	}

	private fun buildSortSection(view: View) {
		val target = sortTarget
		val toggle = view.findViewById<TextView>(R.id.menu_sort_toggle)
		val divider = view.findViewById<View>(R.id.divider_sort_options)
		val optionsContainer = view.findViewById<LinearLayout>(R.id.container_sort_options)
		val sectionLabel = view.findViewById<TextView>(R.id.text_sort_section_label)
		val rowsContainer = view.findViewById<LinearLayout>(R.id.container_sort_rows)

		if (target == null) {
			toggle.visibility = View.GONE
			divider.visibility = View.GONE
			optionsContainer.visibility = View.GONE
			return
		}

		toggle.visibility = View.VISIBLE
		divider.visibility = View.VISIBLE
		// "부록"처럼 평소엔 접혀 있다가 눌러야 펼쳐진다.
		toggle.text = "설교 정렬  ▾"
		optionsContainer.visibility = View.GONE

		toggle.setOnClickListener {
			val expanding = optionsContainer.visibility != View.VISIBLE
			optionsContainer.visibility = if (expanding) View.VISIBLE else View.GONE
			toggle.text = if (expanding) "설교 정렬  ▴" else "설교 정렬  ▾"
		}

		sectionLabel.text = if (sortTabName != null) "${sortTabName} 정렬" else "정렬 기준"

		val currentMode = target.getCurrentSortMode()
		rowsContainer.removeAllViews()
		for ((code, name) in target.getSortOptions()) {
			val row = TextView(requireContext()).apply {
				text = if (code == currentMode) "✓ $name" else name
				textSize = 15f
				setPadding(dp(16), dp(10), dp(16), dp(10))
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
			rowsContainer.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}