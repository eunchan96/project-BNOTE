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

/** 단어 메모 편집 바텀시트. PWA(VerseList.tsx의 handleWordMemoAction + WordMemoSheet.tsx)와 같은
 * 구조로 만들었다 — 헤더(제목 + 메모 추가 "+"), 선택한 단어 미리보기, 메모 박스 여러 개(각각
 * "다른 구절에도 추가" 체크박스 + 삭제 버튼), 맨 아래 저장 버튼 하나로 바뀐 박스들을 한꺼번에
 * 저장한다.
 *
 * 드래그해서 고른 범위와 정확히 같은 위치의 기존 메모는 그대로 편집 가능한 박스(primary)로,
 * 범위는 겹치지만 위치가 다른 기존 메모는 "선택한 부분과 겹치는 기존 메모"로 따로 묶어서
 * 접힌 채로 보여준다(제목을 누르면 펼쳐짐) — PWA의 VerseList.tsx handleWordMemoAction에 있는
 * 겹침 판정 로직(!(end <= m.startOffset || start >= m.endOffset))을 그대로 옮겼다. */
class WordMemoEditorBottomSheet : FixedBottomSheetDialogFragment() {

	var translation: String = "NKRV"
	var bookId: Int = 1
	var chapter: Int = 1
	var verse: Int = 1
	var startOffset: Int = 0
	var endOffset: Int = 0
	var segment: Int = 0

	var onChanged: (() -> Unit)? = null

	private enum class BoxKind { PRIMARY, OVERLAPPING }

	private class Box(
		var existing: WordMemo?,
		val root: View,
		val editText: EditText,
		val checkbox: CheckBox,
		val kind: BoxKind,
		val start: Int,
		val end: Int,
		val selectedText: String
	)

	/** 겹치는 메모 하나의 그룹(위치가 같은 메모들 묶음). 토글 헤더 + 그 아래 박스들을 같이 관리한다. */
	private class Group(
		val header: TextView,
		val boxesContainer: LinearLayout,
		val boxes: MutableList<Box> = mutableListOf()
	)

	/** 전파 요청 하나. PWA와 동일하게, 어느 박스를 체크했는지에 따라 "다른 구절에서 찾을 단어"가
	 * 달라진다(항상 사용자가 지금 드래그한 단어가 아니라, 체크한 그 박스 자신의 범위 텍스트). */
	private data class PropagateRequest(val text: String, val selectedText: String)

	private var verseText = ""
	private var wordText = ""
	private lateinit var container: LinearLayout
	private val boxes = mutableListOf<Box>()
	private val groups = mutableListOf<Group>()
	private var isSaving = false

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_word_memo_editor, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		container = view.findViewById(R.id.container_memo_boxes)
		view.findViewById<ImageView>(R.id.btn_add_memo_box).setOnClickListener {
			addPrimaryBox(null, startOffset, endOffset)
		}
		view.findViewById<TextView>(R.id.btn_save_memo).setOnClickListener { saveAll() }

		loadExisting(view)
	}

	private fun loadExisting(view: View) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val verseRow =
				db.bibleDao().getVerses(translation, bookId, chapter).find { it.verse == verse }
			verseText = if (segment == 1) verseRow?.text2 ?: "" else verseRow?.text ?: ""
			val safeStart = startOffset.coerceIn(0, verseText.length)
			val safeEnd = endOffset.coerceIn(safeStart, verseText.length)
			wordText = verseText.substring(safeStart, safeEnd)
			view.findViewById<TextView>(R.id.text_selected_word).text = wordText

			// 이 절(+같은 함께보기 단)의 단어 메모를 전부 가져와서, 지금 선택한 범위와 겹치는
			// 것만 골라낸다. PWA의 겹침 판정과 동일: !(end <= m.startOffset || start >= m.endOffset)
			val allMemos = db.wordMemoDao()
				.getForVerseSegment(translation, bookId, chapter, verse, segment)
			val overlapping = allMemos.filter {
				!(endOffset <= it.startOffset || startOffset >= it.endOffset)
			}
			val exactMatches =
				overlapping.filter { it.startOffset == startOffset && it.endOffset == endOffset }
			val restOverlapping = overlapping
				.filterNot { it.startOffset == startOffset && it.endOffset == endOffset }
				.sortedWith(compareBy({ it.startOffset }, { it.endOffset }))

			if (exactMatches.isEmpty()) {
				addPrimaryBox(null, startOffset, endOffset)
			} else {
				exactMatches.forEach { addPrimaryBox(it, it.startOffset, it.endOffset) }
			}

			if (restOverlapping.isNotEmpty()) {
				addSeparatorLabel("선택한 부분과 겹치는 기존 메모")
				val grouped = restOverlapping.groupBy { it.startOffset to it.endOffset }
				for ((key, memosInGroup) in grouped) {
					val groupText = safeSubstring(verseText, key.first, key.second)
					addOverlappingGroup(groupText, memosInGroup)
				}
			}
		}
	}

	private fun safeSubstring(text: String, start: Int, end: Int): String {
		val safeStart = start.coerceIn(0, text.length)
		val safeEnd = end.coerceIn(safeStart, text.length)
		return text.substring(safeStart, safeEnd)
	}

	private fun addSeparatorLabel(label: String) {
		val header = TextView(requireContext()).apply {
			text = label
			textSize = 13f
			setTypeface(typeface, android.graphics.Typeface.BOLD)
			setTextColor(
				androidx.core.content.ContextCompat.getColor(
					requireContext(),
					R.color.brown_primary
				)
			)
			setPadding(0, dp(4), 0, dp(8))
		}
		container.addView(header)
	}

	private fun addPrimaryBox(existing: WordMemo?, start: Int, end: Int) {
		val selectedText = safeSubstring(verseText, start, end)
		val box = buildBoxView(existing, BoxKind.PRIMARY, start, end, selectedText)
		boxes.add(box)
		container.addView(box.root)
		box.root.findViewById<ImageView>(R.id.btn_delete_box)
			.setOnClickListener { removeBox(box, group = null) }
	}

	/** 위치가 같은(=하나의 그룹으로 묶이는) 겹치는 메모들. 토글 헤더를 누르면 펼쳐지고,
	 * 기본은 접힌 채로 시작한다(PWA와 동일). */
	private fun addOverlappingGroup(selectedText: String, memos: List<WordMemo>) {
		val boxesContainer = LinearLayout(requireContext()).apply {
			orientation = LinearLayout.VERTICAL
			visibility = View.GONE
		}

		val header = TextView(requireContext()).apply {
			text = "▸ $selectedText"
			textSize = 14f
			setTextColor(
				androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary)
			)
			setBackgroundColor(
				androidx.core.content.ContextCompat.getColor(
					requireContext(),
					R.color.surface_background
				)
			)
			setPadding(dp(10), dp(10), dp(10), dp(10))
			isClickable = true
			isFocusable = true
		}

		val group = Group(header, boxesContainer)
		groups.add(group)

		header.setOnClickListener {
			val collapsed = boxesContainer.visibility != View.VISIBLE
			boxesContainer.visibility = if (collapsed) View.VISIBLE else View.GONE
			header.text = (if (collapsed) "▾ " else "▸ ") + selectedText
		}

		container.addView(header)
		container.addView(boxesContainer)

		for (memo in memos) {
			val box = buildBoxView(
				memo,
				BoxKind.OVERLAPPING,
				memo.startOffset,
				memo.endOffset,
				selectedText
			)
			boxes.add(box)
			group.boxes.add(box)
			boxesContainer.addView(box.root)
			box.root.findViewById<ImageView>(R.id.btn_delete_box)
				.setOnClickListener { removeBox(box, group) }
		}
	}

	private fun buildBoxView(
		existing: WordMemo?,
		kind: BoxKind,
		start: Int,
		end: Int,
		selectedText: String
	): Box {
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

		return Box(existing, boxView, editText, checkbox, kind, start, end, selectedText)
	}

	private fun removeBox(box: Box, group: Group?) {
		val existing = box.existing

		fun finishRemoval() {
			boxes.remove(box)
			(box.root.parent as? ViewGroup)?.removeView(box.root)
			if (group != null) {
				group.boxes.remove(box)
				if (group.boxes.isEmpty()) {
					container.removeView(group.header)
					container.removeView(group.boxesContainer)
					groups.remove(group)
				}
			}
			// primary 박스가 하나도 안 남으면(지금 고른 범위를 편집할 자리가 없어지므로) 빈 박스를
			// 다시 하나 만들어둔다.
			if (boxes.none { it.kind == BoxKind.PRIMARY }) {
				addPrimaryBox(null, startOffset, endOffset)
			}
		}

		if (existing == null) {
			finishRemoval()
			return
		}
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("메모 삭제")
			.setMessage("이 메모를 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					db.wordMemoDao().delete(existing)
					finishRemoval()
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
			val propagateRequests = mutableListOf<PropagateRequest>()

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
							startOffset = box.start,
							endOffset = box.end,
							segment = segment,
							text = text
						)
					)
					box.existing = db.wordMemoDao().getById(newId)
				}
				if (box.checkbox.isChecked) {
					propagateRequests.add(PropagateRequest(text, box.selectedText))
				}
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

	private fun askPropagate(requests: List<PropagateRequest>, db: BibleDatabase) {
		val request = requests.first()
		val remaining = requests.drop(1)

		if (request.selectedText.isBlank()) {
			if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
			return
		}

		lifecycleScope.launch {
			val matches = db.bibleDao().findVersesContainingExact(translation, request.selectedText)
				.filter { !(it.bookId == bookId && it.chapter == chapter && it.verse == verse) }

			if (matches.isEmpty()) {
				android.widget.Toast.makeText(
					requireContext(),
					"\"${request.selectedText}\"가 나오는 다른 구절은 못 찾았어요",
					android.widget.Toast.LENGTH_SHORT
				).show()
				if (remaining.isNotEmpty()) askPropagate(remaining, db) else dismiss()
				return@launch
			}

			MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("다른 구절에도 추가")
				.setMessage("\"${request.selectedText}\"가 나오는 ${matches.size}개 구절에 이 메모를 추가할까요?")
				.setPositiveButton("추가") { _, _ ->
					lifecycleScope.launch {
						val originLabel = "${BibleBooks.shortNameOf(bookId)} $chapter:$verse"
						val propagatedText = "${request.text} (from $originLabel)"
						for (verseRow in matches) {
							val idx = verseRow.text.indexOf(request.selectedText)
							if (idx == -1) continue
							db.wordMemoDao().insert(
								WordMemo(
									translation = translation,
									bookId = verseRow.bookId,
									chapter = verseRow.chapter,
									verse = verseRow.verse,
									startOffset = idx,
									endOffset = idx + request.selectedText.length,
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

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}