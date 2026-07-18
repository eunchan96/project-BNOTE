package com.chan.bnote.ui.mypage

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
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
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.profile.ProfileDisplay
import com.chan.bnote.ui.BibleNavigationHost
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.sermon.SermonDetailActivity
import kotlinx.coroutines.launch

class MyPageFragment : Fragment(), TopBarActionHandler {

	private data class RecentItem(
		val typeLabel: String,
		val title: String,
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
		view.findViewById<TextView>(R.id.menu_settings).setOnClickListener {
			startActivity(Intent(requireContext(), SettingsActivity::class.java))
		}
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
		view.findViewById<TextView>(R.id.menu_bible_knowledge).setOnClickListener {
			startActivity(
				Intent(
					requireContext(),
					com.chan.bnote.ui.knowledge.BibleKnowledgeHubActivity::class.java
				)
			)
		}
		view.findViewById<TextView>(R.id.menu_app_info).setOnClickListener {
			startActivity(Intent(requireContext(), AppInfoActivity::class.java))
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
			val recentSermons = db.sermonDao().getRecent(3)
			val recentMemos = db.verseMemoDao().getRecent(3)

			val container = view.findViewById<LinearLayout>(R.id.container_recent_activity)
			if (recentChapters.isEmpty() && recentSermons.isEmpty() && recentMemos.isEmpty()) {
				container.visibility = View.GONE
				return@launch
			}
			container.visibility = View.VISIBLE

			renderRecentChapters(recentChapters)

			val items = mutableListOf<RecentItem>()
			for (sermon in recentSermons) {
				items.add(
					RecentItem("설교노트", sermon.title, sermon.createdAt) {
						startActivity(
							SermonDetailActivity.createIntent(
								requireContext(),
								sermon.id
							)
						)
					}
				)
			}
			for (memo in recentMemos) {
				val label =
					"${BibleBooks.nameOf(memo.bookId)} ${memo.chapter}:${memo.verse}  ${memo.text}"
				items.add(
					RecentItem("구절 메모", label, memo.updatedAt) {
						navigateToBible(memo.bookId, memo.chapter)
					}
				)
			}
			renderRecentItems(items.sortedByDescending { it.timestamp }.take(4))
		}
	}

	private fun renderRecentChapters(chapters: List<com.chan.bnote.data.mypage.RecentChapterView>) {
		val view = view ?: return
		val container = view.findViewById<LinearLayout>(R.id.container_recent_chapters)
		container.removeAllViews()

		for (chapterView in chapters) {
			val unit = BibleBooks.chapterUnit(chapterView.bookId)
			val chip = TextView(requireContext()).apply {
				text = "${BibleBooks.nameOf(chapterView.bookId)} ${chapterView.chapter}${unit}"
				textSize = 13f
				setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary))
				setPadding(dp(14), dp(8), dp(14), dp(8))
				background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_outline)
				isClickable = true
				isFocusable = true
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { marginEnd = dp(8) }
				setOnClickListener { navigateToBible(chapterView.bookId, chapterView.chapter) }
			}
			container.addView(chip)
		}
	}

	private fun renderRecentItems(items: List<RecentItem>) {
		val view = view ?: return
		val container = view.findViewById<LinearLayout>(R.id.container_recent_items)
		container.removeAllViews()

		for (item in items) {
			val row = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(16), dp(10), dp(16), dp(10))
				background = ContextCompat.getDrawable(
					requireContext(),
					android.R.drawable.list_selector_background
				)
				isClickable = true
				isFocusable = true
				setOnClickListener { item.onClick() }
			}
			val topRow = LinearLayout(requireContext()).apply {
				orientation = LinearLayout.HORIZONTAL
				gravity = Gravity.CENTER_VERTICAL
			}
			val typeView = TextView(requireContext()).apply {
				text = item.typeLabel
				textSize = 11f
				setTypeface(typeface, Typeface.BOLD)
				setTextColor(ContextCompat.getColor(requireContext(), R.color.brown_primary))
			}
			val dateView = TextView(requireContext()).apply {
				text = DateUtils.formatDateShort(item.timestamp)
				textSize = 11f
				setTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint))
				layoutParams =
					LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
				gravity = Gravity.END
			}
			topRow.addView(typeView)
			topRow.addView(dateView)

			val titleView = TextView(requireContext()).apply {
				text = item.title
				textSize = 14f
				maxLines = 1
				ellipsize = android.text.TextUtils.TruncateAt.END
				setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
				setPadding(0, dp(2), 0, 0)
			}

			row.addView(topRow)
			row.addView(titleView)
			container.addView(row)
		}
	}

	private fun navigateToBible(bookId: Int, chapter: Int) {
		(requireActivity() as? BibleNavigationHost)?.navigateToBibleChapter(bookId, chapter)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	override fun getTopBarConfig() = TopBarConfig(title = "마이페이지", showMenu = false)
}