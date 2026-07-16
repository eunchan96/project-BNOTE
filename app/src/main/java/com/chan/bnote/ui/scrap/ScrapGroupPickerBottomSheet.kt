package com.chan.bnote.ui.scrap

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
import com.chan.bnote.data.scrap.ScrapGroup
import com.chan.bnote.ui.common.SimpleListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ScrapGroupPickerBottomSheet : BottomSheetDialogFragment() {

	var onGroupSelected: ((ScrapGroup) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private var groups: List<ScrapGroup> = emptyList()

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_scrap_group_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_scrap_groups)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		view.findViewById<TextView>(R.id.btn_add_group_inline).setOnClickListener {
			showAddGroupDialog()
		}

		loadGroups()
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			groups = db.scrapDao().getAllGroups()
			recyclerView.adapter = SimpleListAdapter(groups.map { it.name }) { position ->
				onGroupSelected?.invoke(groups[position])
				dismiss()
			}
		}
	}

	private fun showAddGroupDialog() {
		val editText = EditText(requireContext()).apply {
			hint = "그룹 이름"
			setPadding(48, 32, 48, 32)
		}
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 그룹 추가")
			.setView(editText)
			.setPositiveButton("추가") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						db.scrapDao().insertGroup(ScrapGroup(name = name, sortOrder = groups.size))
						loadGroups()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}
}