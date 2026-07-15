package com.chan.bnote.ui.sermon

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef
import kotlinx.coroutines.launch
import java.util.Calendar

class AddSermonActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_SERMON_ID = "extra_sermon_id"
		private const val EXTRA_INITIAL_DATE_MILLIS = "extra_initial_date_millis"

		/** 신규 등록용 Intent. */
		fun createIntent(
			context: Context,
			initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		): Intent {
			return Intent(context, AddSermonActivity::class.java).apply {
				putExtra(EXTRA_INITIAL_DATE_MILLIS, initialDateMillis)
			}
		}

		/** 기존 설교 수정용 Intent. */
		fun editIntent(context: Context, sermonId: Long): Intent {
			return Intent(context, AddSermonActivity::class.java).apply {
				putExtra(EXTRA_SERMON_ID, sermonId)
			}
		}
	}

	private var existingSermon: Sermon? = null
	private var selectedDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
	private var selectedCategoryId: Long? = null
	private var selectedPreacherId: Long? = null
	private val bibleRefs = mutableListOf<SermonBibleRef>()

	private lateinit var flexboxRefs: com.google.android.flexbox.FlexboxLayout
	private lateinit var btnPickPreacher: TextView
	private lateinit var btnPickCategory: TextView
	private lateinit var btnDate: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_add_sermon)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_sermon_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val sermonId = intent.getLongExtra(EXTRA_SERMON_ID, -1L)
		val isEditMode = sermonId != -1L

		if (intent.hasExtra(EXTRA_INITIAL_DATE_MILLIS)) {
			selectedDateMillis = intent.getLongExtra(EXTRA_INITIAL_DATE_MILLIS, selectedDateMillis)
		}

		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "설교 기록 수정" else "설교 기록 추가"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val editTitle = findViewById<EditText>(R.id.edit_title)
		val editMemo = findViewById<EditText>(R.id.edit_memo)
		btnDate = findViewById(R.id.btn_pick_date)
		btnPickPreacher = findViewById(R.id.btn_pick_preacher)
		btnPickCategory = findViewById(R.id.btn_pick_category)
		flexboxRefs = findViewById(R.id.flexbox_bible_refs)

		updateDateText()

		btnDate.setOnClickListener {
			val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
			DatePickerDialog(
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

		btnPickPreacher.setOnClickListener {
			val picker = PreacherPickerBottomSheet()
			picker.onPreacherSelected = { preacher ->
				selectedPreacherId = preacher.id
				btnPickPreacher.text = preacher.name
			}
			picker.show(supportFragmentManager, "preacher_picker")
		}

		btnPickCategory.setOnClickListener {
			val picker = CategoryPickerBottomSheet()
			picker.onCategorySelected = { category ->
				selectedCategoryId = category?.id
				btnPickCategory.text = category?.name ?: "카테고리 선택"
			}
			picker.show(supportFragmentManager, "category_picker")
		}

		findViewById<TextView>(R.id.btn_add_bible_ref).setOnClickListener {
			val rangePicker = BibleRangePickerBottomSheet()
			rangePicker.onRangeSelected = { ref ->
				bibleRefs.add(ref)
				renderBibleRefChips()
			}
			rangePicker.show(supportFragmentManager, "bible_range_picker")
		}

		findViewById<TextView>(R.id.btn_save_sermon).setOnClickListener {
			save(editTitle.text.toString().trim(), editMemo.text.toString())
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			if (isEditMode) {
				val sermon = db.sermonDao().getById(sermonId)
				existingSermon = sermon
				if (sermon != null) {
					editTitle.setText(sermon.title)
					editMemo.setText(sermon.memo)
					selectedDateMillis = sermon.sermonDate
					selectedCategoryId = sermon.categoryId
					selectedPreacherId = sermon.preacherId
					updateDateText()

					bibleRefs.addAll(db.sermonBibleRefDao().getBySermon(sermon.id))
					renderBibleRefChips()
				}
			}

			selectedPreacherId?.let { id ->
				db.preacherDao().getById(id)?.let { btnPickPreacher.text = it.name }
			}
			selectedCategoryId?.let { id ->
				db.sermonCategoryDao().getById(id)?.let { btnPickCategory.text = it.name }
			}
		}
	}

	private fun renderBibleRefChips() {
		flexboxRefs.removeAllViews()
		for (ref in bibleRefs) {
			val chip = LayoutInflater.from(this)
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
			Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
			return
		}
		val preacherId = selectedPreacherId
		if (preacherId == null) {
			Toast.makeText(this, "설교자를 선택해주세요", Toast.LENGTH_SHORT).show()
			return
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			val sermonId: Long
			val current = existingSermon
			if (current == null) {
				sermonId = db.sermonDao().insert(
					Sermon(
						title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memo
					)
				)
			} else {
				sermonId = current.id
				db.sermonDao().update(
					current.copy(
						title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memo
					)
				)
				db.sermonBibleRefDao().deleteBySermon(sermonId)
			}

			if (bibleRefs.isNotEmpty()) {
				db.sermonBibleRefDao().insertAll(bibleRefs.map { it.copy(sermonId = sermonId) })
			}

			setResult(Activity.RESULT_OK)
			finish()
		}
	}

	private fun updateDateText() {
		val label = DateUtils.formatDate(selectedDateMillis)
		val spannable = SpannableString(label)
		spannable.setSpan(
			UnderlineSpan(),
			0, label.length,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		btnDate.text = spannable
	}
}