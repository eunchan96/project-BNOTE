package com.chan.bnote

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.chan.bnote.data.AppSettings
import com.chan.bnote.ui.BibleNavigationHost
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.TopBarConfigListener
import com.chan.bnote.ui.bible.BibleFragment
import com.chan.bnote.ui.mypage.MyPageFragment
import com.chan.bnote.ui.sermon.SermonFragment

class MainActivity : AppCompatActivity(), TopBarConfigListener, BibleNavigationHost {

	companion object {
		const val EXTRA_NAVIGATE_BOOK_ID = "extra_navigate_book_id"
		const val EXTRA_NAVIGATE_CHAPTER = "extra_navigate_chapter"

		private const val TAG_BIBLE = "tab_bible"
		private const val TAG_SERMON = "tab_sermon"
		private const val TAG_MYPAGE = "tab_mypage"
	}

	private lateinit var textCurrentLocation: TextView
	private lateinit var btnTranslation: ImageView
	private lateinit var btnSearch: ImageView
	private lateinit var btnBookmarks: ImageView
	private lateinit var btnMenu: ImageView
	private lateinit var btnPrevChapter: ImageView
	private lateinit var btnNextChapter: ImageView

	private lateinit var navBible: ImageView
	private lateinit var navSermon: ImageView
	private lateinit var navMyPage: ImageView

	private lateinit var btnAutoScroll: ImageView
	private lateinit var iconReadingPlanCheck: ImageView
	private lateinit var iconSermonIndicator: ImageView

	// 탭 전환할 때마다 새로 만들지 않고, 만들어둔 인스턴스를 계속 재사용한다
	// (그래야 성경 읽던 위치/스크롤 등이 탭을 왔다갔다 해도 유지된다).
	private var bibleFragment: BibleFragment? = null
	private var sermonFragment: SermonFragment? = null
	private var myPageFragment: MyPageFragment? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)

		val darkMode = AppSettings.isDarkMode(this)
		AppCompatDelegate.setDefaultNightMode(
			if (darkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
		)
		if (AppSettings.isKeepScreenOn(this)) {
			window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}

		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		bindTopBarViews()
		bindBottomNavViews()
		setupTopBarActions()
		setupBottomNavActions()

		if (savedInstanceState != null) {
			// recreate() 등으로 재생성됐을 때, FragmentManager가 이미 복원해둔 인스턴스를 다시 참조만 한다
			bibleFragment = supportFragmentManager.findFragmentByTag(TAG_BIBLE) as? BibleFragment
			sermonFragment = supportFragmentManager.findFragmentByTag(TAG_SERMON) as? SermonFragment
			myPageFragment = supportFragmentManager.findFragmentByTag(TAG_MYPAGE) as? MyPageFragment

			// 다크모드 전환 등으로 인한 재생성(recreate())은 사용자가 보던 탭을 그대로 유지한다.
			val savedTab = AppSettings.getLastTab(this)
			when (savedTab) {
				TAG_SERMON -> switchToSermon()
				TAG_MYPAGE -> switchToMyPage()
				else -> switchToBible()
			}
		} else {
			// 완전히 새로 앱을 시작할 때는 마지막에 보던 탭이 무엇이었든 항상 성경 탭으로 시작한다.
			// (성경 탭 자체는 BibleFragment가 마지막으로 읽던 책/장을 스스로 복원한다.)
			switchToBible()
		}

		if (savedInstanceState == null) {
			handleNavigationIntent(intent)
		}
	}

	override fun onNewIntent(intent: android.content.Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		handleNavigationIntent(intent)
	}

	private fun handleNavigationIntent(intent: android.content.Intent) {
		if (!intent.hasExtra(EXTRA_NAVIGATE_BOOK_ID)) return
		val bookId = intent.getIntExtra(EXTRA_NAVIGATE_BOOK_ID, -1)
		val chapter = intent.getIntExtra(EXTRA_NAVIGATE_CHAPTER, -1)
		if (bookId != -1 && chapter != -1) {
			navigateToBibleChapter(bookId, chapter)
		}
	}

	private fun bindTopBarViews() {
		textCurrentLocation = findViewById(R.id.text_current_location)
		btnTranslation = findViewById(R.id.btn_translation)
		btnSearch = findViewById(R.id.btn_search)
		btnBookmarks = findViewById(R.id.btn_bookmarks)
		btnMenu = findViewById(R.id.btn_menu)
		btnAutoScroll = findViewById(R.id.btn_auto_scroll)
		iconReadingPlanCheck = findViewById(R.id.icon_reading_plan_check)
		iconSermonIndicator = findViewById(R.id.icon_sermon_indicator)
	}

	private fun bindBottomNavViews() {
		btnPrevChapter = findViewById(R.id.btn_prev_chapter)
		btnNextChapter = findViewById(R.id.btn_next_chapter)
		navSermon = findViewById(R.id.nav_sermon)
		navBible = findViewById(R.id.nav_bible)
		navMyPage = findViewById(R.id.nav_mypage)
	}

	private fun setupTopBarActions() {
		textCurrentLocation.setOnClickListener { currentHandler()?.onLocationClicked() }
		btnTranslation.setOnClickListener { currentHandler()?.onTranslationClicked() }
		btnSearch.setOnClickListener { currentHandler()?.onSearchClicked() }
		btnBookmarks.setOnClickListener { currentHandler()?.onBookmarksClicked() }
		btnMenu.setOnClickListener { currentHandler()?.onMenuClicked() }
		btnAutoScroll.setOnClickListener { currentHandler()?.onAutoScrollButtonClicked() }
		iconReadingPlanCheck.setOnClickListener { currentHandler()?.onReadingPlanCheckClicked() }
		iconSermonIndicator.setOnClickListener { currentHandler()?.onSermonIconClicked() }
	}

	private fun setupBottomNavActions() {
		btnPrevChapter.setOnClickListener { currentHandler()?.onPrevChapterClicked() }
		btnNextChapter.setOnClickListener { currentHandler()?.onNextChapterClicked() }

		navBible.setOnClickListener { switchToBible() }
		navSermon.setOnClickListener { switchToSermon() }
		navMyPage.setOnClickListener { switchToMyPage() }
	}

	private fun switchToBible() {
		val fragment = bibleFragment ?: BibleFragment().also { bibleFragment = it }
		switchTo(fragment, TAG_BIBLE, navBible)
	}

	private fun switchToSermon() {
		val fragment = sermonFragment ?: SermonFragment().also { sermonFragment = it }
		switchTo(fragment, TAG_SERMON, navSermon)
	}

	private fun switchToMyPage() {
		val fragment = myPageFragment ?: MyPageFragment().also { myPageFragment = it }
		switchTo(fragment, TAG_MYPAGE, navMyPage)
	}

	/**
	 * 탭 프래그먼트를 새로 만들지 않고(처음 한 번만 add), 이후엔 hide/show만 해서
	 * 인스턴스 상태(성경 읽던 위치, 스크롤 등)가 탭을 넘나들어도 유지되게 한다.
	 */
	private fun switchTo(fragment: Fragment, tag: String, selectedIcon: ImageView) {
		AppSettings.setLastTab(this, tag)
		val transaction = supportFragmentManager.beginTransaction()
		if (!fragment.isAdded) {
			transaction.add(R.id.fragment_container, fragment, tag)
		}
		listOf(bibleFragment, sermonFragment, myPageFragment).forEach { other ->
			if (other != null && other !== fragment && other.isAdded) {
				transaction.hide(other)
			}
		}
		transaction.show(fragment)
		transaction.commitNow()

		updateNavSelection(selectedIcon)
		(fragment as? TopBarActionHandler)?.let { onTopBarConfigChanged(it.getTopBarConfig()) }
	}

	private fun updateNavSelection(selectedIcon: ImageView) {
		listOf(navBible, navSermon, navMyPage).forEach { icon ->
			icon.setColorFilter(
				if (icon == selectedIcon) getColor(R.color.bottom_nav_selected)
				else getColor(R.color.bottom_nav_unselected)
			)
		}
	}

	private fun currentHandler(): TopBarActionHandler? {
		return listOfNotNull(bibleFragment, sermonFragment, myPageFragment)
			.firstOrNull { it.isAdded && !it.isHidden } as? TopBarActionHandler
	}

	override fun onTopBarConfigChanged(config: TopBarConfig) {
		textCurrentLocation.text = config.title
		btnTranslation.visibility = visible(config.showTranslationButton)
		btnSearch.visibility = visible(config.showSearch)
		btnBookmarks.visibility = visible(config.showBookmarks)
		btnMenu.visibility = visible(config.showMenu)
		btnMenu.setImageResource(config.menuIconRes)
		btnPrevChapter.visibility = visible(config.showChapterNav)
		btnNextChapter.visibility = visible(config.showChapterNav)

		iconReadingPlanCheck.visibility = visible(config.showReadingPlanCheck)
		iconReadingPlanCheck.alpha = if (config.isChapterRead) 1f else 0.4f

		btnAutoScroll.visibility = visible(config.showAutoScrollButton)
		btnAutoScroll.setImageResource(if (config.isAutoScrolling) R.drawable.ic_pause else R.drawable.ic_play)

		iconSermonIndicator.visibility = visible(config.showSermonIcon)
	}

	private fun visible(show: Boolean) =
		if (show) android.view.View.VISIBLE else android.view.View.GONE

	override fun navigateToBibleChapter(bookId: Int, chapter: Int) {
		val existing = bibleFragment
		if (existing != null && existing.isAdded) {
			switchTo(existing, TAG_BIBLE, navBible)
			existing.navigateTo(bookId, chapter)
		} else {
			val fragment = BibleFragment.newInstance(bookId, chapter)
			bibleFragment = fragment
			switchTo(fragment, TAG_BIBLE, navBible)
		}
	}

}