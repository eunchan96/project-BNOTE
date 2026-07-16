package com.chan.bnote.ui.sermon

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.Preacher
import com.chan.bnote.ui.common.DragReorderHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SermonByPreacherFragment : Fragment() {

	private lateinit var preacherListContainer: View
	private lateinit var sermonListContainer: View
	private lateinit var preacherRecycler: RecyclerView
	private lateinit var sermonRecycler: RecyclerView
	private lateinit var btnPreacherSort: TextView

	private val addSermonLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadPreachers()
			selectedPreacher?.let { loadSermonsForPreacher(it) }
		}
	}
	private val sermonDetailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadPreachers()
			selectedPreacher?.let { loadSermonsForPreacher(it) }
		}
	}
	private lateinit var btnSermonSort: TextView
	private lateinit var selectedPreacherText: TextView

	private var preachers: MutableList<Preacher> = mutableListOf()
	private var selectedPreacher: Preacher? = null

	private var preacherSortMode = "NAME"
	private var sermonSortMode = "DATE"
	private var isPreacherManageMode = false

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_by_preacher, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		preacherListContainer = view.findViewById(R.id.container_preacher_list)
		sermonListContainer = view.findViewById(R.id.container_preacher_sermons)
		preacherRecycler = view.findViewById(R.id.recycler_preachers)
		sermonRecycler = view.findViewById(R.id.recycler_preacher_sermons)
		btnPreacherSort = view.findViewById(R.id.btn_preacher_sort)
		btnSermonSort = view.findViewById(R.id.btn_sermon_sort)
		selectedPreacherText = view.findViewById(R.id.text_selected_preacher)

		preacherRecycler.layoutManager = LinearLayoutManager(requireContext())
		sermonRecycler.layoutManager = LinearLayoutManager(requireContext())

		preacherSortMode = AppSettings.getPreacherSortMode(requireContext())
		sermonSortMode = AppSettings.getSermonSortMode(requireContext())
		updateSortButtonLabels()

		btnPreacherSort.setOnClickListener { showPreacherSortMenu(it) }
		btnSermonSort.setOnClickListener { showSermonSortMenu(it) }

		view.findViewById<TextView>(R.id.btn_preacher_manage_toggle).setOnClickListener {
			togglePreacherManageMode(it as TextView)
		}

		view.findViewById<TextView>(R.id.btn_add_preacher_in_manage).setOnClickListener {
			showAddPreacherDialog()
		}

		view.findViewById<ImageView>(R.id.btn_back_from_detail).setOnClickListener {
			showPreacherListStep()
		}

		view.findViewById<TextView>(R.id.fab_add_sermon_by_preacher).setOnClickListener {
			addSermonLauncher.launch(AddSermonActivity.createIntent(requireContext()))
		}

		loadPreachers()
	}

	private fun togglePreacherManageMode(button: TextView) {
		isPreacherManageMode = !isPreacherManageMode
		button.text = if (isPreacherManageMode) "완료" else "관리"
		(preacherRecycler.adapter as? PreacherManageAdapter)?.setEditMode(isPreacherManageMode)
		view?.findViewById<TextView>(R.id.btn_add_preacher_in_manage)?.visibility =
			if (isPreacherManageMode) View.VISIBLE else View.GONE
	}

	private fun showAddPreacherDialog() {
		val editText = EditText(requireContext()).apply {
			hint = "설교자 이름"
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = androidx.core.content.ContextCompat.getDrawable(
				requireContext(),
				R.drawable.bg_book_button
			)
		}
		val container = android.widget.FrameLayout(requireContext()).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("설교자 추가")
			.setView(container)
			.setPositiveButton("추가") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						db.preacherDao().insert(Preacher(name = name, sortOrder = preachers.size))
						loadPreachers()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun showEditPreacherDialog(preacher: Preacher) {
		val editText = EditText(requireContext()).apply {
			setText(preacher.name)
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = androidx.core.content.ContextCompat.getDrawable(
				requireContext(),
				R.drawable.bg_book_button
			)
		}
		val container = android.widget.FrameLayout(requireContext()).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("설교자 이름 수정")
			.setView(container)
			.setPositiveButton("저장") { _, _ ->
				val newName = editText.text.toString().trim()
				if (newName.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						db.preacherDao().update(preacher.copy(name = newName))
						loadPreachers()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun confirmDeletePreacher(preacher: Preacher) {
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("설교자 삭제")
			.setMessage("'${preacher.name}'을(를) 삭제할까요? 이 설교자로 등록된 설교는 '미지정' 상태가 돼요.")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					db.preacherDao().clearPreacherFromSermons(preacher.id)
					db.preacherDao().delete(preacher)
					loadPreachers()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun showPreacherSortMenu(anchor: View) {
		val popup = PopupMenu(requireContext(), anchor)
		popup.menu.add(0, 0, 0, "이름순")
		popup.menu.add(0, 1, 1, "직접 설정")
		popup.setOnMenuItemClickListener { item ->
			preacherSortMode = if (item.itemId == 0) "NAME" else "CUSTOM"
			AppSettings.setPreacherSortMode(requireContext(), preacherSortMode)
			updateSortButtonLabels()
			loadPreachers()
			true
		}
		popup.show()
	}

	private fun showSermonSortMenu(anchor: View) {
		val popup = PopupMenu(requireContext(), anchor)
		popup.menu.add(0, 0, 0, "날짜순")
		popup.menu.add(0, 1, 1, "성경순")
		popup.setOnMenuItemClickListener { item ->
			sermonSortMode = if (item.itemId == 0) "DATE" else "BIBLE"
			AppSettings.setSermonSortMode(requireContext(), sermonSortMode)
			updateSortButtonLabels()
			selectedPreacher?.let { loadSermonsForPreacher(it) }
			true
		}
		popup.show()
	}

	private fun updateSortButtonLabels() {
		btnPreacherSort.text = if (preacherSortMode == "NAME") "이름순 ▾" else "직접 설정 ▾"
		btnSermonSort.text = if (sermonSortMode == "DATE") "날짜순 ▾" else "성경순 ▾"
	}

	private fun showPreacherListStep() {
		preacherListContainer.visibility = View.VISIBLE
		sermonListContainer.visibility = View.GONE
		selectedPreacher = null
	}

	private fun showSermonListStep(preacher: Preacher) {
		preacherListContainer.visibility = View.GONE
		sermonListContainer.visibility = View.VISIBLE
		selectedPreacher = preacher
		selectedPreacherText.text = preacher.name
		loadSermonsForPreacher(preacher)
	}

	private fun loadPreachers() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val allPreachers = db.preacherDao().getAll()

			preachers = if (preacherSortMode == "NAME") {
				allPreachers.sortedBy { it.name }.toMutableList()
			} else {
				val customOrderIds = AppSettings.getPreacherCustomOrderIds(requireContext())
				val byId = allPreachers.associateBy { it.id }
				val ordered = customOrderIds.mapNotNull { byId[it] }.toMutableList()
				val rest = allPreachers.filter { it.id !in customOrderIds }
				(ordered + rest).toMutableList()
			}

			val rows = preachers.map { preacher ->
				PreacherRow(preacher, db.sermonDao().getByPreacherId(preacher.id).size)
			}
			renderPreacherList(rows)
		}
	}

	private fun renderPreacherList(rows: List<PreacherRow>) {
		val adapter = PreacherManageAdapter(
			rows = rows,
			isEditMode = isPreacherManageMode,
			onClick = { preacher -> showSermonListStep(preacher) },
			onEdit = { preacher -> showEditPreacherDialog(preacher) },
			onDelete = { preacher -> confirmDeletePreacher(preacher) }
		)
		preacherRecycler.adapter = adapter

		if (preacherSortMode == "CUSTOM") {
			val dragHelper = ItemTouchHelper(DragReorderHelper { from, to ->
				if (from in preachers.indices && to in preachers.indices) {
					val item = preachers.removeAt(from)
					preachers.add(to, item)
					AppSettings.setPreacherCustomOrderIds(requireContext(), preachers.map { it.id })
					loadPreachers()
				}
			})
			dragHelper.attachToRecyclerView(preacherRecycler)
		} else {
			ItemTouchHelper(DragReorderHelper { _, _ -> }).attachToRecyclerView(null)
		}
	}

	private fun loadSermonsForPreacher(preacher: Preacher) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByPreacherId(preacher.id)

			val rowsWithRef = sermons.map { sermon ->
				val firstRef = db.sermonBibleRefDao().getFirstRef(sermon.id)
				sermon to firstRef
			}

			val sortedPairs = if (sermonSortMode == "DATE") {
				rowsWithRef.sortedByDescending { it.first.sermonDate }
			} else {
				rowsWithRef.sortedWith(
					compareBy(
						{ it.second?.startBookId ?: Int.MAX_VALUE },
						{ it.second?.startChapter ?: Int.MAX_VALUE },
						{ it.second?.startVerse ?: Int.MAX_VALUE }
					))
			}

			val rowData = SermonRowBuilder.build(db, sortedPairs.map { it.first })
				.sortedBy { row -> sortedPairs.indexOfFirst { it.first.id == row.sermon.id } }

			val emptyText = view?.findViewById<TextView>(R.id.text_empty_preacher_sermons)
			if (rowData.isEmpty()) {
				emptyText?.visibility = View.VISIBLE
				sermonRecycler.visibility = View.GONE
			} else {
				emptyText?.visibility = View.GONE
				sermonRecycler.visibility = View.VISIBLE
				sermonRecycler.adapter = SermonRowAdapter(rowData) { sermon ->
					sermonDetailLauncher.launch(
						SermonDetailActivity.createIntent(requireContext(), sermon.id)
					)
				}
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}