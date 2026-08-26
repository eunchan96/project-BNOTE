package com.chan.bnote.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.mypage.profile.ProfileDisplay
import com.chan.bnote.ui.BibleNavigationHost
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.mypage.memorization.MemorizationVerseListActivity
import com.chan.bnote.ui.mypage.prayer.PrayerRequestActivity
import com.chan.bnote.ui.mypage.profile.ProfileActivity
import com.chan.bnote.ui.mypage.profile.loadProfilePhoto
import com.chan.bnote.ui.mypage.profile.setNameWithPosition
import com.chan.bnote.ui.mypage.readingplan.ReadingPlanActivity
import com.chan.bnote.ui.mypage.settings.SettingsActivity
import com.chan.bnote.ui.mypage.verseofyear.VerseOfYearActivity
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import kotlinx.coroutines.launch

class MyPageFragment : Fragment(), TopBarActionHandler {

	private data class RecentChip(
		val label: CharSequence,
		val timestamp: Long,
		val onClick: () -> Unit
	)

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_mypage, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<View>(R.id.container_profile_thumbnail).setOnClickListener {
			startActivity(Intent(requireContext(), ProfileActivity::class.java))
		}
		view.findViewById<ImageView>(R.id.img_profile_photo).loadProfilePhoto(null)
		view.findViewById<TextView>(R.id.menu_reading_plan).setOnClickListener {
			startActivity(Intent(requireContext(), ReadingPlanActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_verse_of_year).setOnClickListener {
			startActivity(Intent(requireContext(), VerseOfYearActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_prayer_request).setOnClickListener {
			startActivity(Intent(requireContext(), PrayerRequestActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_memorization).setOnClickListener {
			startActivity(Intent(requireContext(), MemorizationVerseListActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_gratitude).setOnClickListener {
			startActivity(
				Intent(
					requireContext(),
					com.chan.bnote.ui.mypage.gratitude.GratitudeActivity::class.java
				)
			)
		}
	}

	override fun onResume() {
		super.onResume()
		// 정보 수정 화면/다른 활동 후 돌아왔을 때 최신 값을 반영하기 위해 매번 다시 불러온다.
		loadProfileThumbnail()
		loadRecentActivity()
	}

	private fun loadProfileThumbnail() {
		val view = view ?: return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val profile = db.userProfileDao().get()

			view.findViewById<TextView>(R.id.text_profile_name).setNameWithPosition(
				ProfileDisplay.nameText(profile), ProfileDisplay.positionText(profile)
			)
			view.findViewById<TextView>(R.id.text_profile_meta).text =
				ProfileDisplay.thumbnailMetaText(profile)
			view.findViewById<ImageView>(R.id.img_profile_photo)
				.loadProfilePhoto(profile?.photoPath)
		}
	}

	private fun loadRecentActivity() {
		val view = view ?: return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			val recentChapters = db.recentChapterViewDao().getRecent(5)
			val recentSermons = db.sermonDao().getRecent(5)
			val recentVerseMemos = db.verseMemoDao().getRecent(5)
			val recentWordMemos = db.wordMemoDao().getRecent(5)

			val container = view.findViewById<LinearLayout>(R.id.container_recent_activity)
			if (recentChapters.isEmpty() && recentSermons.isEmpty() &&
				recentVerseMemos.isEmpty() && recentWordMemos.isEmpty()
			) {
				container.visibility = View.GONE
				return@launch
			}
			container.visibility = View.VISIBLE

			val chips = mutableListOf<RecentChip>()

			// 장: 접미사 없이 그대로 ("창세기 1장")
			for (chapterView in recentChapters) {
				val unit = BibleBooks.chapterUnit(chapterView.bookId)
				chips.add(
					RecentChip(
						label = "${BibleBooks.nameOf(chapterView.bookId)} ${chapterView.chapter}${unit}",
						timestamp = chapterView.viewedAt
					) { navigateToBible(chapterView.bookId, chapterView.chapter) }
				)
			}

			// 설교: "설교제목 설교" ("설교"만 회색)
			for (sermon in recentSermons) {
				chips.add(
					RecentChip(
						label = suffixSpan(sermon.title, "설교"),
						timestamp = sermon.createdAt
					) {
						startActivity(
							SermonDetailActivity.createIntent(
								requireContext(),
								sermon.id
							)
						)
					}
				)
			}

			// 구절 메모: "창세기 1:1 메모" ("메모"만 회색)
			for (memo in recentVerseMemos) {
				val ref = "${BibleBooks.nameOf(memo.bookId)} ${memo.chapter}:${memo.verse}"
				chips.add(
					RecentChip(label = suffixSpan(ref, "메모"), timestamp = memo.updatedAt) {
						(requireActivity() as? BibleNavigationHost)
							?.navigateToBibleChapterAndOpenVerseMemo(
								memo.bookId, memo.chapter, memo.verse
							)
					}
				)
			}

			// 단어 메모: "창세기 1:1 태초에 메모" (실제 단어는 성경 본문에서 찾아와야 함, "메모"만 회색)
			for (memo in recentWordMemos) {
				val word = fetchWordMemoWord(
					db,
					memo.translation,
					memo.bookId,
					memo.chapter,
					memo.verse,
					memo.startOffset,
					memo.endOffset
				)
				val ref = "${BibleBooks.nameOf(memo.bookId)} ${memo.chapter}:${memo.verse}" +
						(if (word.isNotEmpty()) " $word" else "")
				chips.add(
					RecentChip(label = suffixSpan(ref, "메모"), timestamp = memo.updatedAt) {
						(requireActivity() as? BibleNavigationHost)
							?.navigateToBibleChapterAndOpenWordMemo(
								memo.bookId, memo.chapter, memo.verse,
								memo.startOffset, memo.endOffset, memo.segment
							)
					}
				)
			}

			renderRecentChips(chips.sortedByDescending { it.timestamp }.take(10))
		}
	}

	private suspend fun fetchWordMemoWord(
		db: BibleDatabase, translation: String, bookId: Int, chapter: Int, verse: Int,
		startOffset: Int, endOffset: Int
	): String {
		val verseText = db.bibleDao().getVerses(translation, bookId, chapter)
			.find { it.verse == verse }?.text ?: return ""
		if (startOffset < 0 || endOffset > verseText.length || startOffset >= endOffset) return ""
		return verseText.substring(startOffset, endOffset)
	}

	/** "본문 접미사" 형태로, 접미사(마지막 단어)만 회색으로 표시하는 문자열을 만든다. */
	private fun suffixSpan(prefix: String, suffix: String): CharSequence {
		val full = "$prefix $suffix"
		val spannable = SpannableString(full)
		val start = prefix.length + 1
		spannable.setSpan(
			ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.text_hint)),
			start, full.length,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		return spannable
	}

	private fun renderRecentChips(chips: List<RecentChip>) {
		val view = view ?: return
		val container = view.findViewById<LinearLayout>(R.id.container_recent_chips)
		container.removeAllViews()

		for (chip in chips) {
			val chipView = TextView(requireContext()).apply {
				text = chip.label
				textSize = 13f
				maxLines = 1
				ellipsize = TextUtils.TruncateAt.END
				maxWidth = dp(160)
				setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary))
				setPadding(dp(14), dp(10), dp(14), dp(10))
				background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_outline)
				isClickable = true
				isFocusable = true
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { marginEnd = dp(8) }
				setOnClickListener { chip.onClick() }
			}
			container.addView(chipView)
		}
	}

	private fun navigateToBible(bookId: Int, chapter: Int) {
		(requireActivity() as? BibleNavigationHost)?.navigateToBibleChapter(bookId, chapter)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	override fun getTopBarConfig() = TopBarConfig(
		title = "마이페이지",
		showMenu = true,
		menuIconRes = R.drawable.ic_settings
	)

	override fun onMenuClicked() {
		startActivity(Intent(requireContext(), SettingsActivity::class.java))
	}
}