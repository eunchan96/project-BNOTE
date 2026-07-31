package com.chan.bnote.ui.mypage.memorization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.mypage.memorization.VerseMemorizationProgress
import kotlinx.coroutines.launch

class MemorizationPracticeActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_SINGLE_VERSE_ID = "extra_single_verse_id"
		private const val EXTRA_GROUP_ID = "extra_group_id"

		/** 전체 암송 구절을 섞어서 연습하는 일반 모드. */
		fun allVersesIntent(context: Context): Intent =
			Intent(context, MemorizationPracticeActivity::class.java)

		/** 구절 하나만 연습하는 모드 (암송 구절 상세 화면의 "암송하기" 버튼). */
		fun singleVerseIntent(context: Context, verseId: Long): Intent =
			Intent(context, MemorizationPracticeActivity::class.java)
				.putExtra(EXTRA_SINGLE_VERSE_ID, verseId)

		/** 특정 그룹의 구절만 섞어서 연습하는 모드 (그룹 상세 화면의 "연습" 버튼). */
		fun groupIntent(context: Context, groupId: Long): Intent =
			Intent(context, MemorizationPracticeActivity::class.java)
				.putExtra(EXTRA_GROUP_ID, groupId)
	}

	private lateinit var textProgress: TextView
	private lateinit var textEmptyState: TextView
	private lateinit var containerPractice: LinearLayout
	private lateinit var textVerseRef: TextView
	private lateinit var containerVerseCard: LinearLayout
	private lateinit var textVerseContent: TextView
	private lateinit var btnReveal: TextView
	private lateinit var textHint: TextView
	private lateinit var btnHint: TextView
	private lateinit var containerAnswerButtons: LinearLayout
	private lateinit var containerComplete: LinearLayout
	private lateinit var textCompleteSummary: TextView
	private lateinit var btnRestart: TextView
	private lateinit var btnReviewMissed: TextView

	private var verses: List<MemorizationVerse> = emptyList()
	private var currentIndex = 0
	private var memorizedCount = 0
	private var hintLevel = 0
	private var singleVerseId: Long = -1
	private var groupId: Long = -1

	// 이번 연습 회차에서 "다시 볼래요"를 누른 구절들 — 하나라도 있으면 완료 화면에서
	// "다시 연습하기" 대신 "틀린 것 다시 보기"를 보여준다.
	private var missedVerses: MutableList<MemorizationVerse> = mutableListOf()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_practice)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		textProgress = findViewById(R.id.text_progress)
		textEmptyState = findViewById(R.id.text_empty_state)
		containerPractice = findViewById(R.id.container_practice)
		textVerseRef = findViewById(R.id.text_verse_ref)
		containerVerseCard = findViewById(R.id.container_verse_card)
		textVerseContent = findViewById(R.id.text_verse_content)
		btnReveal = findViewById(R.id.btn_reveal)
		textHint = findViewById(R.id.text_hint)
		btnHint = findViewById(R.id.btn_hint)
		containerAnswerButtons = findViewById(R.id.container_answer_buttons)
		containerComplete = findViewById(R.id.container_complete)
		textCompleteSummary = findViewById(R.id.text_complete_summary)
		btnRestart = findViewById(R.id.btn_restart)
		btnReviewMissed = findViewById(R.id.btn_review_missed)

		singleVerseId = intent.getLongExtra(EXTRA_SINGLE_VERSE_ID, -1)
		groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1)

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		btnReveal.setOnClickListener { revealAnswer() }
		btnHint.setOnClickListener { showHint() }
		findViewById<TextView>(R.id.btn_review_again).setOnClickListener { answerCard(memorized = false) }
		findViewById<TextView>(R.id.btn_memorized).setOnClickListener { answerCard(memorized = true) }
		btnRestart.setOnClickListener { startSession(verses) }
		btnReviewMissed.setOnClickListener { startSession(missedVerses) }
		findViewById<TextView>(R.id.btn_quit).setOnClickListener { finish() }

		loadVerses()
	}

	private fun loadVerses() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val refs = when {
				singleVerseId != -1L -> listOfNotNull(
					db.memorizationVerseDao().getById(singleVerseId)
				)

				groupId != -1L -> db.memorizationVerseDao().getByGroup(groupId)
				else -> db.memorizationVerseDao().getAll()
			}

			if (refs.isEmpty()) {
				textEmptyState.visibility = View.VISIBLE
				containerPractice.visibility = View.GONE
			} else {
				textEmptyState.visibility = View.GONE
				containerPractice.visibility = View.VISIBLE
				startSession(refs)
			}
		}
	}

	private fun startSession(refs: List<MemorizationVerse>) {
		// 단일 구절 모드에서는 순서를 섞을 필요가 없다.
		verses = if (singleVerseId != -1L) refs else refs.shuffled()
		currentIndex = 0
		memorizedCount = 0
		missedVerses = mutableListOf()
		showCard(currentIndex)
	}

	private fun showCard(index: Int) {
		containerComplete.visibility = View.GONE
		textVerseRef.visibility = View.VISIBLE
		containerVerseCard.visibility = View.VISIBLE

		val ref = verses[index]
		textProgress.text = "${index + 1} / ${verses.size}"
		textVerseRef.text = ref.toDisplayLabel()

		textVerseContent.text = ref.verseText
		textVerseContent.visibility = View.GONE
		btnReveal.visibility = View.VISIBLE
		containerAnswerButtons.visibility = View.GONE

		hintLevel = 0
		textHint.visibility = View.GONE
		btnHint.visibility = View.VISIBLE
		btnHint.text = "힌트 보기"
	}

	private fun showHint() {
		hintLevel += 1
		val ref = verses[currentIndex]
		textHint.text = buildHintText(ref.verseText, hintLevel)
		textHint.visibility = View.VISIBLE
		btnHint.text = "힌트 또 보기"
	}

	/** [fullText]를 단어 단위로 나눠서, 앞에서부터 [revealedWords]개의 단어만 보여준다. */
	private fun buildHintText(fullText: String, revealedWords: Int): String {
		val words = fullText.split(Regex("\\s+")).filter { it.isNotEmpty() }
		return words.take(revealedWords).joinToString(" ")
	}

	private fun revealAnswer() {
		btnReveal.visibility = View.GONE
		textVerseContent.visibility = View.VISIBLE
		textHint.visibility = View.GONE
		btnHint.visibility = View.GONE
		containerAnswerButtons.visibility = View.VISIBLE
	}

	private fun answerCard(memorized: Boolean) {
		if (memorized) {
			memorizedCount += 1
		} else {
			missedVerses.add(verses[currentIndex])
		}
		recordProgress(verses[currentIndex], memorized)

		currentIndex += 1
		if (currentIndex < verses.size) {
			showCard(currentIndex)
		} else {
			showComplete()
		}
	}

	private fun recordProgress(ref: MemorizationVerse, memorized: Boolean) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val existing = db.verseMemorizationProgressDao().getByRefId(ref.id)
			db.verseMemorizationProgressDao().upsert(
				VerseMemorizationProgress(
					id = existing?.id ?: 0,
					verseRefId = ref.id,
					reviewCount = (existing?.reviewCount ?: 0) + 1,
					lastReviewedAt = System.currentTimeMillis(),
					isMastered = memorized
				)
			)
		}
	}

	private fun showComplete() {
		textVerseRef.visibility = View.GONE
		containerVerseCard.visibility = View.GONE
		textHint.visibility = View.GONE
		btnHint.visibility = View.GONE
		containerAnswerButtons.visibility = View.GONE
		containerComplete.visibility = View.VISIBLE

		textCompleteSummary.text =
			"오늘 ${verses.size}개 구절을 복습했어요!\n외운 구절 ${memorizedCount}개"

		// 전부 외웠으면(한 번도 "다시 볼래요"를 안 눌렀으면) 전체를 다시 연습할 수 있게 하고,
		// 하나라도 놓쳤으면 그 구절들만 다시 볼 수 있게 한다.
		if (missedVerses.isEmpty()) {
			btnRestart.visibility = View.VISIBLE
			btnReviewMissed.visibility = View.GONE
		} else {
			btnRestart.visibility = View.GONE
			btnReviewMissed.visibility = View.VISIBLE
		}
	}
}