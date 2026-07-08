package com.chan.bnote.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.Sermon
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.Calendar

class AddSermonBottomSheet : BottomSheetDialogFragment() {

	var onSaved: (() -> Unit)? = null

	private var selectedDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
	private var selectedBookId: Int? = null
	private var selectedChapter: Int? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_add_sermon, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val editTitle = view.findViewById<EditText>(R.id.edit_title)
		val editPreacher = view.findViewById<EditText>(R.id.edit_preacher)
		val editMemo = view.findViewById<EditText>(R.id.edit_memo)
		val btnDate = view.findViewById<TextView>(R.id.btn_pick_date)
		val btnBibleRef = view.findViewById<TextView>(R.id.btn_pick_bible_ref)

		btnDate.text = DateUtils.formatDate(selectedDateMillis)
		btnDate.setOnClickListener {
			val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
			DatePickerDialog(
				requireContext(),
				{ _, year, month, day ->
					val picked = Calendar.getInstance()
					picked.set(year, month, day, 0, 0, 0)
					selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
					btnDate.text = DateUtils.formatDate(selectedDateMillis)
				},
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
			).show()
		}

		btnBibleRef.setOnClickListener {
			val sheet = BookChapterPickerBottomSheet("GAEYEOK") // 참조용이라 개역개정 기준 고정
			sheet.onVerseSelected = { bookId, chapter, _ ->
				selectedBookId = bookId
				selectedChapter = chapter
				btnBibleRef.text = "${BibleBooks.nameOf(bookId)} ${chapter}장"
			}
			sheet.show(parentFragmentManager, "sermon_bible_ref")
		}

		view.findViewById<TextView>(R.id.btn_save_sermon).setOnClickListener {
			val title = editTitle.text.toString().trim()
			val preacher = editPreacher.text.toString().trim()

			if (title.isEmpty() || preacher.isEmpty()) {
				Toast.makeText(requireContext(), "제목과 설교자를 입력해주세요", Toast.LENGTH_SHORT).show()
				return@setOnClickListener
			}

			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(requireContext().applicationContext)
				db.sermonDao().insert(
					Sermon(
						title = title,
						preacher = preacher,
						sermonDate = selectedDateMillis,
						bookId = selectedBookId,
						chapter = selectedChapter,
						memo = editMemo.text.toString()
					)
				)
				onSaved?.invoke()
				dismiss()
			}
		}
	}
}