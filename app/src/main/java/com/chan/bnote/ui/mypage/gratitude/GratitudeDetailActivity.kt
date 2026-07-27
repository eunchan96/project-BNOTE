package com.chan.bnote.ui.mypage.gratitude

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class GratitudeDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NOTE_ID = "extra_note_id"

		fun start(context: Context, noteId: Long) {
			context.startActivity(createIntent(context, noteId))
		}

		fun createIntent(context: Context, noteId: Long): Intent {
			return Intent(context, GratitudeDetailActivity::class.java)
				.putExtra(EXTRA_NOTE_ID, noteId)
		}
	}

	private var noteId: Long = -1L
	private var changed = false

	private val editLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			changed = true
			loadNote()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_gratitude_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gratitude_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener {
			setResult(if (changed) Activity.RESULT_OK else Activity.RESULT_CANCELED)
			finish()
		}
		findViewById<ImageView>(R.id.btn_edit_gratitude).setOnClickListener {
			editLauncher.launch(AddGratitudeActivity.editIntent(this, noteId))
		}
		findViewById<ImageView>(R.id.btn_delete_gratitude).setOnClickListener { confirmDelete() }

		loadNote()
	}

	private fun loadNote() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val note = db.gratitudeNoteDao().getById(noteId) ?: run {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_detail_date).text = DateUtils.formatDate(note.date)

			val entries = db.gratitudeEntryDao().getByNote(note.id)
			val container = findViewById<LinearLayout>(R.id.container_gratitude_detail_entries)
			container.removeAllViews()

			if (entries.isEmpty()) {
				val emptyText = TextView(this@GratitudeDetailActivity).apply {
					text = "작성된 감사 내용이 없어요"
					setTextColor(
						androidx.core.content.ContextCompat.getColor(
							this@GratitudeDetailActivity, R.color.text_hint
						)
					)
					setPadding(0, dp(8), 0, dp(8))
				}
				container.addView(emptyText)
			} else {
				for (entry in entries) {
					val row = LinearLayout(this@GratitudeDetailActivity).apply {
						orientation = LinearLayout.HORIZONTAL
						setPadding(0, dp(4), 0, dp(4))
					}
					val check = TextView(this@GratitudeDetailActivity).apply {
						text = "✓"
						textSize = 16f
						setTextColor(
							androidx.core.content.ContextCompat.getColor(
								this@GratitudeDetailActivity, R.color.brown_primary
							)
						)
						setPadding(0, 0, dp(8), 0)
					}
					val text = TextView(this@GratitudeDetailActivity).apply {
						this.text = entry.text
						textSize = 15f
						setTextColor(
							androidx.core.content.ContextCompat.getColor(
								this@GratitudeDetailActivity, R.color.text_primary
							)
						)
					}
					row.addView(check)
					row.addView(text)
					container.addView(row)
				}
			}
		}
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("감사노트 삭제")
			.setMessage("이 감사노트를 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.gratitudeNoteDao().getById(noteId)?.let { note ->
						db.gratitudeEntryDao().deleteByNote(note.id)
						db.gratitudeNoteDao().delete(note)
					}
					setResult(Activity.RESULT_OK)
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}