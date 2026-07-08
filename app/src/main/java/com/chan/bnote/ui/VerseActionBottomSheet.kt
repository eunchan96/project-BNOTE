package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chan.bnote.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class VerseActionBottomSheet(
	private val verseText: String,
	private val isHighlighted: Boolean,
	private val isFavorite: Boolean,
	private val onToggleHighlight: () -> Unit,
	private val onToggleFavorite: () -> Unit
) : BottomSheetDialogFragment() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_verse_actions, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_verse_preview).text = verseText

		val highlightItem = view.findViewById<TextView>(R.id.action_highlight)
		highlightItem.text = if (isHighlighted) "하이라이트 해제" else "하이라이트 표시"
		highlightItem.setOnClickListener {
			onToggleHighlight()
			dismiss()
		}

		val favoriteItem = view.findViewById<TextView>(R.id.action_favorite)
		favoriteItem.text = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가"
		favoriteItem.setOnClickListener {
			onToggleFavorite()
			dismiss()
		}
	}
}