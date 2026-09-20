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
		val root =
			LayoutInflater.from(parent.context).inflate(R.layout.item_bible_page, parent, false)
		val recyclerView = root.findViewById<RecyclerView>(R.id.recycler_page_verses)
		val scrollbarView = root.findViewById<VerseScrollbarView>(R.id.verse_scrollbar)
		return PageViewHolder(root, recyclerView, scrollbarView)
	}

	override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
		val (bookId, chapter) = BibleChapterIndex.chapterAt(position) ?: return
		holder.bind(fragment, bookId, chapter)
	}

	override fun onViewRecycled(holder: PageViewHolder) {
		holder.cancel()
	}

	class PageViewHolder(
		root: android.view.View,
		private val recyclerView: RecyclerView,
		private val scrollbarView: VerseScrollbarView
	) : RecyclerView.ViewHolder(root) {
		private var loadJob: Job? = null

		// 스크롤 리스너가 매번 호출하는 updateFrom()이 설정에서 꺼둔 스크롤바를 다시 보이게
		// 만들어버리지 않도록, "지금 설정상 켜져 있는지"를 따로 기억해둔다.
		private var scrollbarEnabled = true
		var boundBookId: Int = -1
			private set
		var boundChapter: Int = -1
			private set

		init {
			recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
			recyclerView.itemAnimator = null
			recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
					if (scrollbarEnabled) scrollbarView.updateFrom(rv)
				}
			})
			// 처음 레이아웃이 잡히는 시점(뷰 크기가 0에서 실제 값으로 바뀌는 순간)에도 한 번
			// 다시 그려줘야, 페이지가 열리자마자(스크롤 전) 스크롤바가 바로 보인다.
			scrollbarView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
				if (bottom != oldBottom && scrollbarEnabled) scrollbarView.updateFrom(recyclerView)
			}
		}

		fun bind(fragment: BibleFragment, bookId: Int, chapter: Int) {
			boundBookId = bookId
			boundChapter = chapter
			loadJob?.cancel()
			recyclerView.adapter = null
			// 설정에서 바뀌었을 수 있으니 매번 다시 확인해서 반영한다(뷰홀더는 재활용되므로
			// init 시점에만 적용하면 이미 만들어진 페이지에는 설정 변경이 반영되지 않는다).
			scrollbarEnabled =
				com.chan.bnote.data.AppSettings.isBibleScrollbarVisible(fragment.requireContext())
			scrollbarView.visibility =
				if (scrollbarEnabled) android.view.View.VISIBLE else android.view.View.GONE

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
				fragment.consumePendingMemoOpen(bookId, chapter)

				if (scrollbarEnabled) {
					// 새 어댑터/데이터가 붙은 뒤 다음 레이아웃 패스에서 실제 스크롤 범위가 확정되면
					// 그때 한 번 그려준다(리스트가 바뀌자마자 곧바로 계산하면 아직 옛 값일 수 있음).
					recyclerView.post { scrollbarView.updateFrom(recyclerView) }
				}
			}
		}

		fun cancel() {
			loadJob?.cancel()
			scrollbarView.cancelPendingFade()
		}

		fun currentRecyclerView(): RecyclerView = recyclerView

		/** 설정에서 스크롤바 표시를 껐다 켰다 했을 때, 페이지가 다시 bind()되지 않아도(=지금
		 * 보고 있는 장 그대로일 때도) 바로 반영하기 위한 함수. 뷰의 visibility만 바꾸면 내부에
		 * 남아있는 scrollbarEnabled 플래그가 여전히 옛 값이라, 스크롤 리스너가 그 플래그를 보고
		 * 다시 켜버릴 수 있다 — 그래서 반드시 이 함수로 플래그까지 같이 바꿔야 한다. */
		fun setScrollbarEnabled(enabled: Boolean) {
			scrollbarEnabled = enabled
			if (enabled) {
				scrollbarView.visibility = android.view.View.VISIBLE
				scrollbarView.updateFrom(recyclerView)
			} else {
				scrollbarView.visibility = android.view.View.GONE
			}
		}
	}
}