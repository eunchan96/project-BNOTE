package com.chan.bnote.ui.sermon

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.chan.bnote.R

class SermonMenuDialogFragment : DialogFragment() {

	var onCategoryManageClicked: (() -> Unit)? = null

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

		view.findViewById<TextView>(R.id.menu_category_manage).setOnClickListener {
			onCategoryManageClicked?.invoke()
			dismiss()
		}

		return dialog
	}
}