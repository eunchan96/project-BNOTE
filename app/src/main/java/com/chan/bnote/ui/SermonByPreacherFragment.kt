package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import kotlinx.coroutines.launch

class SermonByPreacherFragment : Fragment() {

	private lateinit var preacherListContainer: View
	private lateinit var sermonListContainer: View
	private lateinit var preacherRecycler: RecyclerView
	private lateinit var sermonRecycler: RecyclerView
	private lateinit var btnPreacherSort: TextView
	private lateinit var btnSermonSort: TextView
	private lateinit var selectedPreacherText: TextView

	private var preachers: MutableList<String> = mutableListOf()
	private var selectedPreacher: String? = null

	private var preacherSortMode = "NAME"
	private var sermonSortMode = "DATE"

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

		view.findViewById<TextView>(R.id.btn_back_to_preachers).setOnClickListener {
			showPreacherListStep()
		}

		view.findViewById<TextView>(R.id.fab_add_sermon_by_preacher).setOnClickListener {
			val sheet = AddSermonBottomSheet()
			sheet.onSaved = {
				loadPreachers()
				selectedPreacher?.let { loadSermonsForPreacher(it) }
			}
			sheet.show(parentFragmentManager, "add_sermon")
		}

		loadPreachers()
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

	private fun showSermonListStep(preacher: String) {
		preacherListContainer.visibility = View.GONE
		sermonListContainer.visibility = View.VISIBLE
		selectedPreacher = preacher
		selectedPreacherText.text = preacher
		loadSermonsForPreacher(preacher)
	}

	private fun loadPreachers() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val allPreachers = db.sermonDao().getAllPreachers()

			preachers = if (preacherSortMode == "NAME") {
				allPreachers.sorted().toMutableList()
			} else {
				val customOrder = AppSettings.getPreacherCustomOrder(requireContext())
				val ordered = customOrder.filter { it in allPreachers }.toMutableList()
				val rest = allPreachers.filter { it !in ordered }
				(ordered + rest).toMutableList()
			}

			renderPreacherList()
		}
	}

	private fun renderPreacherList() {
		val adapter = SimpleListAdapter(preachers) { position ->
			showSermonListStep(preachers[position])
		}
		preacherRecycler.adapter = adapter

		if (preacherSortMode == "CUSTOM") {
			val dragHelper = ItemTouchHelper(DragReorderHelper { from, to ->
				if (from in preachers.indices && to in preachers.indices) {
					val item = preachers.removeAt(from)
					preachers.add(to, item)
					adapter.notifyItemMoved(from, to)
					AppSettings.setPreacherCustomOrder(requireContext(), preachers)
				}
			})
			dragHelper.attachToRecyclerView(preacherRecycler)
		} else {
			ItemTouchHelper(DragReorderHelper { _, _ -> }).attachToRecyclerView(null)
		}
	}

	private fun loadSermonsForPreacher(preacher: String) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByPreacher(preacher)

			val rows = sermons.map { sermon ->
				val firstRef = db.sermonBibleRefDao().getFirstRef(sermon.id)
				val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
				Triple(sermon, firstRef, category?.colorHex)
			}

			val sortedRows = if (sermonSortMode == "DATE") {
				rows.sortedByDescending { it.first.sermonDate }
			} else {
				rows.sortedWith(
					compareBy(
					{ it.second?.startBookId ?: Int.MAX_VALUE },
					{ it.second?.startChapter ?: Int.MAX_VALUE },
					{ it.second?.startVerse ?: Int.MAX_VALUE }
				))
			}

			val rowData = sortedRows.map { (sermon, ref, colorHex) ->
				SermonRowData(
					sermon = sermon,
					colorHex = colorHex,
					dateLabel = DateUtils.formatDateShort(sermon.sermonDate),
					bibleRefLabel = ref?.toShortLabel() ?: ""
				)
			}

			val emptyText = view?.findViewById<TextView>(R.id.text_empty_preacher_sermons)
			if (rowData.isEmpty()) {
				emptyText?.visibility = View.VISIBLE
				sermonRecycler.visibility = View.GONE
			} else {
				emptyText?.visibility = View.GONE
				sermonRecycler.visibility = View.VISIBLE
				sermonRecycler.adapter = SermonRowAdapter(rowData) { sermon ->
					val detail = SermonDetailBottomSheet(sermon)
					detail.onChanged = { loadSermonsForPreacher(preacher) }
					detail.show(parentFragmentManager, "sermon_detail")
				}
			}
		}
	}
}