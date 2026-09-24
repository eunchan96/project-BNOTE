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
		val title: String,
		val suffix: String? = null,
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

	/** 탭이 유지되는 프래그먼트라 하단 탭을 눌러 마이페이지로 돌아와도 onResume은 다시 안 불린다
	 * (성경 탭에서 장을 읽거나 스크랩/암송을 추가한 뒤 탭만 바꿔 돌아오면, 다른 화면을 한 번
	 * 거치기 전까지는 "최근 활동"이 그 사이 값을 못 따라오던 원인). BibleFragment의
	 * refreshOnReturnToTab()과 같은 이유로, hide()/show() 탭 전환 콜백에서도 다시 불러온다. */
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) {
			loadProfileThumbnail()
			loadRecentActivity()
		}
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
			val recentSermons = db.sermonViewDao().getRecentSermons(5)
			val recentApplications = db.applicationViewDao().getRecentApplications(5)
			val recentVerseMemos = db.verseMemoDao().getRecent(5)
			val recentWordMemos = db.wordMemoDao().getRecent(5)

			val container = view.findViewById<LinearLayout>(R.id.container_recent_activity)
			if (recentChapters.isEmpty() && recentSermons.isEmpty() && recentApplications.isEmpty() &&
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
						title = "${BibleBooks.nameOf(chapterView.bookId)} ${chapterView.chapter}${unit}",
						timestamp = chapterView.viewedAt
					) { navigateToBible(chapterView.bookId, chapterView.chapter) }
				)
			}

			// 설교: "설교제목 설교" ("설교"만 회색)
			for (row in recentSermons) {
				val sermon = row.sermon
				chips.add(
					RecentChip(
						title = sermon.title,
						suffix = "설교",
						timestamp = row.viewedAt
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

			// 적용: "적용제목 적용" ("적용"만 회색). 제목을 안 적었으면 ApplicationRowBuilder와
			// 같은 방식으로 "OO월 OO일 적용"을 대신 보여준다.
			for (row in recentApplications) {
				val application = row.application
				val displayTitle = application.title.ifBlank {
					"${com.chan.bnote.data.DateUtils.formatDateShort(application.applicationDate)} 적용"
				}
				chips.add(
					RecentChip(
						title = displayTitle,
						suffix = "적용",
						timestamp = row.viewedAt
					) {
						startActivity(
							com.chan.bnote.ui.application.ApplicationDetailActivity.createIntent(
								requireContext(),
								application.id
							)
						)
					}
				)
			}

			// 구절 메모: "창세기 1:1 메모" ("메모"만 회색)
			for (memo in recentVerseMemos) {
				val ref = "${BibleBooks.nameOf(memo.bookId)} ${memo.chapter}:${memo.verse}"
				chips.add(
					RecentChip(title = ref, suffix = "메모", timestamp = memo.updatedAt) {
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
					RecentChip(title = ref, suffix = "메모", timestamp = memo.updatedAt) {
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

	/** 제목이 길면 "..."으로 줄이되, 회색 접미사("설교"/"적용"/"메모")는 절대 안 잘리고 항상 끝에 붙어 나오게 만든다. 예: "눈 앞의 사실보다... 적용".
	 * TextView의 ellipsize 속성은 붙여넣은 문자열 전체를 하나로 보고 끝을 잘라서, 자칫하면 접미사까지 잘려나가던 문제가 있었다 — chip.title/chip.suffix를 따로 받아서 여기서 직접 잘라 붙인다.
	 *
	 * paint는 반드시 이 TextView에 텍스트 크기(textSize)를 먼저 설정한 뒤의 것을 써야 정확히 잰다(paint는 TextView가 그릴 때 쓰는 것과 같은 객체라, 크기를 바꾸면 즉시 반영된다). */
	private fun buildChipLabel(
		chip: RecentChip,
		paint: android.text.TextPaint,
		maxWidthPx: Int
	): CharSequence {
		val suffix = chip.suffix ?: return TextUtils.ellipsize(
			chip.title, paint, maxWidthPx.toFloat(), TextUtils.TruncateAt.END
		)

		val suffixText = " $suffix"
		val suffixWidth = paint.measureText(suffixText)
		val titleMaxWidth = (maxWidthPx - suffixWidth).coerceAtLeast(0f)
		val truncatedTitle =
			TextUtils.ellipsize(chip.title, paint, titleMaxWidth, TextUtils.TruncateAt.END)

		val full = "$truncatedTitle$suffixText"
		val spannable = SpannableString(full)
		spannable.setSpan(
			ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.text_hint)),
			truncatedTitle.length, full.length,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		return spannable
	}

	private fun renderRecentChips(chips: List<RecentChip>) {
		val view = view ?: return
		val container = view.findViewById<LinearLayout>(R.id.container_recent_chips)
		container.removeAllViews()

		val maxWidthPx = dp(160)
		val horizontalPaddingPx = dp(14) * 2

		for (chip in chips) {
			val chipView = TextView(requireContext()).apply {
				textSize = 13f
				maxLines = 1
				maxWidth = maxWidthPx // 아래서 폭에 맞춰 미리 잘라 넣지만, 혹시 몰라 안전장치로 둔다.
				setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary))
				setPadding(dp(14), dp(10), dp(14), dp(10))
				background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_outline)
				isClickable = true
				isFocusable = true
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { marginEnd = dp(8) }
				setOnClickListener { chip.onClick() }
				// textSize를 이미 위에서 정했으니, 이 시점의 paint로 실제 화면에 그려질 폭을 잰다.
				text = buildChipLabel(chip, paint, maxWidthPx - horizontalPaddingPx)
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