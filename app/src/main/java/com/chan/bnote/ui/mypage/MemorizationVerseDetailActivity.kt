package com.chan.bnote.ui.mypage

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
import com.chan.bnote.data.mypage.MemorizationVerse
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MemorizationVerseDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_VERSE_ID = "extra_verse_id"

		fun createIntent(context: Context, verseId: Long): Intent =
			Intent(context, MemorizationVerseDetailActivity::class.java)
				.putExtra(EXTRA_VERSE_ID, verseId)
	}

	private var verse: MemorizationVerse? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_verse_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		findViewById<ImageView>(R.id.btn_delete).setOnClickListener { confirmDelete() }
		findViewById<TextView>(R.id.btn_practice_this).setOnClickListener { practiceThisVerse() }

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
			findViewById<TextView>(R.id.text_verse_ref).text = item.toDisplayLabel()
			findViewById<TextView>(R.id.text_verse_content).text = item.verseText
		}
	}

	private fun practiceThisVerse() {
		val item = verse ?: return
		startActivity(MemorizationPracticeActivity.singleVerseIntent(this, item.id))
	}

	private fun confirmDelete() {
		val item = verse ?: return
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("암송 구절 삭제")
			.setMessage("'${item.toDisplayLabel()}'를 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.memorizationVerseDao().delete(item)
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}
}