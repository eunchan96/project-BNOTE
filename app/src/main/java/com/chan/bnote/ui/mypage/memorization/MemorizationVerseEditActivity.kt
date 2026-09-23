package com.chan.bnote.ui.mypage.memorization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.bible.picker.BibleRangePickerBottomSheet
import kotlinx.coroutines.launch

/** 암송 구절의 성경 범위와 그룹을 바꾸는 화면. 메모는 상세 화면에서 바로 고치므로 여기서는
 * 다루지 않는다. */
class MemorizationVerseEditActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_VERSE_ID = "extra_verse_id"

		fun createIntent(context: Context, verseId: Long): Intent =
			Intent(context, MemorizationVerseEditActivity::class.java)
				.putExtra(EXTRA_VERSE_ID, verseId)
	}

	private var verse: MemorizationVerse? = null
	private var selectedGroup: MemorizationGroup? = null
	private var pendingRef: SermonBibleRef? = null

	private lateinit var btnPickRange: TextView
	private lateinit var btnPickGroup: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_verse_edit)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_verse_edit_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		btnPickRange = findViewById(R.id.btn_pick_range)
		btnPickGroup = findViewById(R.id.btn_pick_group)
		btnPickRange.setOnClickListener { openRangePicker() }
		btnPickGroup.setOnClickListener { openGroupPicker() }
		findViewById<TextView>(R.id.btn_save).setOnClickListener { save() }

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
			btnPickRange.text = item.toDisplayLabel()

			val group = db.memorizationVerseDao().getAllGroups().find { it.id == item.groupId }
			selectedGroup = group
			btnPickGroup.text = group?.name ?: "미분류"
		}
	}

	/** 지금 범위를 미리 채운 채로 다시 골라서 바꾼다. 여기서는 화면 표시만 갱신하고,
	 * 실제 저장은 "저장"을 눌렀을 때 한 번에 한다. */
	private fun openRangePicker() {
		val item = verse ?: return
		val existingRef = pendingRef ?: SermonBibleRef(
			sermonId = 0,
			startBookId = item.startBookId,
			startChapter = item.startChapter,
			startVerse = item.startVerse,
			endBookId = item.endBookId,
			endChapter = item.endChapter,
			endVerse = item.endVerse
		)
		val picker = BibleRangePickerBottomSheet()
		picker.existingRef = existingRef
		picker.onRangeSelected = { ref ->
			pendingRef = ref
			btnPickRange.text = SermonRefLabel.of(ref)
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

	private fun save() {
		val item = verse ?: return
		val group = selectedGroup ?: return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val ref = pendingRef
			val updated = if (ref != null) {
				item.copy(
					groupId = group.id,
					startBookId = ref.startBookId,
					startChapter = ref.startChapter,
					startVerse = ref.startVerse,
					endBookId = ref.endBookId,
					endChapter = ref.endChapter,
					endVerse = ref.endVerse,
					verseText = buildVerseText(ref)
				)
			} else {
				item.copy(groupId = group.id)
			}
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