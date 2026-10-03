package com.chan.bnote.ui.sermon

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.chan.bnote.R
import com.chan.bnote.ui.common.RightPanelSize

class SermonMenuDialogFragment : DialogFragment() {

	var onByBookClicked: (() -> Unit)? = null
	var onCategoryManageClicked: (() -> Unit)? = null
	var onPreacherManageClicked: (() -> Unit)? = null
	var onApplicationCategoryManageClicked: (() -> Unit)? = null

	override fun onStart() {
		super.onStart()

		// 폭은 화면 폭의 일정 비율(기기마다 같은 비율로 보이게), 높이는 화면 전체.
		dialog?.window?.let { RightPanelSize.apply(it, requireContext()) }
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
			RightPanelSize.apply(this, requireContext())
			setDimAmount(0.4f)
		}

		view.findViewById<TextView>(R.id.menu_by_book).setOnClickListener {
			onByBookClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_category_manage).setOnClickListener {
			onCategoryManageClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_preacher_manage).setOnClickListener {
			onPreacherManageClicked?.invoke()
			dismiss()
		}

		view.findViewById<TextView>(R.id.menu_application_category_manage).setOnClickListener {
			onApplicationCategoryManageClicked?.invoke()
			dismiss()
		}

		return dialog
	}
}