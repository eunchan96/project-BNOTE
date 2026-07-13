package com.chan.bnote.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.Sermon
import com.chan.bnote.data.SermonBibleRef
import kotlinx.coroutines.launch
import java.util.Calendar

class AddSermonBottomSheet(
	private val existingSermon: Sermon? = null,
	private val initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.85f

	var onSaved: (() -> Unit)? = null

	private var selectedDateMillis: Long = existingSermon?.sermonDate ?: initialDateMillis
	private var selectedCategoryId: Long? = existingSermon?.categoryId
	private var selectedPreacherId: Long? = existingSermon?.preacherId
	private val bibleRefs = mutableListOf<SermonBibleRef>()

	private lateinit var flexboxRefs: com.google.android.flexbox.FlexboxLayout
	private lateinit var btnPickPreacher: TextView
	private lateinit var btnPickCategory: TextView

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
		val editMemo = view.findViewById<EditText>(R.id.edit_memo)
		val btnDate = view.findViewById<TextView>(R.id.btn_pick_date)
		btnPickPreacher = view.findViewById(R.id.btn_pick_preacher)
		btnPickCategory = view.findViewById(R.id.btn_pick_category)
		flexboxRefs = view.findViewById(R.id.flexbox_bible_refs)

		editTitle.setText(existingSermon?.title ?: "")
		editMemo.setText(existingSermon?.memo ?: "")
		updateDateText(btnDate)

		btnDate.setOnClickListener {
			val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
			DatePickerDialog(
				requireContext(),
				{ _, year, month, day ->
					val picked = Calendar.getInstance()
					picked.set(year, month, day, 0, 0, 0)
					selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
					updateDateText(btnDate)
				},
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
			).show()
		}

		btnPickPreacher.setOnClickListener {
			val picker = PreacherPickerBottomSheet()
			picker.onPreacherSelected = { preacher ->
				selectedPreacherId = preacher.id
				btnPickPreacher.text = preacher.name
			}
			picker.show(parentFragmentManager, "preacher_picker")
		}

		btnPickCategory.setOnClickListener {
			val picker = CategoryPickerBottomSheet()
			picker.onCategorySelected = { category ->
				selectedCategoryId = category?.id
				btnPickCategory.text = category?.name ?: "카테고리 선택"
			}
			picker.show(parentFragmentManager, "category_picker")
		}

		view.findViewById<TextView>(R.id.btn_add_bible_ref).setOnClickListener {
			val rangePicker = BibleRangePickerBottomSheet()
			rangePicker.onRangeSelected = { ref ->
				bibleRefs.add(ref)
				renderBibleRefChips()
			}
			rangePicker.show(parentFragmentManager, "bible_range_picker")
		}

		view.findViewById<TextView>(R.id.btn_save_sermon).setOnClickListener {
			save(editTitle.text.toString().trim(), editMemo.text.toString())
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			selectedPreacherId?.let { id ->
				db.preacherDao().getById(id)?.let { btnPickPreacher.text = it.name }
			}
			selectedCategoryId?.let { id ->
				db.sermonCategoryDao().getById(id)?.let { btnPickCategory.text = it.name }
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
			chip.findViewById<TextView>(R.id.text_chip_label).text = ref.toDisplayLabel()
			chip.findViewById<TextView>(R.id.btn_remove_chip).setOnClickListener {
				bibleRefs.remove(ref)
				renderBibleRefChips()
			}
			flexboxRefs.addView(chip)
		}
	}

	private fun save(title: String, memo: String) {
		if (title.isEmpty()) {
			Toast.makeText(requireContext(), "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
			return
		}
		val preacherId = selectedPreacherId
		if (preacherId == null) {
			Toast.makeText(requireContext(), "설교자를 선택해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			val sermonId: Long
			if (existingSermon == null) {
				sermonId = db.sermonDao().insert(
					Sermon(
						title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memo
					)
				)
			} else {
				sermonId = existingSermon.id
				db.sermonDao().update(
					existingSermon.copy(
						title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memo
					)
				)
				db.sermonBibleRefDao().deleteBySermon(sermonId)
			}

			if (bibleRefs.isNotEmpty()) {
				db.sermonBibleRefDao().insertAll(bibleRefs.map { it.copy(sermonId = sermonId) })
			}

			onSaved?.invoke()
			dismiss()
		}
	}

	private fun updateDateText(view: TextView) {
		val label = DateUtils.formatDate(selectedDateMillis)
		val spannable = android.text.SpannableString(label)
		spannable.setSpan(
			android.text.style.UnderlineSpan(),
			0, label.length,
			android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		view.text = spannable
	}
}