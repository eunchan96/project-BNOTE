package com.chan.bnote.ui.bible

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.chan.bnote.R

class BibleMenuDialogFragment(
	private val isReadingPlanEnabled: Boolean,
	private val isAutoScrollEnabled: Boolean,
	private val isDndEnabled: Boolean
) : DialogFragment() {

	var onAppendixItemSelected: ((String) -> Unit)? = null
	var onReadingPlanToggled: ((Boolean) -> Unit)? = null
	var onAutoScrollToggled: ((Boolean) -> Unit)? = null

	// true를 반환하면(권한이 있어서 실제로 적용됐으면) 스위치를 그 상태로 유지하고, false면 원래대로
	// 되돌린다(예: 방해금지 권한이 없어서 설정 화면으로 보내고 토글은 취소해야 할 때).
	var onDndToggleRequested: ((Boolean) -> Boolean)? = null
	var onScrapClicked: (() -> Unit)? = null
	var onHymnClicked: (() -> Unit)? = null
	var onHighlightClicked: (() -> Unit)? = null
	var onMemoClicked: (() -> Unit)? = null
	var onBibleKnowledgeClicked: (() -> Unit)? = null

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
		val view = LayoutInflater.from(requireContext()).inflate(R.layout.panel_bible_menu, null)
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

		setupViews(view)
		return dialog
	}

	private fun setupViews(view: View) {
		view.findViewById<TextView>(R.id.menu_scrap).setOnClickListener {
			onScrapClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_highlight).setOnClickListener {
			onHighlightClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_memo).setOnClickListener {
			onMemoClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_bible_knowledge).setOnClickListener {
			onBibleKnowledgeClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_hymn).setOnClickListener {
			onHymnClicked?.invoke()
			dismiss()
		}

		val appendixContainer = view.findViewById<LinearLayout>(R.id.container_appendix_list)
		val appendixToggle = view.findViewById<LinearLayout>(R.id.menu_appendix)
		val appendixToggleIcon = view.findViewById<ImageView>(R.id.icon_appendix_toggle)

		val appendixItems = listOf("주기도문", "사도신경", "십계명", "교독문")
		for (itemName in appendixItems) {
			val itemView = TextView(requireContext()).apply {
				text = itemName
				textSize = 15f
				setPadding(dp(32), dp(14), dp(16), dp(14))
				background = ContextCompat.getDrawable(
					requireContext(), android.R.drawable.list_selector_background
				)
				isClickable = true
				isFocusable = true
				setOnClickListener {
					onAppendixItemSelected?.invoke(itemName)
					dismiss() // 실제 항목 선택 시에는 패널 닫힘
				}
			}
			appendixContainer.addView(itemView)
		}

		appendixToggle.setOnClickListener {
			val expanding = appendixContainer.visibility != View.VISIBLE
			appendixContainer.visibility = if (expanding) View.VISIBLE else View.GONE
			appendixToggleIcon.animate().rotation(if (expanding) 180f else 0f).setDuration(150)
				.start()
		}

		view.findViewById<Switch>(R.id.switch_reading_plan).apply {
			isChecked = isReadingPlanEnabled
			setOnCheckedChangeListener { _, checked ->
				onReadingPlanToggled?.invoke(checked)
				dismiss()
			}
		}
		view.findViewById<Switch>(R.id.switch_auto_scroll).apply {
			isChecked = isAutoScrollEnabled
			setOnCheckedChangeListener { _, checked ->
				onAutoScrollToggled?.invoke(checked)
				dismiss()
			}
		}
		view.findViewById<Switch>(R.id.switch_dnd).apply {
			isChecked = isDndEnabled
			setDndCheckedListener(this)
		}

		view.findViewById<View>(R.id.row_reading_plan_toggle).setOnClickListener {
			val switch = view.findViewById<Switch>(R.id.switch_reading_plan)
			switch.isChecked = !switch.isChecked
		}
		view.findViewById<View>(R.id.row_auto_scroll_toggle).setOnClickListener {
			val switch = view.findViewById<Switch>(R.id.switch_auto_scroll)
			switch.isChecked = !switch.isChecked
		}
		view.findViewById<View>(R.id.row_dnd_toggle).setOnClickListener {
			val switch = view.findViewById<Switch>(R.id.switch_dnd)
			switch.isChecked = !switch.isChecked
		}
	}

	/** 방해금지 권한이 없으면(설정 화면으로 보냈으면) 스위치를 원래 상태로 되돌리고, 있으면 그대로 두고
	 * 메뉴를 닫는다. */
	private fun setDndCheckedListener(switch: Switch) {
		switch.setOnCheckedChangeListener { _, checked ->
			val applied = onDndToggleRequested?.invoke(checked) ?: false
			if (applied) {
				dismiss()
			} else {
				switch.setOnCheckedChangeListener(null)
				switch.isChecked = !checked
				setDndCheckedListener(switch)
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}