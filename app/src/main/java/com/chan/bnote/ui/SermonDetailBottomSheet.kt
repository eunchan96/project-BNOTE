package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.Sermon
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SermonDetailBottomSheet(private val sermon: Sermon) : BottomSheetDialogFragment() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_sermon_detail, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_sermon_title).text = sermon.title
		view.findViewById<TextView>(R.id.text_sermon_meta).text =
			"${sermon.preacher} · ${DateUtils.formatDate(sermon.sermonDate)}"

		val bibleRefView = view.findViewById<TextView>(R.id.text_sermon_bible_ref)
		if (sermon.bookId != null && sermon.chapter != null) {
			bibleRefView.text = "${BibleBooks.nameOf(sermon.bookId)} ${sermon.chapter}장"
			bibleRefView.visibility = View.VISIBLE
		}

		view.findViewById<TextView>(R.id.text_sermon_memo).text =
			sermon.memo.ifBlank { "메모가 없어요" }
	}
}