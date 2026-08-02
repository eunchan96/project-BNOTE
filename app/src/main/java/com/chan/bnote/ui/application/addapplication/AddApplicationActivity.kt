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
import androidx.core.view.doOnPreDraw
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

		// "카테고리" 라벨이 제일 길어서(좁은 화면일수록 더 두드러짐) 그 라벨만 폭이 넓어지는데,
		// "제목"·"본문" 라벨은 그대로라 세 박스의 왼쪽 시작 위치가 안 맞았다. 실제로 그려진 뒤
		// (기기별 폰트 크기 등에 따라 정확한 폭을 알 수 있는 시점에) 제일 넓은 라벨 폭에 나머지
		// 두 라벨을 맞춰서, 아래 입력 박스들의 시작 위치가 항상 같은 줄에 맞도록 한다.
		val labelTitle = findViewById<TextView>(R.id.text_label_title)
		val labelCategory = findViewById<TextView>(R.id.text_label_category)
		val labelRef = findViewById<TextView>(R.id.text_label_ref)
		findViewById<View>(R.id.add_application_root).doOnPreDraw {
			val maxWidth =
				maxOf(labelTitle.width, labelCategory.width, labelRef.width)
			for (label in listOf(labelTitle, labelCategory, labelRef)) {
				if (label.width != maxWidth) {
					label.layoutParams = label.layoutParams.apply { width = maxWidth }
				}
			}
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

		findViewById<ImageView>(R.id.btn_prayer_info).setOnClickListener { showPrayerInfoDialog() }
		findViewById<ImageView>(R.id.btn_obedience_info).setOnClickListener { showObedienceInfoDialog() }

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
		val label = DateUtils.formatDate(selectedDateMillis)
		val spannable = android.text.SpannableString(label)
		spannable.setSpan(
			android.text.style.UnderlineSpan(),
			0, label.length,
			android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		btnPickDate.text = spannable
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
			if (current == null) {
				// 새로 작성한 적용은 어디서 시작했든 그 적용의 상세 화면으로 바로 이동해줘야
				// 하므로 새로 연다.
				ApplicationDetailActivity.start(this@AddApplicationActivity, applicationId)
			}
			// 수정인 경우엔 항상 적용 상세 화면(editLauncher)에서 열렸으므로, 여기서 새 상세
			// 화면을 또 띄우면 뒤로가기 시 상세 화면이 중복으로 쌓인다. RESULT_OK로 finish만
			// 하면 원래 상세 화면이 자기 자신을 다시 불러온다(editLauncher 콜백 참고).
			finish()
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private fun showPrayerInfoDialog() {
		val message = """
			기도의 중요한 네 가지 영역은 다음과 같습니다.

			· 찬양(Adoration) : 하나님을 높이는 것입니다. 하나님의 사랑, 능력과 위엄, 하나님의 놀라운 선물인 예수 그리스도를 인하여 하나님을 찬양하십시오.
			· 회개 (Confession) : 당신이 지은 죄를 하나님 앞에 시인하는 것입니다. 솔직하고 겸손하십시오. 하나님께서는 당신을 잘 알고 계시며, 여전히 사랑하고 계심을 잊지 마십시오.
			· 감사 (Thanksgiving) : 하나님께서 당신에게 주신 모든 것에 대하여, 마음에 들지 않는 일까지도, 하나님께 감사하는 것입니다. 감사하는 삶을 통하여 당신은 하나님의 목표를 더 잘 알게 됩니다.
			· 간구 (Supplication) : 특별한 요청입니다. 먼저 남을 위하여 기도하고, 그 다음에 당신을 위하여 기도하십시오.

			이 네 단어의 영어 첫 글자를 따면 'ACTS(사도행전)'가 됩니다. 이것을 기도의 길잡이로 사용하면 균형 있는 기도의 삶을 유지하는 데 도움이 됩니다.
		""".trimIndent()

		com.google.android.material.dialog.MaterialAlertDialogBuilder(
			this, R.style.ThemeOverlay_BNOTE_Dialog
		)
			.setTitle("기도하기 안내")
			.setMessage(message)
			.setPositiveButton("확인", null)
			.show()
	}

	private fun showObedienceInfoDialog() {
		val message = """
			말씀을 삶에 적용하는 방법으로 4P를 사용할 수 있습니다.

			· Personal(개인적 적용) : 말씀을 다른 사람에게 적용하기 전에 먼저 나 자신에게 적용하는 것입니다. 이 말씀이 나의 삶에 무엇을 말씀하고 있는지 돌아보십시오.
			· Practical(실제적 적용) : 깨달은 말씀을 구체적인 행동으로 옮기는 것입니다. "잘해야겠다"는 막연한 결심보다 내가 실제로 무엇을 할 것인지 정하십시오.
			· Possible(가능한 적용) : 내가 실제로 순종할 수 있는 작고 현실적인 것을 정하는 것입니다. 큰 결심보다는 지금 내가 할 수 있는 작은 순종부터 시작하십시오.
			· Period(기간) : 언제 실천할 것인지 구체적인 시간이나 기간을 정하는 것입니다. "언젠가"가 아니라 오늘 또는 이번 주처럼 실천할 시점을 정하십시오.

			이 네 단어의 첫 글자를 따면 4P(Personal, Practical, Possible, Period)가 됩니다. 4P를 말씀 묵상의 길잡이로 사용하면 말씀을 깨닫는 데서 그치지 않고, 내 삶에 적용하여 작고 구체적인 순종으로 실천하는 데 도움이 됩니다.
		""".trimIndent()

		com.google.android.material.dialog.MaterialAlertDialogBuilder(
			this, R.style.ThemeOverlay_BNOTE_Dialog
		)
			.setTitle("순종하기 안내")
			.setMessage(message)
			.setPositiveButton("확인", null)
			.show()
	}
}