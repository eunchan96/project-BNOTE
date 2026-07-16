package com.chan.bnote.ui.sermon

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.Preacher
import com.chan.bnote.ui.common.SimpleListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PreacherPickerBottomSheet : BottomSheetDialogFragment() {

	var onPreacherSelected: ((Preacher) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private var preachers: List<Preacher> = emptyList()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_preacher_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_preachers_picker)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		view.findViewById<TextView>(R.id.btn_add_preacher_inline).setOnClickListener {
			showAddDialog()
		}

		loadPreachers()
	}

	private fun loadPreachers() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			preachers = db.preacherDao().getAll()
			recyclerView.adapter = SimpleListAdapter(preachers.map { it.name }) { position ->
				onPreacherSelected?.invoke(preachers[position])
				dismiss()
			}
		}
	}

	private fun showAddDialog() {
		val editText = EditText(requireContext()).apply {
			hint = "설교자 이름"
			setPadding(48, 32, 48, 32)
		}
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 설교자 추가")
			.setView(editText)
			.setPositiveButton("추가") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						val newId = db.preacherDao()
							.insert(Preacher(name = name, sortOrder = preachers.size))
						onPreacherSelected?.invoke(Preacher(id = newId, name = name))
						dismiss()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}
}