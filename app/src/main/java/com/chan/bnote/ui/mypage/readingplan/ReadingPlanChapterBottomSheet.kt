package com.chan.bnote.ui.mypage.readingplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import com.chan.bnote.ui.DraggableBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ReadingPlanChapterBottomSheet(
	private val bookId: Int
) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.7f

	/** 장 읽음 상태를 바꾸고 이 바텀시트가 닫힐 때 호출된다 (전체 진행률 갱신용). */
	var onDismissed: (() -> Unit)? = null

	private var changed = false
	private var maxChapter = 1
	private lateinit var gridAdapter: ReadingPlanChapterGridAdapter

	override fun onDismiss(dialog: android.content.DialogInterface) {
		super.onDismiss(dialog)
		if (changed) onDismissed?.invoke()
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_reading_plan_chapters, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_reading_plan_chapter_title).text =
			"${BibleBooks.nameOf(bookId)} 읽음 현황"

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_reading_plan_chapters)
		recyclerView.layoutManager = GridLayoutManager(requireContext(), 5)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			maxChapter = db.bibleDao().getMaxChapter("NKRV", bookId)
			val readChapters = loadReadChapters(db)

			gridAdapter = ReadingPlanChapterGridAdapter(
				maxChapter = maxChapter,
				readChapters = readChapters,
				onToggle = { chapter -> toggleChapter(chapter) },
				onNavigate = { chapter ->
					navigateToBible(chapter)
					dismissAllAndNavigate()
				}
			)
			recyclerView.adapter = gridAdapter

			view.findViewById<TextView>(R.id.btn_mark_all_read)
				.setOnClickListener { confirmMarkAllRead() }
			view.findViewById<TextView>(R.id.btn_reset_book_progress)
				.setOnClickListener { confirmResetBook() }
		}
	}

	private fun confirmMarkAllRead() {
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("전체 읽음 처리")
			.setMessage("${BibleBooks.nameOf(bookId)} 전체 ${maxChapter}장을 읽음으로 표시할까요?")
			.setPositiveButton("전체 읽음") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					for (chapter in 1..maxChapter) {
						db.readingProgressDao()
							.upsert(ReadingProgress(bookId = bookId, chapter = chapter))
					}
					changed = true
					gridAdapter.updateReadChapters(loadReadChapters(db))
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun confirmResetBook() {
		MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("읽음 기록 초기화")
			.setMessage("${BibleBooks.nameOf(bookId)}의 읽음 기록을 전부 지울까요?")
			.setPositiveButton("초기화") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(requireContext().applicationContext)
					db.readingProgressDao().deleteByBook(bookId)
					changed = true
					gridAdapter.updateReadChapters(loadReadChapters(db))
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private suspend fun loadReadChapters(db: com.chan.bnote.data.BibleDatabase): Set<Int> {
		return db.readingProgressDao().getAll()
			.filter { it.bookId == bookId }
			.map { it.chapter }
			.toSet()
	}

	private fun toggleChapter(chapter: Int) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val existing = db.readingProgressDao().get(bookId, chapter)
			if (existing != null) {
				db.readingProgressDao().delete(bookId, chapter)
			} else {
				db.readingProgressDao().upsert(ReadingProgress(bookId = bookId, chapter = chapter))
			}
			changed = true
			gridAdapter.updateReadChapters(loadReadChapters(db))
		}
	}

	// 마이페이지 -> 성경읽기표 -> 책별 화면, 이렇게 시트가 2겹 열려있을 수 있어서 둘 다 닫아줌
	private fun dismissAllAndNavigate() {
		dismiss()
		(parentFragmentManager.findFragmentByTag("reading_plan") as? DraggableBottomSheet)?.dismiss()
	}

	private fun navigateToBible(chapter: Int) {
		val intent =
			android.content.Intent(requireContext(), com.chan.bnote.MainActivity::class.java)
				.apply {
					putExtra(com.chan.bnote.MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
					putExtra(com.chan.bnote.MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
					flags =
						android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
				}
		startActivity(intent)
	}
}