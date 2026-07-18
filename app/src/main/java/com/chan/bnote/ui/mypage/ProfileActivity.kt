package com.chan.bnote.ui.mypage

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.profile.ProfileDisplay
import kotlinx.coroutines.launch
import java.util.Calendar

/** 내 정보(프로필 요약 + 수정 진입) + 나의 신앙 기록(통독 현황·활동 통계)을 함께 보여주는 화면. */
class ProfileActivity : AppCompatActivity() {

	private data class StatItem(val label: String, val value: String)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_profile)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "내 정보"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		findViewById<TextView>(R.id.btn_edit_profile).setOnClickListener {
			startActivity(Intent(this, ProfileEditActivity::class.java))
		}
		findViewById<ImageView>(R.id.img_profile_photo).loadProfilePhoto(null)
	}

	override fun onResume() {
		super.onResume()
		// 정보 수정 화면에서 돌아왔을 때 최신 값을 반영하기 위해 매번 다시 불러온다.
		loadProfile()
		loadStats()
	}

	private fun loadProfile() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val profile = db.userProfileDao().get()

			findViewById<TextView>(R.id.text_profile_name).setNameWithPosition(
				ProfileDisplay.nameText(profile), ProfileDisplay.positionText(profile)
			)
			findViewById<TextView>(R.id.text_profile_meta).text =
				ProfileDisplay.profilePageMetaText(profile)
			findViewById<ImageView>(R.id.img_profile_photo).loadProfilePhoto(profile?.photoPath)
		}
	}

	private fun loadStats() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			// 통독 현황
			val maxChapterByBook = (1..66).associateWith { bookId ->
				db.bibleDao().getMaxChapter("NKRV", bookId)
			}
			val totalChapters = maxChapterByBook.values.sum()
			val readList = db.readingProgressDao().getAll()
			val totalRead = readList.size
			val percent = if (totalChapters > 0) (totalRead * 100 / totalChapters) else 0

			val (currentStreak, longestStreak) = calculateStreak(readList.map { it.readAt })

			// 나의 기록
			val highlightCount = db.partialHighlightDao().countAll()
			val mostUsedColor = db.partialHighlightDao().getMostUsedColor()
			val bookmarkCount = db.bookmarkDao().countBookmarks()
			val scrapCount = db.scrapDao().countAllScraps()
			val verseMemoCount = db.verseMemoDao().count()
			val wordMemoCount = db.wordMemoDao().count()
			val sermonCount = db.sermonDao().count()

			renderReadingStatus(totalRead, totalChapters, percent, currentStreak, longestStreak)
			renderStatGrid(
				listOf(
					StatItem("하이라이트", "${highlightCount}개"),
					StatItem("북마크", "${bookmarkCount}개"),
					StatItem("스크랩", "${scrapCount}개"),
					StatItem("구절 메모", "${verseMemoCount}개"),
					StatItem("단어 메모", "${wordMemoCount}개"),
					StatItem("설교노트", "${sermonCount}개")
				)
			)
			renderTopHighlightColor(mostUsedColor?.colorHex, mostUsedColor?.count)
		}
	}

	private fun renderReadingStatus(
		totalRead: Int, totalChapters: Int, percent: Int, currentStreak: Int, longestStreak: Int
	) {
		findViewById<TextView>(R.id.text_reading_summary).text =
			"전체 $totalRead / ${totalChapters}장 읽음 ($percent%)"
		findViewById<ProgressBar>(R.id.progress_reading_percent).progress = percent

		val streakText = findViewById<TextView>(R.id.text_streak)
		streakText.text = if (currentStreak > 0) {
			"🔥 연속 ${currentStreak}일째 읽는 중 (최고 기록 ${longestStreak}일)"
		} else if (longestStreak > 0) {
			"오늘부터 다시 시작해볼까요? (최고 기록 ${longestStreak}일)"
		} else {
			"오늘부터 통독을 시작해보세요!"
		}
	}

	private fun renderStatGrid(items: List<StatItem>) {
		val container = findViewById<LinearLayout>(R.id.container_stat_grid)
		container.removeAllViews()

		items.chunked(2).forEach { rowItems ->
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { bottomMargin = dp(8) }
			}
			rowItems.forEach { item -> row.addView(buildStatCard(item)) }
			// 항목이 홀수로 끝나면 남는 칸만큼 빈 스페이서를 넣어서 카드 너비가 늘어나지 않게 한다.
			if (rowItems.size == 1) {
				row.addView(View(this).apply {
					layoutParams = LinearLayout.LayoutParams(0, 0, 1f).apply {
						marginStart = dp(4)
						marginEnd = dp(4)
					}
				})
			}
			container.addView(row)
		}
	}

	private fun buildStatCard(item: StatItem): View {
		val card = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			gravity = Gravity.CENTER_HORIZONTAL
			background = ContextCompat.getDrawable(this@ProfileActivity, R.drawable.bg_stat_card)
			setPadding(dp(12), dp(16), dp(12), dp(16))
			layoutParams = LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
			).apply { marginStart = dp(4); marginEnd = dp(4) }
		}

		val valueView = TextView(this).apply {
			text = item.value
			textSize = 20f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.brown_primary))
		}
		val labelView = TextView(this).apply {
			text = item.label
			textSize = 13f
			setTextColor(ContextCompat.getColor(this@ProfileActivity, R.color.text_secondary))
			setPadding(0, dp(4), 0, 0)
		}

		card.addView(valueView)
		card.addView(labelView)
		return card
	}

	private fun renderTopHighlightColor(colorHex: String?, count: Int?) {
		val container = findViewById<LinearLayout>(R.id.container_top_highlight_color)
		if (colorHex == null || count == null || count <= 0) {
			container.visibility = View.GONE
			return
		}
		container.visibility = View.VISIBLE

		val swatch = findViewById<View>(R.id.view_top_highlight_color_swatch)
		val drawable = GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(Color.parseColor(colorHex))
			setStroke(1, Color.parseColor("#55000000"))
		}
		swatch.background = drawable

		findViewById<TextView>(R.id.text_top_highlight_color).text =
			"가장 많이 쓴 하이라이트 색 (${count}개)"
	}

	/** [readAtList] 각 타임스탬프를 날짜 단위로 묶어 (현재 연속 일수, 최고 연속 일수)를 계산한다. */
	private fun calculateStreak(readAtList: List<Long>): Pair<Int, Int> {
		if (readAtList.isEmpty()) return 0 to 0

		val days = readAtList.map { startOfDay(it) }.toSortedSet().toList()

		var longest = 1
		var current = 1
		for (i in 1 until days.size) {
			if (days[i] == addDays(days[i - 1], 1)) {
				current += 1
			} else {
				if (current > longest) longest = current
				current = 1
			}
		}
		if (current > longest) longest = current

		val today = startOfDay(System.currentTimeMillis())
		val yesterday = addDays(today, -1)
		var currentStreak = 0
		val lastDay = days.last()
		if (lastDay == today || lastDay == yesterday) {
			currentStreak = 1
			var cursor = lastDay
			for (i in days.size - 2 downTo 0) {
				if (days[i] == addDays(cursor, -1)) {
					currentStreak += 1
					cursor = days[i]
				} else {
					break
				}
			}
		}

		return currentStreak to longest
	}

	private fun startOfDay(timestamp: Long): Long {
		val cal = Calendar.getInstance()
		cal.timeInMillis = timestamp
		cal.set(Calendar.HOUR_OF_DAY, 0)
		cal.set(Calendar.MINUTE, 0)
		cal.set(Calendar.SECOND, 0)
		cal.set(Calendar.MILLISECOND, 0)
		return cal.timeInMillis
	}

	private fun addDays(timestamp: Long, days: Int): Long {
		val cal = Calendar.getInstance()
		cal.timeInMillis = timestamp
		cal.add(Calendar.DAY_OF_YEAR, days)
		return cal.timeInMillis
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}