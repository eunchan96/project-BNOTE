package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.memo.CitationMatch
import com.chan.bnote.ui.DraggableBottomSheet

class MemoEditorBottomSheet(
	private val titleText: String,
	private val previewText: String?,
	private val initialText: String,
	private val isExisting: Boolean,
	private val onSave: (String) -> Unit,
	private val onDelete: (() -> Unit)?
) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.55f

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_memo_editor, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_memo_title).text = titleText

		val previewView = view.findViewById<TextView>(R.id.text_memo_preview)
		if (!previewText.isNullOrBlank()) {
			previewView.text = previewText
			previewView.visibility = View.VISIBLE
		} else {
			previewView.visibility = View.GONE
		}

		val editText = view.findViewById<EditText>(R.id.edit_memo_text)
		editText.setText(initialText)

		var latestCitations: List<CitationMatch> = emptyList()

		fun refreshCitations() {
			latestCitations = CitationBubbleHelper.applySpans(editText.text)
		}

		editText.addTextChangedListener(object : android.text.TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: android.text.Editable?) {
				refreshCitations()
			}
		})

		refreshCitations()
		CitationBubbleHelper.attachTouchHandling(editText, { latestCitations }, lifecycleScope)

		val deleteBtn = view.findViewById<TextView>(R.id.btn_delete_memo)
		if (isExisting && onDelete != null) {
			deleteBtn.visibility = View.VISIBLE
			deleteBtn.setOnClickListener {
				onDelete.invoke()
				dismiss()
			}
		}

		view.findViewById<TextView>(R.id.btn_save_memo).setOnClickListener {
			val text = editText.text.toString().trim()
			if (text.isNotEmpty()) {
				onSave(text)
				dismiss()
			}
		}
	}
}