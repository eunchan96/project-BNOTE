package com.chan.bnote.ui.mypage.memorization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.bible.picker.BibleRangePickerBottomSheet
import com.chan.bnote.ui.common.RefBoxStyle
import com.chan.bnote.ui.common.UnsavedChangesDialog
import kotlinx.coroutines.launch

/** 암송 구절의 그룹 · 성경 범위 · 메모를 바꾸는 화면. */
class MemorizationVerseEditActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_VERSE_ID = "extra_verse_id"

		fun createIntent(context: Context, verseId: Long): Intent =
			Intent(context, MemorizationVerseEditActivity::class.java)
				.putExtra(EXTRA_VERSE_ID, verseId)
	}

	private var verse: MemorizationVerse? = null
	private var selectedGroup: MemorizationGroup? = null

	// 지금 화면에 보여주고 있는 범위. 처음엔 불러온 구절 그대로고, 카드를 눌러서 다시 고르면
	// 여기가 바뀐다. 이 화면은 항상 기존 구절을 고치는 용도라 비어 있는 채로 시작하는 일은
	// 거의 없지만, "성경 구절 추가" 버튼은 그 경우(=아직 범위가 없을 때)에만 보여준다.
	private var currentRef: SermonBibleRef? = null
	private var currentVerseText: String = ""

	// 뒤로가기 시 "바뀐 게 있는지" 비교할 기준값(불러온 그대로).
	private var originalGroupId: Long = -1
	private var originalRefSignature: String = ""
	private var originalNote: String = ""

	private lateinit var btnAddRange: TextView
	private lateinit var cardRange: LinearLayout
	private lateinit var textRangeLabel: TextView
	private lateinit var textRangeVerse: TextView
	private lateinit var btnPickGroup: TextView
	private lateinit var noteEdit: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_verse_edit)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_verse_edit_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }
		onBackPressedDispatcher.addCallback(
			this,
			object : androidx.activity.OnBackPressedCallback(true) {
				override fun handleOnBackPressed() {
					handleBackPress()
				}
			})

		btnAddRange = findViewById(R.id.btn_add_range)
		cardRange = findViewById(R.id.card_range)
		textRangeLabel = findViewById(R.id.text_range_label)
		textRangeVerse = findViewById(R.id.text_range_verse)
		btnPickGroup = findViewById(R.id.btn_pick_group)
		noteEdit = findViewById(R.id.edit_verse_note)

		btnAddRange.setOnClickListener { openRangePicker(null) }
		cardRange.setOnClickListener { openRangePicker(currentRef) }
		btnPickGroup.setOnClickListener { openGroupPicker() }
		findViewById<TextView>(R.id.btn_save).setOnClickListener { save() }

		// 이 화면은 항상 기존 구절을 불러오는 용도라 처음엔 currentRef가 null이다 — 불러오기
		// 전까지도 "구절 없음" 스타일(전체 너비 박스)이 한 번 적용되게 해서, 약속의 말씀 편집과
		// 똑같이 항상 올바른 초기 모양으로 시작하게 한다.
		updateRangeDisplay()

		loadVerse()
	}

	private fun loadVerse() {
		val verseId = intent.getLongExtra(EXTRA_VERSE_ID, -1)
		if (verseId == -1L) {
			finish()
			return
		}
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val item = db.memorizationVerseDao().getById(verseId)
			if (item == null) {
				finish()
				return@launch
			}
			verse = item
			noteEdit.setText(item.note)
			originalNote = item.note

			currentRef = SermonBibleRef(
				sermonId = 0,
				startBookId = item.startBookId,
				startChapter = item.startChapter,
				startVerse = item.startVerse,
				endBookId = item.endBookId,
				endChapter = item.endChapter,
				endVerse = item.endVerse
			)
			currentVerseText = item.verseText
			originalRefSignature = refSignature(currentRef)
			updateRangeDisplay()

			val group = db.memorizationVerseDao().getAllGroups().find { it.id == item.groupId }
			selectedGroup = group
			originalGroupId = item.groupId
			btnPickGroup.text = group?.name ?: "미분류"
		}
	}

	/** currentRef 유무에 따라 "성경 구절 추가" 버튼과 범위 카드 중 하나만 보여준다. 약속의
	 * 말씀 편집과 똑같은 스타일(RefBoxStyle)을 그대로 쓴다 — 카드가 있으면 버튼은 목록 아래에
	 * 붙는 작은 링크, 없으면 버튼이 전체 너비 박스가 된다. */
	private fun updateRangeDisplay() {
		val ref = currentRef
		if (ref == null) {
			cardRange.visibility = android.view.View.GONE
			btnAddRange.visibility = android.view.View.VISIBLE
		} else {
			cardRange.visibility = android.view.View.VISIBLE
			btnAddRange.visibility = android.view.View.GONE
			textRangeLabel.text = SermonRefLabel.of(ref)
			textRangeVerse.apply {
				text = currentVerseText
				textSize = AppSettings.getFontSize(this@MemorizationVerseEditActivity).toFloat()
			}
		}
		RefBoxStyle.applyAddButtonStyle(this, btnAddRange, ref != null)
	}

	/** 성경 구절 선택 창을 연다. existing이 있으면(카드를 눌러서 연 경우) 그 범위를 미리
	 * 채워주고, 창 안의 삭제 버튼도 함께 나온다. */
	private fun openRangePicker(existing: SermonBibleRef?) {
		val picker = BibleRangePickerBottomSheet()
		picker.existingRef = existing
		// 암송 구절도 한 구절씩 정확히 고르는 경우가 많아서 기본은 꺼둔다.
		picker.defaultMultiMode = false
		picker.onRangeSelected = { ref ->
			lifecycleScope.launch {
				currentRef = ref
				currentVerseText = buildVerseText(ref)
				updateRangeDisplay()
			}
		}
		if (existing != null) {
			picker.onDeleteRequested = {
				currentRef = null
				currentVerseText = ""
				updateRangeDisplay()
			}
		}
		picker.show(supportFragmentManager, "memorization_verse_range_picker")
	}

	private fun openGroupPicker() {
		val picker = MemorizationGroupPickerBottomSheet()
		picker.onGroupSelected = { group ->
			selectedGroup = group
			btnPickGroup.text = group.name
		}
		picker.show(supportFragmentManager, "memorization_group_picker")
	}

	private fun refSignature(ref: SermonBibleRef?): String {
		ref ?: return ""
		return "${ref.startBookId}:${ref.startChapter}:${ref.startVerse}-${ref.endBookId}:${ref.endChapter}:${ref.endVerse}"
	}

	private fun hasUnsavedContent(): Boolean {
		return selectedGroup?.id != originalGroupId ||
				refSignature(currentRef) != originalRefSignature ||
				noteEdit.text.toString() != originalNote
	}

	private fun handleBackPress() {
		if (!hasUnsavedContent()) {
			finish()
			return
		}
		UnsavedChangesDialog.show(this, onDiscard = { finish() })
	}

	private fun save() {
		val item = verse ?: return
		val group = selectedGroup ?: return
		val ref = currentRef
		if (ref == null) {
			android.widget.Toast.makeText(this, "성경 구절을 골라주세요", android.widget.Toast.LENGTH_SHORT)
				.show()
			return
		}
		val newNote = noteEdit.text.toString()
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val updated = item.copy(
				groupId = group.id,
				startBookId = ref.startBookId,
				startChapter = ref.startChapter,
				startVerse = ref.startVerse,
				endBookId = ref.endBookId,
				endChapter = ref.endChapter,
				endVerse = ref.endVerse,
				verseText = currentVerseText,
				note = newNote
			)
			db.memorizationVerseDao().update(updated)
			finish()
		}
	}

	private suspend fun buildVerseText(ref: SermonBibleRef): String {
		val db = BibleDatabase.getInstance(applicationContext)
		val parts = mutableListOf<String>()
		for (chapter in ref.startChapter..ref.endChapter) {
			val versesInChapter = db.bibleDao().getVerses("NKRV", ref.startBookId, chapter)
			val filtered = versesInChapter.filter { v ->
				when {
					ref.startChapter == ref.endChapter -> v.verse in ref.startVerse..ref.endVerse
					chapter == ref.startChapter -> v.verse >= ref.startVerse
					chapter == ref.endChapter -> v.verse <= ref.endVerse
					else -> true
				}
			}
			parts.addAll(filtered.map { it.text })
		}
		return parts.joinToString("\n")
	}
}

/** SermonBibleRef를 "MemorizationVerse.toDisplayLabel()"과 같은 모양의 문자열로 만든다
 * (범위 선택 직후에는 아직 MemorizationVerse가 아니라 SermonBibleRef뿐이라 따로 뗀 것). */
private object SermonRefLabel {
	fun of(ref: SermonBibleRef): String {
		val bookName = com.chan.bnote.data.bible.BibleBooks.nameOf(ref.startBookId)
		val unit = com.chan.bnote.data.bible.BibleBooks.chapterUnit(ref.startBookId)
		return if (ref.startChapter == ref.endChapter) {
			if (ref.startVerse == ref.endVerse) {
				"$bookName ${ref.startChapter}${unit} ${ref.startVerse}절"
			} else {
				"$bookName ${ref.startChapter}${unit} ${ref.startVerse}~${ref.endVerse}절"
			}
		} else {
			"$bookName ${ref.startChapter}${unit} ${ref.startVerse}절~${ref.endChapter}${unit} ${ref.endVerse}절"
		}
	}
}