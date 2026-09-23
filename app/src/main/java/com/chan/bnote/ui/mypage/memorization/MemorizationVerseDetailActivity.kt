package com.chan.bnote.ui.mypage.memorization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
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
	private lateinit var noteEdit: EditText

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_verse_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_detail_root)) { v, insets ->
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

		noteEdit = findViewById(R.id.edit_verse_note)

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		findViewById<ImageView>(R.id.btn_delete).setOnClickListener { confirmDelete() }
		findViewById<ImageView>(R.id.btn_edit_verse).setOnClickListener {
			val item = verse ?: return@setOnClickListener
			startActivity(MemorizationVerseEditActivity.createIntent(this, item.id))
		}
		findViewById<TextView>(R.id.btn_practice_this).setOnClickListener { practiceThisVerse() }

		loadVerse()
	}

	override fun onResume() {
		super.onResume()
		// 수정 화면(구절 · 그룹 변경)에서 고치고 돌아왔을 수도 있으니 매번 다시 불러온다.
		loadVerse()
	}

	override fun onPause() {
		super.onPause()
		saveNote()
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
			// onResume에서 다시 불러올 때, 방금 이 화면에서 입력 중이던 메모(아직 onPause로
			// 저장 안 된 것)를 덮어쓰지 않도록 포커스가 없을 때만 다시 채운다.
			if (!noteEdit.isFocused) noteEdit.setText(item.note)

			val group = db.memorizationVerseDao().getAllGroups().find { it.id == item.groupId }
			findViewById<TextView>(R.id.text_verse_group).text = "그룹 : ${group?.name ?: "미분류"}"
		}
	}

	/** 메모는 저장 버튼 없이, 화면을 벗어날 때(뒤로가기·연습하기·수정 화면 이동 포함) 자동으로 저장한다. */
	private fun saveNote() {
		val item = verse ?: return
		val newNote = noteEdit.text.toString()
		if (newNote == item.note) return

		val updated = item.copy(note = newNote)
		verse = updated
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			db.memorizationVerseDao().update(updated)
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