package com.chan.bnote.ui.application.addapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowBuilder
import kotlinx.coroutines.launch

/** 적용 작성 화면의 "설교 추가" 버튼에서 열리는, 최근에 작성한 설교를 최신순으로 보여주는 목록. */
class SermonPickerBottomSheet : FixedBottomSheetDialogFragment() {

	var onSermonSelected: ((Sermon) -> Unit)? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_sermon_picker, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_sermon_picker)
		val emptyText = view.findViewById<TextView>(R.id.text_sermon_picker_empty)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			// 최신순(추가한 순서 최근 것부터) — 최대 100개까지만 불러와도 실사용엔 충분하다.
			val sermons = db.sermonDao().getRecent(100)

			if (sermons.isEmpty()) {
				emptyText.visibility = View.VISIBLE
				recyclerView.visibility = View.GONE
				return@launch
			}
			emptyText.visibility = View.GONE
			recyclerView.visibility = View.VISIBLE

			val rows = SermonRowBuilder.build(db, sermons, useDateLabel = true)
			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				onSermonSelected?.invoke(sermon)
				dismiss()
			}
		}
	}
}