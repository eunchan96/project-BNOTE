package com.chan.bnote.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.Sermon
import com.chan.bnote.data.SermonBibleRef
import com.chan.bnote.data.SermonCategory
import kotlinx.coroutines.launch
import java.util.Calendar

class AddSermonBottomSheet(
	private val existingSermon: Sermon? = null,
	private val initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis()) // 추가
) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.85f

	var onSaved: (() -> Unit)? = null

	private var selectedDateMillis: Long = existingSermon?.sermonDate ?: initialDateMillis
	private var selectedCategoryId: Long? = existingSermon?.categoryId
	private val bibleRefs = mutableListOf<SermonBibleRef>()
	private var categories: List<SermonCategory> = emptyList()

	private lateinit var flexboxRefs: com.google.android.flexbox.FlexboxLayout

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_add_sermon, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_sheet_header).text =
			if (existingSermon == null) "설교 기록 추가" else "설교 기록 수정"

		val editTitle = view.findViewById<EditText>(R.id.edit_title)
		val editPreacher = view.findViewById<EditText>(R.id.edit_preacher)
		val editMemo = view.findViewById<EditText>(R.id.edit_memo)
		val btnDate = view.findViewById<TextView>(R.id.btn_pick_date)
		val categoryRecycler = view.findViewById<RecyclerView>(R.id.recycler_categories)
		flexboxRefs = view.findViewById(R.id.flexbox_bible_refs)

		editTitle.setText(existingSermon?.title ?: "")
		editPreacher.setText(existingSermon?.preacher ?: "")
		editMemo.setText(existingSermon?.memo ?: "")
		btnDate.text = DateUtils.formatDate(selectedDateMillis)

		btnDate.setOnClickListener {
			val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
			DatePickerDialog(
				requireContext(),
				{ _, year, month, day ->
					val picked = Calendar.getInstance()
					picked.set(year, month, day, 0, 0, 0)
					selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
					btnDate.text = DateUtils.formatDate(selectedDateMillis)
				},
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
			).show()
		}

		categoryRecycler.layoutManager =
			LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

		view.findViewById<TextView>(R.id.btn_add_bible_ref).setOnClickListener {
			val picker = BibleRangePickerBottomSheet()
			picker.onRangeSelected = { ref ->
				bibleRefs.add(ref)
				renderBibleRefChips()
			}
			picker.show(parentFragmentManager, "bible_range_picker")
		}

		view.findViewById<TextView>(R.id.btn_save_sermon).setOnClickListener {
			save(
				editTitle.text.toString().trim(),
				editPreacher.text.toString().trim(),
				editMemo.text.toString()
			)
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			categories = db.sermonCategoryDao().getAll()
			categoryRecycler.adapter =
				CategoryChipAdapter(categories, selectedCategoryId) { category ->
					selectedCategoryId = category.id
					categoryRecycler.adapter?.notifyDataSetChanged()
				}

			if (existingSermon != null) {
				bibleRefs.addAll(db.sermonBibleRefDao().getBySermon(existingSermon.id))
				renderBibleRefChips()
			}
		}
	}

	private fun renderBibleRefChips() {
		flexboxRefs.removeAllViews()
		for (ref in bibleRefs) {
			val chip = LayoutInflater.from(requireContext())
				.inflate(R.layout.item_bible_ref_chip, flexboxRefs, false)
			chip.findViewById<TextView>(R.id.text_chip_label).text =
				"${BibleBooks.nameOf(ref.startBookId)} ${ref.startChapter}:${ref.startVerse}~${ref.endChapter}:${ref.endVerse}"
			chip.findViewById<TextView>(R.id.btn_remove_chip).setOnClickListener {
				bibleRefs.remove(ref)
				renderBibleRefChips()
			}
			flexboxRefs.addView(chip)
		}
	}

	private fun save(title: String, preacher: String, memo: String) {
		if (title.isEmpty() || preacher.isEmpty()) {
			Toast.makeText(requireContext(), "제목과 설교자를 입력해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			val sermonId: Long
			if (existingSermon == null) {
				sermonId = db.sermonDao().insert(
					Sermon(
						title = title, preacher = preacher, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memo
					)
				)
			} else {
				sermonId = existingSermon.id
				db.sermonDao().update(
					existingSermon.copy(
						title = title, preacher = preacher, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memo
					)
				)
				db.sermonBibleRefDao().deleteBySermon(sermonId) // 기존 구절 지우고 다시 저장
			}

			if (bibleRefs.isNotEmpty()) {
				db.sermonBibleRefDao().insertAll(bibleRefs.map { it.copy(sermonId = sermonId) })
			}

			onSaved?.invoke()
			dismiss()
		}
	}

	private class CategoryChipAdapter(
		private val items: List<SermonCategory>,
		private var selectedId: Long?,
		private val onSelect: (SermonCategory) -> Unit
	) : RecyclerView.Adapter<CategoryChipAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val dot: View = view.findViewById(R.id.chip_color_dot)
			val name: TextView = view.findViewById(R.id.chip_category_name)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.item_category_chip, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val category = items[position]
			val drawable = GradientDrawable()
			drawable.shape = GradientDrawable.OVAL
			drawable.setColor(Color.parseColor(category.colorHex))
			holder.dot.background = drawable
			holder.name.text = category.name

			val isSelected = category.id == selectedId
			holder.itemView.background = if (isSelected) {
				GradientDrawable().apply {
					setColor(Color.parseColor("#F5F5F5"))
					cornerRadius = 8f * holder.itemView.resources.displayMetrics.density
					setStroke(2, Color.parseColor(category.colorHex))
				}
			} else null

			holder.itemView.setOnClickListener {
				selectedId = category.id
				onSelect(category)
			}
		}

		override fun getItemCount() = items.size
	}
}