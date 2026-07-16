package com.chan.bnote.ui.bible

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.chan.bnote.R

class BibleMenuDialogFragment(
	private val isReadingPlanEnabled: Boolean,
	private val isAutoScrollEnabled: Boolean
) : DialogFragment() {

	var onAppendixItemSelected: ((String) -> Unit)? = null
	var onReadingPlanToggled: ((Boolean) -> Unit)? = null
	var onAutoScrollToggled: ((Boolean) -> Unit)? = null
	var onScrapClicked: (() -> Unit)? = null
	var onHymnClicked: (() -> Unit)? = null

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val dialog = Dialog(requireContext(), R.style.RightPanelDialog)
		val view = LayoutInflater.from(requireContext()).inflate(R.layout.panel_bible_menu, null)
		dialog.setContentView(view)

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

		view.findViewById<TextView>(R.id.menu_hymn).setOnClickListener {
			onHymnClicked?.invoke()
			dismiss()
		}

		val appendixContainer = view.findViewById<LinearLayout>(R.id.container_appendix_list)
		val appendixToggle = view.findViewById<TextView>(R.id.menu_appendix)

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
			appendixToggle.text = if (expanding) "부록  ▴" else "부록  ▾"
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
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}