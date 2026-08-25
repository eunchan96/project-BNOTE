package com.chan.bnote.ui.bible.memo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 단어 메모 편집 바텀시트. PWA(components/bible/WordMemoSheet.tsx)와 같은 구조로 만들었다 —
 * 헤더(제목 + 메모 추가 "+" + 닫기 "X"), 선택한 단어 미리보기, 메모 박스 여러 개(각각 "다른
 * 구절에도 추가" 체크박스 + 삭제 버튼), 맨 아래 저장 버튼 하나로 바뀐 박스들을 한꺼번에 저장한다.
 *
 * PWA에는 "선택한 부분과 겹치는 기존 메모"를 접어서 같이 보여주는 기능이 있는데, 안드로이드
 * 쪽 DB 조회(getAtPosition)는 정확히 같은 위치의 메모만 가져오는 구조라 이번엔 그 부분은
 * 그대로 두고 옮기지 않았다 — 필요하면 별도로 추가하면 된다. */
class WordMemoEditorBottomSheet : FixedBottomSheetDialogFragment() {

	var translation: String = "NKRV"
	var bookId: Int = 1
	var chapter: Int = 1
	var verse: Int = 1
	var startOffset: Int = 0
	var endOffset: Int = 0
	var segment: Int = 0

	var onChanged: (() -> Unit)? = null

	private class Box(
		var existing: WordMemo?,
		val root: View,
		val editText: EditText,
		val checkbox: CheckBox
	)

	private var wordText = ""
	private lateinit var container: LinearLayout
	private val boxes = mutableListOf<Box>()
	private var isSaving = false

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_word_memo_editor, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		container = view.findViewById(R.id.container_memo_boxes)
		view.findViewById<ImageView>(R.id.btn_add_memo_box).setOnClickListener { addBox(null) }
		view.findViewById<TextView>(R.id.btn_save_memo).setOnClickListener { saveAll() }

		loadExisting(view)
	}

	private fun loadExisting(view: View) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val verseRow =
				db.bibleDao().getVerses(translation, bookId, chapter).find { it.verse == verse }
			val verseText = if (segment == 1) verseRow?.text2 ?: "" else verseRow?.text ?: ""
			val safeStart = startOffset.coerceIn(0, verseText.length)
			val safeEnd = endOffset.coerceIn(safeStart, verseText.length)
			wordText = verseText.substring(safeStart, safeEnd)
			view.findViewById<TextView>(R.id.text_selected_word).text = wordText

			val existingMemos = db.wordMemoDao()
				.getAtPosition(translation, bookId, chapter, verse, startOffset, endOffset, segment)
			if (existingMemos.isEmpty()) addBox(null) else existingMemos.forEach { addBox(it) }
		}
	}

	private fun addBox(existing: WordMemo?) {
		val boxView = LayoutInflater.from(requireContext())
			.inflate(R.layout.item_memo_box_v2, container, false)
		val editText = boxView.findViewById<EditText>(R.id.edit_box_text)
		editText.setText(existing?.text ?: "")
		com.chan.bnote.ui.common.TextAutoReplace.attachArrowReplacement(editText)
		com.chan.bnote.ui.common.LinkifyHelper.applySmartLinks(editText)
		editText.setOnFocusChangeListener { _, hasFocus ->
			if (!hasFocus) com.chan.bnote.ui.common.LinkifyHelper.applySmartLinks(editText)
		}

		val checkbox = boxView.findViewById<CheckBox>(R.id.chk_box_propagate)
		checkbox.visibility = View.VISIBLE

		val box = Box(existing, boxView, editText, checkbox)
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
					db.wordMemoDao().delete(existing)
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
			val propagateRequests = mutableListOf<String>()

			for (box in changed) {
				val text = box.editText.text.toString().trim()
				val existing = box.existing
				if (existing != null) {
					if (existing.text != text) {
						val updated =
							existing.copy(text = text, updatedAt = System.currentTimeMillis())
						db.wordMemoDao().update(updated)
						box.existing = updated
					}
				} else {
					val newId = db.wordMemoDao().insert(
						WordMemo(
							translation = translation,
							bookId = bookId,
							chapter = chapter,
							verse = verse,
							startOffset = startOffset,
							endOffset = endOffset,
							segment = segment,
							text = text
						)
					)
					box.existing = db.wordMemoDao().getById(newId)
				}
				if (box.checkbox.isChecked) propagateRequests.add(text)
			}

			isSaving = false
			onChanged?.invoke()

			// 체크된 게 있으면 먼저 저장은 다 끝낸 뒤(위에서 완료), 확산 여부는 확인 다이얼로그로
			// 하나씩 물어본다. 여러 박스가 동시에 체크된 경우는 흔치 않으니 순서대로 처리한다.
			if (propagateRequests.isNotEmpty()) {
				askPropagate(propagateRequests, db)
			} else {
				dismiss()
			}
		}
	}

	private fun askPropagate(texts: List<String>, db: BibleDatabase) {
		val text = texts.first()
		val remaining = texts.drop(1)

		if (wordText.isBlank()) {
			if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
			return
		}

		lifecycleScope.launch {
			val matches = db.bibleDao().findVersesContainingExact(translation, wordText)
				.filter { !(it.bookId == bookId && it.chapter == chapter && it.verse == verse) }

			if (matches.isEmpty()) {
				android.widget.Toast.makeText(
					requireContext(),
					"\"$wordText\"가 나오는 다른 구절은 못 찾았어요",
					android.widget.Toast.LENGTH_SHORT
				).show()
				if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
				return@launch
			}

			MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("다른 구절에도 추가")
				.setMessage("\"$wordText\"가 나오는 ${matches.size}개 구절에 이 메모를 추가할까요?")
				.setPositiveButton("추가") { _, _ ->
					lifecycleScope.launch {
						val originLabel = "${BibleBooks.shortNameOf(bookId)} $chapter:$verse"
						val propagatedText = "$text (from $originLabel)"
						for (verseRow in matches) {
							val idx = verseRow.text.indexOf(wordText)
							if (idx == -1) continue
							db.wordMemoDao().insert(
								WordMemo(
									translation = translation,
									bookId = verseRow.bookId,
									chapter = verseRow.chapter,
									verse = verseRow.verse,
									startOffset = idx,
									endOffset = idx + wordText.length,
									text = propagatedText
								)
							)
						}
						android.widget.Toast.makeText(
							requireContext(),
							"${matches.size}개 구절에도 추가됐어요",
							android.widget.Toast.LENGTH_SHORT
						).show()
						if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
					}
				}
				.setNegativeButton("추가 안 함") { _, _ ->
					if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
				}
				.setOnCancelListener {
					if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
				}
				.show()
		}
	}
}