package com.chan.bnote.ui.application.addapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.application.Application
import com.chan.bnote.data.application.ApplicationBibleRef
import com.chan.bnote.data.application.ApplicationCategory
import com.chan.bnote.data.application.ApplicationSermonLink
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.ui.application.ApplicationDetailActivity
import com.chan.bnote.ui.common.UnsavedChangesDialog
import kotlinx.coroutines.launch

class AddApplicationActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_APPLICATION_ID = "extra_application_id"
		private const val EXTRA_INITIAL_DATE_MILLIS = "extra_initial_date_millis"
		private const val EXTRA_PRELINK_SERMON_ID = "extra_prelink_sermon_id"

		fun createIntent(
			context: Context,
			initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		): Intent {
			return Intent(context, AddApplicationActivity::class.java).apply {
				putExtra(EXTRA_INITIAL_DATE_MILLIS, initialDateMillis)
			}
		}

		fun editIntent(context: Context, applicationId: Long): Intent {
			return Intent(context, AddApplicationActivity::class.java).apply {
				putExtra(EXTRA_APPLICATION_ID, applicationId)
			}
		}

		/** 설교 detail 화면의 "적용하러 가기"에서 호출 — 그 설교가 칩으로 미리 추가된 채로 열린다. */
		fun createIntentForSermon(context: Context, sermonId: Long): Intent {
			return Intent(context, AddApplicationActivity::class.java).apply {
				putExtra(
					EXTRA_INITIAL_DATE_MILLIS,
					DateUtils.normalizeToDayStart(System.currentTimeMillis())
				)
				putExtra(EXTRA_PRELINK_SERMON_ID, sermonId)
			}
		}
	}

	private var isEditMode = false
	private var existingApplication: Application? = null
	private var selectedDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
	private var selectedCategory: ApplicationCategory? = null
	private val bibleRefs = mutableListOf<ApplicationBibleRef>()
	private val linkedSermons = mutableListOf<Sermon>()

	private var originalTitle = ""
	private var originalMeditation = ""
	private var originalPrayer = ""
	private var originalObedience = ""
	private var originalDateMillis = 0L
	private var originalCategoryId: Long? = null
	private var originalRefsSignature = ""
	private var originalSermonIds: List<Long> = emptyList()

	private lateinit var editTitle: EditText
	private lateinit var btnPickDate: TextView
	private lateinit var btnPickCategory: TextView
	private lateinit var flexboxRefs: com.google.android.flexbox.FlexboxLayout
	private lateinit var scrollSermonChips: View
	private lateinit var containerSermonChips: LinearLayout
	private lateinit var editMeditation: EditText
	private lateinit var editPrayer: EditText
	private lateinit var editObedience: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_add_application)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_application_root)) { v, insets ->
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

		val applicationId = intent.getLongExtra(EXTRA_APPLICATION_ID, -1L)
		isEditMode = applicationId != -1L
		val preLinkSermonId = intent.getLongExtra(EXTRA_PRELINK_SERMON_ID, -1L)
		selectedDateMillis = intent.getLongExtra(
			EXTRA_INITIAL_DATE_MILLIS, DateUtils.normalizeToDayStart(System.currentTimeMillis())
		)

		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "적용 수정" else "적용 작성"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }

		editTitle = findViewById(R.id.edit_application_title)
		btnPickDate = findViewById(R.id.btn_pick_date)
		btnPickCategory = findViewById(R.id.btn_pick_application_category)
		flexboxRefs = findViewById(R.id.flexbox_application_bible_refs)
		scrollSermonChips = findViewById(R.id.scroll_sermon_chips)
		containerSermonChips = findViewById(R.id.container_sermon_chips)
		editMeditation = findViewById(R.id.edit_meditation)
		editPrayer = findViewById(R.id.edit_prayer)
		editObedience = findViewById(R.id.edit_obedience)

		updateDateText()
		btnPickDate.setOnClickListener { showDatePicker() }

		btnPickCategory.setOnClickListener {
			val picker = ApplicationCategoryPickerBottomSheet()
			picker.onCategorySelected = { category ->
				selectedCategory = category
				btnPickCategory.text = category?.name ?: "카테고리 선택"
			}
			picker.show(supportFragmentManager, "application_category_picker")
		}

		renderBibleRefBoxes()

		findViewById<TextView>(R.id.btn_add_verse_to_meditation).setOnClickListener {
			val picker = ApplicationBibleRangePickerBottomSheet()
			picker.defaultChapterOnly = selectedCategory?.name == "통독"
			picker.onRangeSelected = { ref -> insertVerseTextIntoMeditation(ref) }
			picker.show(supportFragmentManager, "verse_to_meditation_picker")
		}
		findViewById<TextView>(R.id.btn_add_sermon_to_meditation).setOnClickListener {
			val picker = SermonPickerBottomSheet()
			picker.onSermonSelected = { sermon -> addSermonChip(sermon) }
			picker.show(supportFragmentManager, "sermon_picker_for_application")
		}

		findViewById<TextView>(R.id.btn_save_application).setOnClickListener { save() }

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				handleBackPress()
			}
		})

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			if (isEditMode) {
				val application = db.applicationDao().getById(applicationId)
				existingApplication = application
				if (application != null) {
					editTitle.setText(application.title)
					editMeditation.setText(application.meditationMemo)
					editPrayer.setText(application.prayerMemo)
					editObedience.setText(application.obedienceMemo)
					selectedDateMillis = application.applicationDate
					updateDateText()

					selectedCategory =
						application.categoryId?.let { db.applicationCategoryDao().getById(it) }
					btnPickCategory.text = selectedCategory?.name ?: "카테고리 선택"

					bibleRefs.addAll(db.applicationBibleRefDao().getByApplication(application.id))
					renderBibleRefBoxes()

					val links = db.applicationSermonLinkDao().getByApplication(application.id)
					linkedSermons.addAll(links.mapNotNull { db.sermonDao().getById(it.sermonId) })
					renderSermonChips()

					originalTitle = application.title
					originalMeditation = application.meditationMemo
					originalPrayer = application.prayerMemo
					originalObedience = application.obedienceMemo
					originalDateMillis = application.applicationDate
					originalCategoryId = application.categoryId
					originalRefsSignature = refsSignature(bibleRefs)
					originalSermonIds = linkedSermons.map { it.id }
				}
			} else if (preLinkSermonId != -1L) {
				db.sermonDao().getById(preLinkSermonId)?.let { sermon ->
					linkedSermons.add(sermon)
					renderSermonChips()
				}
			}
		}
	}

	private fun updateDateText() {
		btnPickDate.text = DateUtils.formatDate(selectedDateMillis)
	}

	private fun showDatePicker() {
		val cal = java.util.Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
		android.app.DatePickerDialog(
			this,
			{ _, year, month, day ->
				val picked = java.util.Calendar.getInstance()
				picked.set(year, month, day, 0, 0, 0)
				selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
				updateDateText()
			},
			cal.get(java.util.Calendar.YEAR),
			cal.get(java.util.Calendar.MONTH),
			cal.get(java.util.Calendar.DAY_OF_MONTH)
		).show()
	}

	// ---- 본문(성경 구절) ----

	private fun renderBibleRefBoxes() {
		flexboxRefs.removeAllViews()

		if (bibleRefs.isEmpty()) {
			flexboxRefs.addView(buildRefBox("본문 선택", fullWidth = true) {
				openBibleRangePicker(
					existing = null
				)
			})
			return
		}

		for (ref in bibleRefs) {
			flexboxRefs.addView(
				buildRefBox(
					ref.toDisplayLabel(),
					fullWidth = false
				) { openBibleRangePicker(existing = ref) }
			)
		}
		val addButton = buildAddSquareButton { openBibleRangePicker(existing = null) }
		flexboxRefs.addView(addButton)

		flexboxRefs.post {
			val refBox = flexboxRefs.getChildAt(0)
			val height = refBox?.height ?: 0
			if (height > 0) {
				val lp =
					addButton.layoutParams as com.google.android.flexbox.FlexboxLayout.LayoutParams
				lp.height = height
				lp.width = height
				addButton.layoutParams = lp
			}
		}
	}

	private fun openBibleRangePicker(existing: ApplicationBibleRef?) {
		val rangePicker = ApplicationBibleRangePickerBottomSheet()
		rangePicker.existingRef = existing
		rangePicker.defaultChapterOnly = existing?.isChapterOnly ?: (selectedCategory?.name == "통독")
		rangePicker.onRangeSelected = { ref ->
			if (existing != null) {
				val index = bibleRefs.indexOf(existing)
				if (index != -1) bibleRefs[index] = ref else bibleRefs.add(ref)
			} else {
				bibleRefs.add(ref)
			}
			renderBibleRefBoxes()
		}
		rangePicker.onDeleteRequested = {
			bibleRefs.remove(existing)
			renderBibleRefBoxes()
		}
		rangePicker.show(supportFragmentManager, "application_bible_range_picker")
	}

	private fun buildRefBox(text: String, fullWidth: Boolean, onClick: () -> Unit): View {
		return TextView(this).apply {
			this.text = text
			textSize = 15f
			gravity = Gravity.START or Gravity.CENTER_VERTICAL
			maxLines = 1
			ellipsize = android.text.TextUtils.TruncateAt.END
			setPadding(dp(12), dp(8), dp(12), dp(8))
			setTextColor(ContextCompat.getColor(this@AddApplicationActivity, R.color.text_primary))
			background =
				ContextCompat.getDrawable(this@AddApplicationActivity, R.drawable.bg_book_button)
			isClickable = true
			isFocusable = true
			layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
				if (fullWidth) ViewGroup.LayoutParams.MATCH_PARENT else 0,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				flexGrow = 1f
				marginEnd = dp(4)
				bottomMargin = dp(4)
			}
			setOnClickListener { onClick() }
		}
	}

	private fun buildAddSquareButton(onClick: () -> Unit): View {
		return TextView(this).apply {
			text = "+"
			textSize = 18f
			gravity = Gravity.CENTER
			setTextColor(ContextCompat.getColor(this@AddApplicationActivity, R.color.brown_primary))
			background =
				ContextCompat.getDrawable(this@AddApplicationActivity, R.drawable.bg_book_button)
			isClickable = true
			isFocusable = true
			layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply { bottomMargin = dp(4) }
			setOnClickListener { onClick() }
		}
	}

	// ---- 묵상하기: 말씀 추가(본문 텍스트 삽입) ----

	private fun insertVerseTextIntoMeditation(ref: ApplicationBibleRef) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val translation = AppSettings.getPrimaryTranslation(this@AddApplicationActivity)
			val text = fetchRefText(ref, db, translation)
			val current = editMeditation.text?.toString().orEmpty()
			editMeditation.setText(if (current.isBlank()) text else "$current\n\n$text")
			editMeditation.setSelection(editMeditation.text?.length ?: 0)
		}
	}

	private suspend fun fetchRefText(
		ref: ApplicationBibleRef, db: BibleDatabase, translation: String
	): String {
		val unit = BibleBooks.chapterUnit(ref.startBookId)
		if (ref.isChapterOnly) {
			val parts = (ref.startChapter..ref.endChapter).map { chapter ->
				val verses = db.bibleDao().getVerses(translation, ref.startBookId, chapter)
				"${BibleBooks.nameOf(ref.startBookId)} ${chapter}${unit}\n" +
						verses.joinToString("\n") { "${it.verse}. ${it.text}" }
			}
			return parts.joinToString("\n\n")
		}
		val parts = (ref.startChapter..ref.endChapter).map { chapter ->
			val verses = db.bibleDao().getVerses(translation, ref.startBookId, chapter)
			verses.filter { v ->
				val afterStart = chapter > ref.startChapter || v.verse >= ref.startVerse
				val beforeEnd = chapter < ref.endChapter || v.verse <= ref.endVerse
				afterStart && beforeEnd
			}.joinToString("\n") { "${it.verse}. ${it.text}" }
		}
		return "${BibleBooks.nameOf(ref.startBookId)} ${ref.startChapter}${unit}\n" + parts.joinToString(
			"\n"
		)
	}

	// ---- 묵상하기: 설교 추가(칩) ----

	private fun addSermonChip(sermon: Sermon) {
		if (linkedSermons.any { it.id == sermon.id }) return
		linkedSermons.add(sermon)
		renderSermonChips()
	}

	private fun renderSermonChips() {
		containerSermonChips.removeAllViews()
		scrollSermonChips.visibility = if (linkedSermons.isEmpty()) View.GONE else View.VISIBLE

		for (sermon in linkedSermons) {
			val chip = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
				background = ContextCompat.getDrawable(
					this@AddApplicationActivity,
					R.drawable.bg_book_button
				)
				setPadding(dp(10), dp(6), dp(6), dp(6))
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { marginEnd = dp(6); bottomMargin = dp(4) }
			}
			val label = TextView(this).apply {
				text = sermon.title
				textSize = 13f
				setTextColor(
					ContextCompat.getColor(
						this@AddApplicationActivity,
						R.color.text_primary
					)
				)
				maxLines = 1
				ellipsize = android.text.TextUtils.TruncateAt.END
			}
			val remove = TextView(this).apply {
				text = "✕"
				textSize = 12f
				setPadding(dp(6), 0, dp(2), 0)
				setTextColor(ContextCompat.getColor(this@AddApplicationActivity, R.color.text_hint))
				isClickable = true
				isFocusable = true
				setOnClickListener {
					linkedSermons.remove(sermon)
					renderSermonChips()
				}
			}
			chip.addView(label)
			chip.addView(remove)
			containerSermonChips.addView(chip)
		}
	}

	// ---- 저장 / 나가기 ----

	private fun refsSignature(refs: List<ApplicationBibleRef>): String =
		refs.joinToString("|") {
			"${it.startBookId}-${it.startChapter}-${it.startVerse}-${it.endBookId}-${it.endChapter}-${it.endVerse}-${it.isChapterOnly}"
		}

	private fun hasUnsavedContent(): Boolean {
		if (!isEditMode) {
			return editTitle.text.toString().isNotBlank() ||
					editMeditation.text.toString().isNotBlank() ||
					editPrayer.text.toString().isNotBlank() ||
					editObedience.text.toString().isNotBlank() ||
					bibleRefs.isNotEmpty() ||
					linkedSermons.isNotEmpty() ||
					selectedCategory != null
		}
		return editTitle.text.toString().trim() != originalTitle ||
				editMeditation.text.toString() != originalMeditation ||
				editPrayer.text.toString() != originalPrayer ||
				editObedience.text.toString() != originalObedience ||
				selectedDateMillis != originalDateMillis ||
				selectedCategory?.id != originalCategoryId ||
				refsSignature(bibleRefs) != originalRefsSignature ||
				linkedSermons.map { it.id } != originalSermonIds
	}

	private fun handleBackPress() {
		if (!hasUnsavedContent()) {
			finish()
			return
		}
		UnsavedChangesDialog.show(
			context = this,
			onSaveAndExit = { save() },
			onDiscard = { finish() }
		)
	}

	private fun save() {
		val title = editTitle.text.toString().trim()

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val meditation = editMeditation.text.toString()
			val prayer = editPrayer.text.toString()
			val obedience = editObedience.text.toString()

			val applicationId: Long
			val current = existingApplication
			if (current == null) {
				applicationId = db.applicationDao().insert(
					Application(
						title = title,
						categoryId = selectedCategory?.id,
						applicationDate = selectedDateMillis,
						meditationMemo = meditation,
						prayerMemo = prayer,
						obedienceMemo = obedience
					)
				)
			} else {
				applicationId = current.id
				db.applicationDao().update(
					current.copy(
						title = title,
						categoryId = selectedCategory?.id,
						applicationDate = selectedDateMillis,
						meditationMemo = meditation,
						prayerMemo = prayer,
						obedienceMemo = obedience
					)
				)
				db.applicationBibleRefDao().deleteByApplication(applicationId)
				db.applicationSermonLinkDao().deleteByApplication(applicationId)
			}

			if (bibleRefs.isNotEmpty()) {
				db.applicationBibleRefDao()
					.insertAll(bibleRefs.map { it.copy(applicationId = applicationId) })
			}
			for (sermon in linkedSermons) {
				db.applicationSermonLinkDao().insert(
					ApplicationSermonLink(applicationId = applicationId, sermonId = sermon.id)
				)
			}

			setResult(RESULT_OK)
			ApplicationDetailActivity.start(this@AddApplicationActivity, applicationId)
			finish()
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}