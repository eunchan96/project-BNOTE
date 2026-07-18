package com.chan.bnote.ui.mypage

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
import com.chan.bnote.data.mypage.MemorizationVerse
import com.chan.bnote.data.mypage.VerseMemorizationProgress
import kotlinx.coroutines.launch

class MemorizationPracticeActivity : AppCompatActivity() {

	private lateinit var textProgress: TextView
	private lateinit var textEmptyState: TextView
	private lateinit var containerPractice: LinearLayout
	private lateinit var textVerseRef: TextView
	private lateinit var containerVerseCard: LinearLayout
	private lateinit var textVerseContent: TextView
	private lateinit var btnReveal: TextView
	private lateinit var containerAnswerButtons: LinearLayout
	private lateinit var containerComplete: LinearLayout
	private lateinit var textCompleteSummary: TextView

	private var verses: List<MemorizationVerse> = emptyList()
	private var currentIndex = 0
	private var memorizedCount = 0

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
		containerAnswerButtons = findViewById(R.id.container_answer_buttons)
		containerComplete = findViewById(R.id.container_complete)
		textCompleteSummary = findViewById(R.id.text_complete_summary)

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		btnReveal.setOnClickListener { revealAnswer() }
		findViewById<TextView>(R.id.btn_review_again).setOnClickListener { answerCard(memorized = false) }
		findViewById<TextView>(R.id.btn_memorized).setOnClickListener { answerCard(memorized = true) }
		findViewById<TextView>(R.id.btn_restart).setOnClickListener { startSession(verses) }

		loadVerses()
	}

	private fun loadVerses() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val refs = db.memorizationVerseDao().getAll()

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
		verses = refs.shuffled()
		currentIndex = 0
		memorizedCount = 0
		showCard(currentIndex)
	}

	private fun showCard(index: Int) {
		containerComplete.visibility = View.GONE
		textVerseRef.visibility = View.VISIBLE
		containerVerseCard.visibility = View.VISIBLE

		val ref = verses[index]
		textProgress.text = "${index + 1} / ${verses.size}"
		textVerseRef.text = ref.toShortLabel()

		textVerseContent.text = ref.verseText
		textVerseContent.visibility = View.GONE
		btnReveal.visibility = View.VISIBLE
		containerAnswerButtons.visibility = View.GONE
	}

	private fun revealAnswer() {
		btnReveal.visibility = View.GONE
		textVerseContent.visibility = View.VISIBLE
		containerAnswerButtons.visibility = View.VISIBLE
	}

	private fun answerCard(memorized: Boolean) {
		if (memorized) memorizedCount += 1
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
		containerAnswerButtons.visibility = View.GONE
		containerComplete.visibility = View.VISIBLE

		textCompleteSummary.text =
			"오늘 ${verses.size}개 구절을 복습했어요!\n외운 구절 ${memorizedCount}개"
	}
}