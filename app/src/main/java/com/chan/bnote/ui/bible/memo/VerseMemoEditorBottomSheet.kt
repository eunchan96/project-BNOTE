package com.chan.bnote.ui.bible.memo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 구절 메모 편집 바텀시트. PWA(components/bible/VerseMemoEditorSheet.tsx)와 같은 구조로 만들었다 —
 * 헤더(제목 + 메모 추가 "+" + 닫기 "X"), 절 본문 미리보기, 메모 박스 여러 개(각각 삭제 버튼만
 * 있고), 맨 아래 저장 버튼 하나로 바뀐 박스들을 한꺼번에 저장한다(예전처럼 박스마다 따로
 * 저장 버튼을 두지 않음).
 *
 * 사용하는 쪽에서 bookId/chapter/verse/onChanged를 미리 세팅한 뒤 show()를 부른다:
 * ```
 * VerseMemoEditorBottomSheet().apply {
 *     this.bookId = bookId; this.chapter = chapter; this.verse = verse
 *     onChanged = { ... }
 * }.show(childFragmentManager, "verse_memo_editor")
 * ``` */
class VerseMemoEditorBottomSheet : FixedBottomSheetDialogFragment() {

	var bookId: Int = 1
	var chapter: Int = 1
	var verse: Int = 1

	/** 저장/삭제가 실제로 하나라도 일어났을 때 호출한 쪽에 알려준다(밑줄 등 화면 갱신용). */
	var onChanged: (() -> Unit)? = null

	private class Box(var existing: VerseMemo?, val root: View, val editText: EditText)

	private lateinit var container: LinearLayout
	private val boxes = mutableListOf<Box>()
	private var isSaving = false

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_verse_memo_editor, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val unit = BibleBooks.chapterUnit(bookId)
		view.findViewById<TextView>(R.id.text_sheet_title).text =
			"${BibleBooks.nameOf(bookId)} $chapter$unit ${verse}절 메모"

		container = view.findViewById(R.id.container_memo_boxes)
		view.findViewById<ImageView>(R.id.btn_add_memo_box).setOnClickListener { addBox(null) }
		view.findViewById<ImageView>(R.id.btn_close_sheet).setOnClickListener { dismiss() }
		view.findViewById<TextView>(R.id.btn_save_memo).setOnClickListener { saveAll() }

		loadExisting(view)
	}

	private fun loadExisting(view: View) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val translation = AppSettings.getPrimaryTranslation(requireContext())
			val verseText = db.bibleDao().getVerses(translation, bookId, chapter)
				.find { it.verse == verse }?.text
			view.findViewById<TextView>(R.id.text_verse_preview).text = verseText ?: ""

			val existingMemos = db.verseMemoDao().getAtPosition(bookId, chapter, verse)
			if (existingMemos.isEmpty()) addBox(null) else existingMemos.forEach { addBox(it) }
		}
	}

	private fun addBox(existing: VerseMemo?) {
		val boxView = LayoutInflater.from(requireContext())
			.inflate(R.layout.item_memo_box_v2, container, false)
		val editText = boxView.findViewById<EditText>(R.id.edit_box_text)
		editText.setText(existing?.text ?: "")
		com.chan.bnote.ui.common.TextAutoReplace.attachArrowReplacement(editText)
		com.chan.bnote.ui.common.LinkifyHelper.applySmartLinks(editText)
		editText.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus) com.chan.bnote.ui.common.LinkifyHelper.applySmartLinks(editText)
		}

		val box = Box(existing, boxView, editText)
		boxes.add(box)
		container.addView(boxView)

		boxView.findViewById<ImageView>(R.id.btn_delete_box).setOnClickListener { removeBox(box) }
	}

	private fun removeBox(box: Box) {
		val existing = box.existing
		if (existing == null) {
			boxes.remove(box)
			container.removeView(box.root)
			return
		}
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("메모 삭제")
			.setMessage("이 메모를 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					db.verseMemoDao().delete(existing)
					boxes.remove(box)
					container.removeView(box.root)
					if (boxes.isEmpty()) addBox(null)
					onChanged?.invoke()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun saveAll() {
		if (isSaving) return
		val changed = boxes.filter { box ->
			val text = box.editText.text.toString().trim()
			text.isNotEmpty() && text != (box.existing?.text ?: "")
		}
		if (changed.isEmpty()) {
			dismiss()
			return
		}

		isSaving = true
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			for (box in changed) {
				val text = box.editText.text.toString().trim()
				val existing = box.existing
				if (existing != null) {
					if (existing.text != text) {
						val updated =
							existing.copy(text = text, updatedAt = System.currentTimeMillis())
						db.verseMemoDao().update(updated)
						box.existing = updated
					}
				} else {
					val newId = db.verseMemoDao().insert(
						VerseMemo(bookId = bookId, chapter = chapter, verse = verse, text = text)
					)
					box.existing = VerseMemo(
						id = newId, bookId = bookId, chapter = chapter, verse = verse, text = text
					)
				}
			}
			isSaving = false
			onChanged?.invoke()
			dismiss()
		}
	}
}