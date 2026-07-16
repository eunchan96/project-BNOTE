package com.chan.bnote.ui.sermon

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.ui.bible.CitationBubbleHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SermonDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_SERMON_ID = "extra_sermon_id"

		fun start(context: Context, sermonId: Long) {
			context.startActivity(createIntent(context, sermonId))
		}

		fun createIntent(context: Context, sermonId: Long): Intent {
			val intent = Intent(context, SermonDetailActivity::class.java)
			intent.putExtra(EXTRA_SERMON_ID, sermonId)
			return intent
		}
	}

	private var changed = false
	private lateinit var sermon: Sermon

	private val editSermonLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			changed = true
			loadSermon()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_sermon_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sermon_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finishWithResult() }

		findViewById<ImageView>(R.id.btn_edit_sermon).setOnClickListener {
			editSermonLauncher.launch(AddSermonActivity.editIntent(this, sermon.id))
		}

		findViewById<ImageView>(R.id.btn_delete_sermon).setOnClickListener {
			MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("설교 삭제")
				.setMessage("'${sermon.title}'을(를) 삭제할까요?")
				.setPositiveButton("삭제") { _, _ ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.sermonBibleRefDao().deleteBySermon(sermon.id)
						db.sermonDao().delete(sermon)
						changed = true
						finishWithResult()
					}
				}
				.setNegativeButton("취소", null)
				.show()
		}

		onBackPressedDispatcher.addCallback(this) {
			finishWithResult()
		}

		loadSermon()
	}

	private fun finishWithResult() {
		setResult(if (changed) Activity.RESULT_OK else Activity.RESULT_CANCELED)
		finish()
	}

	private fun loadSermon() {
		val sermonId = intent.getLongExtra(EXTRA_SERMON_ID, -1L)
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val loaded = db.sermonDao().getById(sermonId)
			if (loaded == null) {
				finishWithResult()
				return@launch
			}
			sermon = loaded
			render(db)
		}
	}

	private suspend fun render(db: com.chan.bnote.data.BibleDatabase) {
		findViewById<TextView>(R.id.text_top_bar_title).text = sermon.title

		val flexbox =
			findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flexbox_detail_refs)
		val memoView = findViewById<TextView>(R.id.text_sermon_memo)

		// 날짜 · 카테고리 · 설교자
		val preacherName =
			sermon.preacherId?.let { db.preacherDao().getById(it)?.name } ?: "설교자 미지정"
		val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
		val dateLabel = DateUtils.formatDate(sermon.sermonDate)

		val metaView = findViewById<TextView>(R.id.text_sermon_meta)
		if (category != null) {
			/* val prefix = "$dateLabel · "
			val categoryPart = category.name
			val suffix = " · $preacherName"
			val builder = SpannableStringBuilder()
			builder.append(prefix)
			val categoryStart = builder.length
			builder.append(categoryPart)
			builder.setSpan(
				ForegroundColorSpan(Color.parseColor(category.colorHex)),
				categoryStart, builder.length,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
			builder.append(suffix)*/
			metaView.text = "$dateLabel· ${category.name} · $preacherName"
		} else {
			metaView.text = "$dateLabel · $preacherName"
		}

		// 성경 구절 칩 (탭하면 해당 성경 위치로 이동)
		flexbox.removeAllViews()
		val refs = db.sermonBibleRefDao().getBySermon(sermon.id)
		for (ref in refs) {
			val chip = LayoutInflater.from(this)
				.inflate(R.layout.item_bible_ref_chip_compact, flexbox, false)
			chip.findViewById<TextView>(R.id.text_chip_label).text = ref.toDisplayLabel()
			chip.setOnClickListener {
				val intent = Intent(this, MainActivity::class.java).apply {
					putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, ref.startBookId)
					putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, ref.startChapter)
					flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
				}
				startActivity(intent)
			}
			flexbox.addView(chip)
		}

		// 메모 (인용 구절 밑줄 + 롱프레스 말풍선)
		val memoText = sermon.memo.ifBlank { "메모가 없어요" }
		if (sermon.memo.isBlank()) {
			memoView.text = memoText
		} else {
			val (spanned, citations) = CitationBubbleHelper.buildSpannedText(memoText)
			memoView.text = spanned
			CitationBubbleHelper.attachTouchHandling(memoView, { citations }, lifecycleScope)
		}
	}
}