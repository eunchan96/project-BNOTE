package com.chan.bnote.ui.mypage.gratitude

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.mypage.gratitude.GratitudeEntry
import com.chan.bnote.data.mypage.gratitude.GratitudeNote
import com.chan.bnote.ui.common.KeyboardBar
import com.chan.bnote.ui.common.UnsavedChangesDialog
import kotlinx.coroutines.launch
import java.util.Calendar

class AddGratitudeActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NOTE_ID = "extra_note_id"
		private const val EXTRA_INITIAL_DATE_MILLIS = "extra_initial_date_millis"
		private const val DEFAULT_ENTRY_COUNT = 5

		fun createIntent(
			context: Context,
			initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		): Intent {
			return Intent(context, AddGratitudeActivity::class.java).apply {
				putExtra(EXTRA_INITIAL_DATE_MILLIS, initialDateMillis)
			}
		}

		fun editIntent(context: Context, noteId: Long): Intent {
			return Intent(context, AddGratitudeActivity::class.java).apply {
				putExtra(EXTRA_NOTE_ID, noteId)
			}
		}
	}

	private var isEditMode = false
	private var existingNote: GratitudeNote? = null
	private var selectedDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
	private var originalTexts: List<String> = emptyList()
	private var originalDateMillis: Long = 0L

	private lateinit var btnPickDate: TextView
	private lateinit var containerEntries: LinearLayout
	private lateinit var btnAddEntry: TextView
	private lateinit var btnReorder: TextView

	// "순서 변경" 중인지. 이때는 각 줄의 체크 아이콘이 ≡ 손잡이로 바뀌고, 손잡이를 끌어서 줄을 옮긴다.
	private var isReorderMode = false

	// 지금 끌고 있는 줄과, 직전 손가락 위치(화면 기준 y).
	private var draggingRow: View? = null
	private var lastDragRawY = 0f

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_add_gratitude)

		val keyboardBar = KeyboardBar(this, findViewById(R.id.keyboard_bar))

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_gratitude_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			v.setPadding(
				systemBars.left,
				systemBars.top,
				systemBars.right,
				maxOf(systemBars.bottom, ime.bottom)
			)
			keyboardBar.onInsetsChanged(insets)
			insets
		}

		val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
		isEditMode = noteId != -1L
		selectedDateMillis = intent.getLongExtra(
			EXTRA_INITIAL_DATE_MILLIS, DateUtils.normalizeToDayStart(System.currentTimeMillis())
		)

		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "감사 노트 수정" else "감사 노트 작성"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }

		btnPickDate = findViewById(R.id.btn_pick_date)
		containerEntries = findViewById(R.id.container_gratitude_entries)
		btnAddEntry = findViewById(R.id.btn_add_gratitude_entry)
		btnReorder = findViewById(R.id.btn_reorder_gratitude)

		updateDateText()
		btnPickDate.setOnClickListener { showDatePicker() }

		btnAddEntry.setOnClickListener { addEntryRow("") }
		btnReorder.setOnClickListener { setReorderMode(!isReorderMode) }
		findViewById<TextView>(R.id.btn_save_gratitude).setOnClickListener { save() }

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBackPress()
			}
		})

		if (isEditMode) {
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(applicationContext)
				val note = db.gratitudeNoteDao().getById(noteId)
				existingNote = note
				if (note != null) {
					selectedDateMillis = note.date
					originalDateMillis = note.date
					updateDateText()
					val entries = db.gratitudeEntryDao().getByNote(note.id)
					if (entries.isEmpty()) {
						repeat(DEFAULT_ENTRY_COUNT) { addEntryRow("") }
					} else {
						entries.forEach { addEntryRow(it.text) }
					}
					originalTexts = currentEntryTexts()
				}
			}
		} else {
			repeat(DEFAULT_ENTRY_COUNT) { addEntryRow("") }
		}
	}

	private fun updateDateText() {
		val label = DateUtils.formatDate(selectedDateMillis)
		val spannable = SpannableString(label)
		spannable.setSpan(UnderlineSpan(), 0, label.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
		btnPickDate.text = spannable
	}

	private fun showDatePicker() {
		val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
		android.app.DatePickerDialog(
			this,
			{ _, year, month, day ->
				val picked = Calendar.getInstance()
				picked.set(year, month, day, 0, 0, 0)
				selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
				updateDateText()
			},
			cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
		).show()
	}

	private fun addEntryRow(initialText: String) {
		val row = layoutInflater.inflate(R.layout.item_gratitude_entry_row, containerEntries, false)
		val editText = row.findViewById<EditText>(R.id.edit_gratitude_entry)
		editText.setText(initialText)

		// textMultiLine이라 길게 쓰면 화면 폭에서 자연스럽게 줄바꿈(래핑)되지만, 실제로 개행 문자
		// ("\n")가 들어가는 건 막는다 — 각 칸은 어디까지나 한 줄짜리 항목이어야 한다. 키보드마다
		// 엔터를 처리하는 방식이 조금씩 달라서(어떤 키보드는 그냥 "\n"을 넣어버림), 입력 필터로
		// 한 번 더 확실히 걸러낸다.
		editText.filters = arrayOf(android.text.InputFilter { source, _, _, _, _, _ ->
			if (source.contains("\n")) source.toString().replace("\n", "") else source
		})

		editText.setOnEditorActionListener { view, actionId, event ->
			val isEnterKeyDown = event != null &&
					event.keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
					event.action == android.view.KeyEvent.ACTION_DOWN
			if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT || isEnterKeyDown) {
				focusNextRowAfter(view as EditText)
				true
			} else if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
				view.clearFocus()
				true
			} else {
				false
			}
		}

		// 순서 변경 중일 때만 아이콘(손잡이)을 잡고 끌 수 있다. 평소엔 터치를 그냥 흘려보낸다.
		row.findViewById<ImageView>(R.id.icon_gratitude_entry).setOnTouchListener { _, event ->
			if (isReorderMode) handleDrag(row, event) else false
		}

		containerEntries.addView(row)
		applyRowMode(row)
		updateImeActionsForRows()
	}

	/** 지금 칸 다음에 있는 행의 입력칸으로 포커스를 옮긴다(그 행이 맨 마지막이면 아무 일도 안 함). */
	private fun focusNextRowAfter(current: EditText) {
		for (i in 0 until containerEntries.childCount) {
			val row = containerEntries.getChildAt(i)
			val editText = row.findViewById<EditText>(R.id.edit_gratitude_entry)
			if (editText === current) {
				val nextRow = containerEntries.getChildAt(i + 1) ?: return
				nextRow.findViewById<EditText>(R.id.edit_gratitude_entry).requestFocus()
				return
			}
		}
	}

	/** 키보드의 엔터 자리에 "다음" 버튼이 뜨게 해서, 다음 칸을 직접 안 눌러도 그대로 넘어갈 수
	 * 있게 한다(각 칸이 한 줄짜리라 원래 줄바꿈이 필요 없다). 실제로 다음 칸이 있는 행만 "다음"으로
	 * 보여주고, 맨 마지막 행은 "완료"로 보여준다 — "항목 추가"로 새 행이 생기면 그 직전까지
	 * "완료"였던 행도 다시 "다음"으로 바뀌어야 하므로 매번 전체를 다시 맞춘다. */
	private fun updateImeActionsForRows() {
		val count = containerEntries.childCount
		for (i in 0 until count) {
			val editText =
				containerEntries.getChildAt(i).findViewById<EditText>(R.id.edit_gratitude_entry)
			val isLast = i == count - 1
			editText.imeOptions = if (isLast) {
				android.view.inputmethod.EditorInfo.IME_ACTION_DONE
			} else {
				android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
			}
		}
	}

	private fun currentEntryTexts(): List<String> {
		val texts = mutableListOf<String>()
		for (i in 0 until containerEntries.childCount) {
			val row = containerEntries.getChildAt(i)
			// 입력 필터로 개행을 걸러내고 있지만, 혹시 다른 경로(자동완성 등)로 섞여 들어왔을 수도
			// 있으니 저장 직전에도 한 번 더 확실히 없앤다 — 개행이 남아있으면 캘린더 미리보기의
			// 들여쓰기 계산이 어긋난다.
			val text = row.findViewById<EditText>(R.id.edit_gratitude_entry).text.toString()
				.replace("\n", " ").trim()
			texts.add(text)
		}
		return texts
	}

	private fun hasUnsavedContent(): Boolean {
		val currentNonBlank = currentEntryTexts().filter { it.isNotBlank() }
		if (!isEditMode) {
			return currentNonBlank.isNotEmpty()
		}
		val originalNonBlank = originalTexts.filter { it.isNotBlank() }
		return currentNonBlank != originalNonBlank || selectedDateMillis != originalDateMillis
	}

	// ---- 순서 변경 ----

	/**
	 * "순서 변경"을 켜고 끈다. 화면은 그대로 두고, 각 줄의 체크 아이콘만 ≡ 손잡이로 바뀐다.
	 * 켜져 있는 동안엔 글자를 고치지 않도록 입력칸을 잠그고(모양은 그대로), "항목 추가"도 잠근다.
	 * 저장은 하지 않는다(저장하기를 눌러야 저장).
	 */
	private fun setReorderMode(enabled: Boolean) {
		if (enabled && containerEntries.childCount < 2) return
		isReorderMode = enabled

		if (enabled) {
			// 키보드가 떠 있으면 내려서 줄 전체가 보이게 한다.
			currentFocus?.let { focused ->
				focused.clearFocus()
				(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
					.hideSoftInputFromWindow(focused.windowToken, 0)
			}
		}
		for (i in 0 until containerEntries.childCount) applyRowMode(containerEntries.getChildAt(i))

		btnAddEntry.isEnabled = !enabled
		btnAddEntry.alpha = if (enabled) 0.4f else 1f
		btnReorder.text = if (enabled) "완료" else "순서 변경"
		btnReorder.setTypeface(
			null,
			if (enabled) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
		)
		if (!enabled) updateImeActionsForRows()
	}

	/** 한 줄의 모양을 지금 모드에 맞춘다: 아이콘(체크 ↔ 손잡이)과 입력 잠금. */
	private fun applyRowMode(row: View) {
		row.findViewById<ImageView>(R.id.icon_gratitude_entry).apply {
			setImageResource(if (isReorderMode) R.drawable.ic_drag_handle else R.drawable.ic_check_circle)
			contentDescription = if (isReorderMode) "끌어서 순서 바꾸기" else null
		}
		row.findViewById<EditText>(R.id.edit_gratitude_entry).apply {
			isFocusable = !isReorderMode
			isFocusableInTouchMode = !isReorderMode
		}
	}

	/**
	 * ≡ 손잡이를 잡고 위아래로 끌 때. 끄는 줄은 손가락을 따라 움직이고(translationY), 이웃 줄의 가운데를
	 * 넘어가면 그 이웃 줄을 반대편으로 옮겨서 자리를 바꾼다. 끄는 줄 자체는 떼었다 붙이지 않아서
	 * 손가락 터치가 끊기지 않는다. 손을 떼면 제자리로 부드럽게 내려앉는다.
	 */
	private fun handleDrag(row: View, event: MotionEvent): Boolean {
		when (event.actionMasked) {
			MotionEvent.ACTION_DOWN -> {
				draggingRow = row
				lastDragRawY = event.rawY
				// 끄는 동안 바깥 스크롤뷰가 터치를 가로채서 화면이 스크롤되지 않게 한다.
				row.parent.requestDisallowInterceptTouchEvent(true)
				row.setBackgroundColor(ContextCompat.getColor(this, R.color.surface_elevated))
				row.elevation = 6 * resources.displayMetrics.density
				return true
			}

			MotionEvent.ACTION_MOVE -> {
				val dragging = draggingRow ?: return false
				dragging.translationY += event.rawY - lastDragRawY
				lastDragRawY = event.rawY
				swapWithNeighborIfNeeded(dragging)
				return true
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				val dragging = draggingRow ?: return false
				draggingRow = null
				dragging.animate().translationY(0f).setDuration(150).withEndAction {
					dragging.elevation = 0f
					dragging.background = null
				}.start()
				return true
			}
		}
		return false
	}

	/** 줄 하나가 차지하는 세로 칸(높이 + 위아래 여백). */
	private fun slotHeight(view: View): Int {
		val lp = view.layoutParams as LinearLayout.LayoutParams
		return view.height + lp.topMargin + lp.bottomMargin
	}

	/** [index]번째 줄의 원래 자리 가운데 y(컨테이너 기준). 레이아웃이 아직 안 끝났어도 맞도록 높이를 더해서 구한다. */
	private fun slotCenter(index: Int): Float {
		var top = 0
		for (i in 0 until index) top += slotHeight(containerEntries.getChildAt(i))
		return top + slotHeight(containerEntries.getChildAt(index)) / 2f
	}

	private fun swapWithNeighborIfNeeded(row: View) {
		val index = containerEntries.indexOfChild(row)
		val center = slotCenter(index) + row.translationY

		if (index < containerEntries.childCount - 1 && center > slotCenter(index + 1)) {
			// 아래 줄을 끄는 줄 위로 올리면, 끄는 줄의 원래 자리가 그 줄 높이만큼 내려가므로 그만큼 되돌린다.
			val next = containerEntries.getChildAt(index + 1)
			containerEntries.removeViewAt(index + 1)
			containerEntries.addView(next, index)
			row.translationY -= slotHeight(next)
		} else if (index > 0 && center < slotCenter(index - 1)) {
			val prev = containerEntries.getChildAt(index - 1)
			containerEntries.removeViewAt(index - 1)
			containerEntries.addView(prev, index)
			row.translationY += slotHeight(prev)
		}
	}

	private fun handleBackPress() {
		// 순서 변경 중이면 뒤로가기는 먼저 순서 변경만 끝낸다(바뀐 순서는 그대로 유지).
		if (isReorderMode) {
			setReorderMode(false)
			return
		}
		if (!hasUnsavedContent()) {
			finish()
			return
		}
		UnsavedChangesDialog.show(
			context = this,
			onDiscard = { finish() }
		)
	}

	private fun save() {
		// 순서 변경 중에 바로 저장하기를 눌러도 지금 보이는 순서 그대로 저장된다(줄 자체를 옮겨 두었으므로).
		if (isReorderMode) setReorderMode(false)
		val texts = currentEntryTexts().filter { it.isNotBlank() }

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val current = existingNote

			val noteId: Long = if (current == null) {
				db.gratitudeNoteDao().insert(GratitudeNote(date = selectedDateMillis))
			} else {
				db.gratitudeNoteDao().update(current.copy(date = selectedDateMillis))
				db.gratitudeEntryDao().deleteByNote(current.id)
				current.id
			}

			if (texts.isNotEmpty()) {
				db.gratitudeEntryDao().insertAll(
					texts.mapIndexed { index, text ->
						GratitudeEntry(noteId = noteId, text = text, sortOrder = index)
					}
				)
			}

			setResult(RESULT_OK)
			finish()
		}
	}
}