package com.chan.bnote.ui.bible

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ViewPager2가 쓰는 어댑터. 페이지 하나 = 성경 장 하나. 스크롤(스와이프)은 ViewPager2가 자체적으로
 * 부드럽게 처리해주므로, 예전처럼 직접 만든 터치/애니메이션 코드가 필요 없다.
 *
 * 각 페이지는 독립적으로 자기 장의 본문·하이라이트·북마크·메모를 불러와서 실제 인터랙티브
 * VerseAdapter를 만든다. 이때 쓰는 콜백들은 BibleFragment가 이미 갖고 있던 것과 동일한 걸 그대로
 * 재사용한다(하이라이트 저장 등은 currentBookId/currentChapter 기준으로 동작하는데, 이 값들은 페이지가
 * 바뀔 때마다 BibleFragment의 onPageSelected에서 갱신되므로 항상 "지금 보이는 페이지"와 일치한다).
 */
class BiblePageAdapter(private val fragment: BibleFragment) :
	RecyclerView.Adapter<BiblePageAdapter.PageViewHolder>() {

	override fun getItemCount(): Int = BibleChapterIndex.totalPages()

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_bible_page, parent, false)
		return PageViewHolder(view as RecyclerView)
	}

	override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
		val (bookId, chapter) = BibleChapterIndex.chapterAt(position) ?: return
		holder.bind(fragment, bookId, chapter)
	}

	override fun onViewRecycled(holder: PageViewHolder) {
		holder.cancel()
	}

	class PageViewHolder(private val recyclerView: RecyclerView) :
		RecyclerView.ViewHolder(recyclerView) {
		private var loadJob: Job? = null
		var boundBookId: Int = -1
			private set
		var boundChapter: Int = -1
			private set

		init {
			recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
			recyclerView.itemAnimator = null
		}

		fun bind(fragment: BibleFragment, bookId: Int, chapter: Int) {
			boundBookId = bookId
			boundChapter = chapter
			loadJob?.cancel()
			recyclerView.adapter = null
			// 설정에서 바뀌었을 수 있으니 매번 다시 확인해서 반영한다(뷰홀더는 재활용되므로
			// init 시점에만 적용하면 이미 만들어진 페이지에는 설정 변경이 반영되지 않는다).
			recyclerView.isVerticalScrollBarEnabled =
				com.chan.bnote.data.AppSettings.isBibleScrollbarVisible(fragment.requireContext())
			loadJob = fragment.viewLifecycleOwner.lifecycleScope.launch {
				val page = fragment.loadPageData(bookId, chapter)
				val footer = fragment.createFooterAdapterFor(bookId, chapter, page.isRead)
				recyclerView.adapter =
					androidx.recyclerview.widget.ConcatAdapter(page.adapter, footer)
				fragment.registerPageFooter(bookId, chapter, footer)
				// onPageSelected 시점엔 이 페이지의 뷰홀더/어댑터가 아직 준비 안 됐을 수 있어서(타이밍 문제),
				// 데이터 로드가 끝나는 이 시점에도 다시 한번 알려서 fragment.adapter가 확실히 채워지게 한다.
				// (그렇지 않으면 lateinit adapter가 초기화되기 전에 절을 탭했을 때 앱이 튕겼다.)
				fragment.onPageDataReady(bookId, chapter, recyclerView, page.adapter)

				fragment.consumePendingScrollVerse(bookId, chapter)?.let { verseNum ->
					val index = page.verses.indexOfFirst { it.verse == verseNum }
					if (index >= 0) recyclerView.scrollToPosition(index) else recyclerView.scrollToPosition(
						0
					)
				}
			}
		}

		fun cancel() {
			loadJob?.cancel()
		}

		fun currentRecyclerView(): RecyclerView = recyclerView
	}
}