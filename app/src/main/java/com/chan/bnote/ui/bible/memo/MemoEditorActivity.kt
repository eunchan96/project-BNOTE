package com.chan.bnote.ui.bible.memo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.sermon.CitationMatch
import com.chan.bnote.ui.sermon.detail.CitationBubbleHelper

class MemoEditorActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_TITLE = "extra_title"
		private const val EXTRA_PREVIEW = "extra_preview"
		private const val EXTRA_INITIAL_TEXT = "extra_initial_text"
		private const val EXTRA_IS_EXISTING = "extra_is_existing"

		const val EXTRA_RESULT_ACTION = "extra_result_action"
		const val EXTRA_RESULT_TEXT = "extra_result_text"
		const val ACTION_SAVE = "action_save"
		const val ACTION_DELETE = "action_delete"

		fun createIntent(
			context: Context,
			titleText: String,
			previewText: String?,
			initialText: String,
			isExisting: Boolean
		): Intent {
			return Intent(context, MemoEditorActivity::class.java).apply {
				putExtra(EXTRA_TITLE, titleText)
				putExtra(EXTRA_PREVIEW, previewText)
				putExtra(EXTRA_INITIAL_TEXT, initialText)
				putExtra(EXTRA_IS_EXISTING, isExisting)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memo_editor)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memo_editor_root)) { v, insets ->
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

		val titleText = intent.getStringExtra(EXTRA_TITLE) ?: ""
		val previewText = intent.getStringExtra(EXTRA_PREVIEW)
		val initialText = intent.getStringExtra(EXTRA_INITIAL_TEXT) ?: ""
		val isExisting = intent.getBooleanExtra(EXTRA_IS_EXISTING, false)

		findViewById<TextView>(R.id.text_top_bar_title).text = titleText
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val previewView = findViewById<TextView>(R.id.text_memo_preview)
		if (!previewText.isNullOrBlank()) {
			previewView.text = previewText
			previewView.visibility = View.VISIBLE
		} else {
			previewView.visibility = View.GONE
		}

		val editText = findViewById<EditText>(R.id.edit_memo_text)
		editText.setText(initialText)

		var latestCitations: List<CitationMatch> = emptyList()

		fun refreshCitations() {
			latestCitations = CitationBubbleHelper.applySpans(editText.text)
		}

		editText.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				refreshCitations()
			}
		})

		refreshCitations()
		CitationBubbleHelper.attachTouchHandling(editText, { latestCitations }, lifecycleScope)

		val deleteBtn = findViewById<TextView>(R.id.btn_delete_memo)
		if (isExisting) {
			deleteBtn.visibility = View.VISIBLE
			deleteBtn.setOnClickListener {
				val result = Intent().putExtra(EXTRA_RESULT_ACTION, ACTION_DELETE)
				setResult(Activity.RESULT_OK, result)
				finish()
			}
		}

		findViewById<TextView>(R.id.btn_save_memo).setOnClickListener {
			val text = editText.text.toString().trim()
			if (text.isNotEmpty()) {
				val result = Intent()
					.putExtra(EXTRA_RESULT_ACTION, ACTION_SAVE)
					.putExtra(EXTRA_RESULT_TEXT, text)
				setResult(Activity.RESULT_OK, result)
				finish()
			}
		}
	}
}