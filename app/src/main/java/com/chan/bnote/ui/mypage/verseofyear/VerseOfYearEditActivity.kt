package com.chan.bnote.ui.mypage.verseofyear

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.mypage.verseofyear.VerseOfYear
import com.chan.bnote.data.mypage.verseofyear.VerseOfYearRef
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.bible.picker.BibleRangePickerBottomSheet
import com.chan.bnote.ui.mypage.memorization.MemorizationVerseListActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Calendar

class VerseOfYearEditActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_YEAR = "extra_year"

		/** 신규 추가용 Intent. */
		fun addIntent(context: Context): Intent =
			Intent(context, VerseOfYearEditActivity::class.java)

		/** 기존 연도 수정용 Intent. */
		fun editIntent(context: Context, year: Int): Intent {
			return Intent(context, VerseOfYearEditActivity::class.java).apply {
				putExtra(EXTRA_YEAR, year)
			}
		}
	}

	private var isEditMode = false
	private var editingYear = 0
	private var selectedYear = Calendar.getInstance().get(Calendar.YEAR)

	// 이번 화면에서 추가한 성경 범위들 (bookId/chapter/verse ~ end, verseText 포함해서 함께 들고 있음)
	private val bibleRefs = mutableListOf<Pair<SermonBibleRef, String>>()

	private lateinit var btnPickYear: TextView
	private lateinit var refsContainer: android.widget.LinearLayout
	private lateinit var noteEdit: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_verse_of_year_edit)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verse_of_year_edit_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			v.setPadding(
				systemBars.left,
				systemBars.top,
				systemBars.right,
				maxOf(systemBars.bottom, ime.bottom)
			)
			insets
		}

		isEditMode = intent.hasExtra(EXTRA_YEAR)
		editingYear = intent.getIntExtra(EXTRA_YEAR, selectedYear)

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "${editingYear}년 말씀 수정" else "약속의 말씀 추가"

		btnPickYear = findViewById(R.id.btn_pick_year)
		refsContainer = findViewById(R.id.container_bible_refs)
		noteEdit = findViewById(R.id.edit_verse_note)

		// 연도는 신규 작성이든 수정이든 항상 눌러서 바꿀 수 있다(수정 중에도 연도 자체를
		// 옮길 수 있어야 하므로, "수정 모드엔 고정 텍스트만 보여준다"던 예전 분기를 없앴다).
		selectedYear = if (isEditMode) editingYear else Calendar.getInstance().get(Calendar.YEAR)
		btnPickYear.text = "${selectedYear}년"
		btnPickYear.setOnClickListener { showYearPicker() }

		findViewById<TextView>(R.id.btn_add_bible_ref).setOnClickListener {
			val rangePicker = BibleRangePickerBottomSheet()
			rangePicker.onRangeSelected = { ref ->
				lifecycleScope.launch {
					val verseText = buildVerseText(ref)
					bibleRefs.add(ref to verseText)
					renderBibleRefChips()
				}
			}
			rangePicker.show(supportFragmentManager, "verse_of_year_range_picker")
		}

		findViewById<TextView>(R.id.btn_save_verse_of_year).setOnClickListener { save() }
		findViewById<TextView>(R.id.btn_go_memorize).setOnClickListener { saveAndGoToMemorize() }

		if (isEditMode) {
			loadExisting()
		}
	}

	private fun showYearPicker() {
		val picker = NumberPicker(this).apply {
			minValue = selectedYear - 100
			maxValue = selectedYear + 20
			value = selectedYear
		}
		val container = android.widget.FrameLayout(this).apply {
			setPadding(dp(24), dp(8), dp(24), dp(8))
			addView(
				picker,
				android.widget.FrameLayout.LayoutParams(
					android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
					android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
					android.view.Gravity.CENTER
				)
			)
		}
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("연도 선택")
			.setView(container)
			.setPositiveButton("확인") { _, _ ->
				selectedYear = picker.value
				btnPickYear.text = "${selectedYear}년"
			}
			.setNegativeButton("취소", null)
			.show()
	}

	/** 성경 구절 선택 창을 연다. existing이 있으면(카드를 눌러서 연 경우) 그 범위를 미리
	 * 채워주고, 창 안의 삭제 버튼도 함께 나온다("+ 성경 구절 추가"로 새로 열 때는 null). */
	private fun openBibleRefPicker(existing: SermonBibleRef?) {
		val rangePicker = BibleRangePickerBottomSheet()
		rangePicker.existingRef = existing
		rangePicker.onRangeSelected = { ref ->
			lifecycleScope.launch {
				val verseText = buildVerseText(ref)
				if (existing != null) {
					val index = bibleRefs.indexOfFirst { it.first === existing }
					if (index >= 0) bibleRefs[index] = ref to verseText
				} else {
					bibleRefs.add(ref to verseText)
				}
				renderBibleRefChips()
			}
		}
		if (existing != null) {
			rangePicker.onDeleteRequested = {
				bibleRefs.removeAll { it.first === existing }
				renderBibleRefChips()
			}
		}
		rangePicker.show(supportFragmentManager, "verse_of_year_range_picker")
	}

	private fun renderBibleRefChips() {
		refsContainer.removeAllViews()
		for ((ref, verseText) in bibleRefs) {
			val card = LayoutInflater.from(this)
				.inflate(R.layout.item_verse_of_year_ref_card, refsContainer, false)
			card.findViewById<TextView>(R.id.text_ref_label).text = ref.toDisplayLabel()
			card.findViewById<TextView>(R.id.text_ref_verse_text).apply {
				text = verseText
				textSize = AppSettings.getFontSize(this@VerseOfYearEditActivity).toFloat()
			}
			card.setOnClickListener { openBibleRefPicker(ref) }
			refsContainer.addView(card)
		}

		// 구절이 하나도 없을 땐 "+" 버튼을 눈에 띄게(전체 너비) 두고, 하나라도 있으면 작게 줄인다.
		val addBtn = findViewById<TextView>(R.id.btn_add_bible_ref)
		val params = addBtn.layoutParams as android.widget.LinearLayout.LayoutParams
		if (bibleRefs.isEmpty()) {
			params.width = android.widget.LinearLayout.LayoutParams.MATCH_PARENT
			params.gravity = android.view.Gravity.NO_GRAVITY
			addBtn.layoutParams = params
			addBtn.setPadding(dp(14), dp(14), dp(14), dp(14))
			addBtn.textSize = 15f
		} else {
			params.width = android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
			params.gravity = android.view.Gravity.END
			addBtn.layoutParams = params
			addBtn.setPadding(dp(10), dp(8), dp(10), dp(8))
			addBtn.textSize = 13f
		}
	}

	private suspend fun buildVerseText(ref: SermonBibleRef): String {
		val db = BibleDatabase.getInstance(applicationContext)
		val parts = mutableListOf<String>()
		for (chapter in ref.startChapter..ref.endChapter) {
			val verses = db.bibleDao().getVerses("NKRV", ref.startBookId, chapter)
			val filtered = verses.filter { v ->
				when {
					ref.startChapter == ref.endChapter -> v.verse in ref.startVerse..ref.endVerse
					chapter == ref.startChapter -> v.verse >= ref.startVerse
					chapter == ref.endChapter -> v.verse <= ref.endVerse
					else -> true
				}
			}
			parts.addAll(filtered.map { it.text })
		}
		// 구절이 여러 개면 구절 단위로 줄바꿈해서 저장한다 (단일 구절이면 그냥 한 줄).
		return parts.joinToString("\n")
	}

	private fun loadExisting() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val existing = db.verseOfYearDao().getByYear(editingYear) ?: return@launch
			noteEdit.setText(existing.note)

			val refs = db.verseOfYearRefDao().getByYear(editingYear)
			bibleRefs.clear()
			for (r in refs) {
				val sermonRef = SermonBibleRef(
					sermonId = 0,
					startBookId = r.startBookId,
					startChapter = r.startChapter,
					startVerse = r.startVerse,
					endBookId = r.endBookId,
					endChapter = r.endChapter,
					endVerse = r.endVerse
				)
				bibleRefs.add(sermonRef to r.verseText)
			}
			renderBibleRefChips()
		}
	}

	private fun save() {
		if (bibleRefs.isEmpty()) {
			Toast.makeText(this, "말씀을 하나 이상 추가해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			val year = persistEntry() ?: return@launch
			Toast.makeText(this@VerseOfYearEditActivity, "저장됐어요", Toast.LENGTH_SHORT).show()
			finish()
		}
	}

	/** 저장 후 곧바로 이 구절들을 암송 구절 리스트에 추가하고 그 화면으로 이동한다. */
	private fun saveAndGoToMemorize() {
		if (bibleRefs.isEmpty()) {
			Toast.makeText(this, "말씀을 하나 이상 추가해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			persistEntry() ?: return@launch
			val db = BibleDatabase.getInstance(applicationContext)
			val defaultGroupId = db.memorizationVerseDao().getAllGroups().first().id
			for ((ref, text) in bibleRefs) {
				val alreadyExists = db.memorizationVerseDao().existsCount(
					ref.startBookId, ref.startChapter, ref.startVerse,
					ref.endBookId, ref.endChapter, ref.endVerse
				) > 0
				if (!alreadyExists) {
					db.memorizationVerseDao().insert(
						MemorizationVerse(
							groupId = defaultGroupId,
							startBookId = ref.startBookId,
							startChapter = ref.startChapter,
							startVerse = ref.startVerse,
							endBookId = ref.endBookId,
							endChapter = ref.endChapter,
							endVerse = ref.endVerse,
							verseText = text
						)
					)
				}
			}
			startActivity(
				Intent(
					this@VerseOfYearEditActivity,
					MemorizationVerseListActivity::class.java
				)
			)
			finish()
		}
	}

	/** 연도/구절/메모를 저장한다. 이미 존재하는(그리고 지금 편집 중인 항목이 아닌) 연도면
	 * null을 반환한다(호출부에서 그냥 return). */
	private suspend fun persistEntry(): Int? {
		val year = selectedYear
		val db = BibleDatabase.getInstance(applicationContext)

		// 신규 작성이거나, 수정 중에 연도 자체를 다른 해로 옮긴 경우 — 그 해에 이미 다른 기록이
		// 있는지 확인한다(자기 자신은 제외).
		if (!isEditMode || year != editingYear) {
			val existing = db.verseOfYearDao().getByYear(year)
			if (existing != null) {
				Toast.makeText(
					this@VerseOfYearEditActivity,
					"이미 ${year}년 말씀이 있어요. 목록에서 수정해주세요",
					Toast.LENGTH_SHORT
				).show()
				return null
			}
		}

		// 연도를 옮긴 경우, 예전 연도의 기록은 지우고 새 연도로 다시 써야 한다(연도가
		// VerseOfYear의 기본 키라 그냥 update로는 옮길 수 없다).
		if (isEditMode && year != editingYear) {
			db.verseOfYearRefDao().deleteByYear(editingYear)
			db.verseOfYearDao().delete(editingYear)
		}

		db.verseOfYearDao().upsert(VerseOfYear(year = year, note = noteEdit.text.toString()))
		db.verseOfYearRefDao().deleteByYear(year)
		db.verseOfYearRefDao().insertAll(
			bibleRefs.map { (ref, text) ->
				VerseOfYearRef(
					year = year,
					startBookId = ref.startBookId,
					startChapter = ref.startChapter,
					startVerse = ref.startVerse,
					endBookId = ref.endBookId,
					endChapter = ref.endChapter,
					endVerse = ref.endVerse,
					verseText = text
				)
			}
		)
		// 이 화면에 계속 머무는 동안(자동 저장 등) 다음 저장부터는 지금 이 연도를 "원래 연도"로
		// 본다 — 그래야 다시 저장할 때 방금 옮긴 연도와 또 충돌 처리를 하지 않는다.
		isEditMode = true
		editingYear = year
		return year
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("${editingYear}년 말씀 삭제")
			.setMessage("삭제하면 되돌릴 수 없어요. 계속할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.verseOfYearRefDao().deleteByYear(editingYear)
					db.verseOfYearDao().delete(editingYear)
					Toast.makeText(this@VerseOfYearEditActivity, "삭제됐어요", Toast.LENGTH_SHORT).show()
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}