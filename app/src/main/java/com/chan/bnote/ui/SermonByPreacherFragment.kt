package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

	private lateinit var preacherRecycler: RecyclerView
	private lateinit var sermonRecycler: RecyclerView
	private lateinit var btnPreacherSort: TextView
	private lateinit var btnSermonSort: TextView
	private lateinit var selectedPreacherText: TextView

	private var preachers: MutableList<String> = mutableListOf()
	private var selectedPreacher: String? = null

	private var preacherSortMode = "NAME" // NAME | CUSTOM
	private var sermonSortMode = "DATE"   // DATE | BIBLE

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_by_preacher, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

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

		btnPreacherSort.setOnClickListener {
			val sheet = SortOptionBottomSheet("설교자 정렬", listOf("이름순", "직접 설정"))
			sheet.onSelected = { position ->
				preacherSortMode = if (position == 0) "NAME" else "CUSTOM"
				AppSettings.setPreacherSortMode(requireContext(), preacherSortMode)
				updateSortButtonLabels()
				loadPreachers()
			}
			sheet.show(parentFragmentManager, "preacher_sort")
		}

		btnSermonSort.setOnClickListener {
			val sheet = SortOptionBottomSheet("설교 정렬", listOf("날짜순", "성경순"))
			sheet.onSelected = { position ->
				sermonSortMode = if (position == 0) "DATE" else "BIBLE"
				AppSettings.setSermonSortMode(requireContext(), sermonSortMode)
				updateSortButtonLabels()
				selectedPreacher?.let { loadSermonsForPreacher(it) }
			}
			sheet.show(parentFragmentManager, "sermon_sort")
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

	private fun updateSortButtonLabels() {
		btnPreacherSort.text = if (preacherSortMode == "NAME") "이름순 ▾" else "직접 설정 ▾"
		btnSermonSort.text = if (sermonSortMode == "DATE") "날짜순 ▾" else "성경순 ▾"
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
			selectedPreacher = preachers[position]
			selectedPreacherText.text = selectedPreacher
			loadSermonsForPreacher(preachers[position])
		}
		preacherRecycler.adapter = adapter

		// 기존 드래그 헬퍼가 붙어있으면 해제 후 다시 부착 (중복 방지)
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
			var sermons = db.sermonDao().getByPreacher(preacher)

			// 각 설교의 대표 구절(정렬/표시용) 미리 로드
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