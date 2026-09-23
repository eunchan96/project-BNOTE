package com.chan.bnote.ui.mypage.verseofyear

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
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.mypage.verseofyear.VerseOfYearRef
import com.chan.bnote.ui.mypage.memorization.MemorizationVerseListActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** 특정 연도의 약속의 말씀을 읽기 전용으로 보여준다("암송 구절" 상세 화면과 같은 가운데 정렬
 * 스타일). 실제로 고치는 작업은 여기서 하지 않고, 상단바 연필 아이콘으로 VerseOfYearEditActivity를 연다. */
class VerseOfYearDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_YEAR = "extra_year"

		fun start(context: Context, year: Int) {
			context.startActivity(createIntent(context, year))
		}

		fun createIntent(context: Context, year: Int): Intent =
			Intent(context, VerseOfYearDetailActivity::class.java).putExtra(EXTRA_YEAR, year)
	}

	private var year: Int = 0
	private var refs: List<VerseOfYearRef> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_verse_of_year_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verse_of_year_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		year = intent.getIntExtra(EXTRA_YEAR, -1)
		if (year == -1) {
			finish()
			return
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.text_top_bar_title).text = "${year}년 약속의 말씀"
		findViewById<TextView>(R.id.btn_go_memorize).setOnClickListener { goToMemorize() }
		findViewById<ImageView>(R.id.btn_edit_entry).setOnClickListener {
			startActivity(VerseOfYearEditActivity.editIntent(this, year))
			finish()
		}
		findViewById<ImageView>(R.id.btn_delete_entry).setOnClickListener { confirmDelete() }
	}

	override fun onResume() {
		super.onResume()
		// 수정 화면에서 고치고 돌아왔을 수도 있으니 매번 다시 불러온다.
		loadEntry()
	}

	private fun loadEntry() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val entry = db.verseOfYearDao().getByYear(year)
			if (entry == null) {
				finish()
				return@launch
			}
			refs = db.verseOfYearRefDao().getByYear(year)

			findViewById<TextView>(R.id.text_verse_ref).text =
				refs.joinToString(", ") { it.toDisplayLabel() }
			findViewById<TextView>(R.id.text_verse_content).text =
				refs.joinToString("\n") { it.verseText }

			val noteView = findViewById<TextView>(R.id.text_verse_note)
			if (entry.note.isNotBlank()) {
				noteView.text = entry.note
				noteView.setTextColor(
					androidx.core.content.ContextCompat.getColor(
						this@VerseOfYearDetailActivity,
						R.color.text_primary
					)
				)
			} else {
				noteView.text = "메모가 없어요"
				noteView.setTextColor(
					androidx.core.content.ContextCompat.getColor(
						this@VerseOfYearDetailActivity,
						R.color.text_hint
					)
				)
			}
		}
	}

	/** 이 연도의 구절들을 암송 구절 리스트(기본 그룹)에 추가하고 그 화면으로 이동한다.
	 * VerseOfYearEditActivity.saveAndGoToMemorize()와 같은 로직 — 이미 저장된 항목을
	 * 보는 화면이라 새로 저장할 내용은 없고, 등록만 하면 된다. */
	private fun goToMemorize() {
		if (refs.isEmpty()) return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val defaultGroupId = db.memorizationVerseDao().getAllGroups().first().id
			for (ref in refs) {
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
							verseText = ref.verseText
						)
					)
				}
			}
			startActivity(
				Intent(this@VerseOfYearDetailActivity, MemorizationVerseListActivity::class.java)
			)
		}
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("${year}년 말씀 삭제")
			.setMessage("삭제하면 되돌릴 수 없어요. 계속할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.verseOfYearRefDao().deleteByYear(year)
					db.verseOfYearDao().delete(year)
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}
}