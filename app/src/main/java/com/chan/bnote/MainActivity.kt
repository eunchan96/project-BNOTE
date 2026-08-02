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
		const val EXTRA_NAVIGATE_VERSE = "extra_navigate_verse"

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
	private lateinit var btnGoToApplication: ImageView

	private lateinit var navBible: ImageView
	private lateinit var navSermon: ImageView
	private lateinit var navMyPage: ImageView

	private lateinit var btnAutoScroll: ImageView
	private lateinit var iconReadingPlanCheck: ImageView
	private lateinit var iconSermonIndicator: ImageView

	private lateinit var topBarScroll: android.widget.HorizontalScrollView
	private lateinit var topBar: android.widget.LinearLayout
	private lateinit var topBarSpacer: android.view.View

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
			val navBookId = intent.getIntExtra(EXTRA_NAVIGATE_BOOK_ID, -1)
			val navChapter = intent.getIntExtra(EXTRA_NAVIGATE_CHAPTER, -1)
			if (navBookId != -1 && navChapter != -1) {
				val navVerse = intent.getIntExtra(EXTRA_NAVIGATE_VERSE, -1).takeIf { it != -1 }
				// 알림을 눌러서 완전히 새로 켜진 경우: switchToBible()로 먼저 성경 탭을 만들고 나서
				// 바로 이어서 navigateToBibleChapter()를 부르면, 앞의 트랜잭션이 아직 반영되기 전이라
				// isAdded가 false로 나와서 성경 프래그먼트가 중복으로 만들어지며 튕기는 문제가 있었다.
				// 그래서 이 경우엔 switchToBible()을 건너뛰고 바로 이동한다(그 안에서 알아서 만든다).
				navigateToBibleChapter(navBookId, navChapter, navVerse)
			} else {
				// 완전히 새로 앱을 시작할 때는 마지막에 보던 탭이 무엇이었든 항상 성경 탭으로 시작한다.
				// (성경 탭 자체는 BibleFragment가 마지막으로 읽던 책/장을 스스로 복원한다.)
				switchToBible()
			}
		}
	}

	override fun onNewIntent(intent: android.content.Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		handleNavigationIntent(intent)
	}

	override fun onResume() {
		super.onResume()
		// 설정 화면에서 "화면 켜짐 유지"를 바꾸고 돌아왔을 수도 있으니 다시 확인해서 반영한다.
		if (AppSettings.isKeepScreenOn(this)) {
			window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		} else {
			window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}
	}

	private fun handleNavigationIntent(intent: android.content.Intent) {
		if (!intent.hasExtra(EXTRA_NAVIGATE_BOOK_ID)) return
		val bookId = intent.getIntExtra(EXTRA_NAVIGATE_BOOK_ID, -1)
		val chapter = intent.getIntExtra(EXTRA_NAVIGATE_CHAPTER, -1)
		if (bookId != -1 && chapter != -1) {
			val verse = intent.getIntExtra(EXTRA_NAVIGATE_VERSE, -1).takeIf { it != -1 }
			navigateToBibleChapter(bookId, chapter, verse)
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
		topBarScroll = findViewById(R.id.top_bar_scroll)
		topBar = findViewById(R.id.top_bar)
		topBarSpacer = findViewById(R.id.view_top_bar_spacer)

		// 화면 크기가 처음 확정되거나(첫 레이아웃) 회전 등으로 바뀌었을 때만 다시 계산하면 된다.
		// 탭이 바뀔 때(onTopBarConfigChanged)는 adjustTopBarSpacer()를 동기적으로 직접 불러서
		// 처리하므로, 여기서는 "스크롤뷰 자체 크기"가 실제로 달라졌을 때만 반응한다.
		topBarScroll.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
			if (right - left != oldRight - oldLeft) adjustTopBarSpacer()
		}
	}

	private fun bindBottomNavViews() {
		btnPrevChapter = findViewById(R.id.btn_prev_chapter)
		btnNextChapter = findViewById(R.id.btn_next_chapter)
		btnGoToApplication = findViewById(R.id.btn_go_to_application)
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
		btnGoToApplication.setOnClickListener { currentHandler()?.onApplicationButtonClicked() }

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
		btnGoToApplication.visibility = visible(config.showApplicationButton)

		iconReadingPlanCheck.visibility = visible(config.showReadingPlanCheck)
		iconReadingPlanCheck.alpha = if (config.isChapterRead) 1f else 0.4f

		btnAutoScroll.visibility = visible(config.showAutoScrollButton)
		btnAutoScroll.setImageResource(if (config.isAutoScrolling) R.drawable.ic_pause else R.drawable.ic_play)

		iconSermonIndicator.visibility = visible(config.showSermonIcon)

		adjustTopBarSpacer()
	}

	/** 상단바 안 버튼들의 실제 너비 합이 화면(스크롤뷰) 너비 안에 다 들어가면, 오른쪽 스페이서가
	 * 남는 공간을 채워서 지금까지처럼 오른쪽 끝에 버튼들이 붙어 보이게 한다. 책 이름이 길거나
	 * (예: "데살로니가전서") 켜진 아이콘이 많아서 다 안 들어갈 때만 스페이서 너비를 0으로 줄여서,
	 * 버튼들이 서로 붙은 채로 가로 스크롤되게 한다.
	 *
	 * 실제 레이아웃이 끝나길(다음 프레임) 기다리지 않고, 각 버튼의 고정 크기(layoutParams,
	 * 아이콘들은 전부 dp 고정값)와 제목 텍스트의 실측 너비(Paint.measureText)만으로 지금 이
	 * 함수 안에서 바로 계산한다 — 그래야 탭을 바꿀 때 이전 스페이서 크기로 잠깐 그려졌다가
	 * 다음 프레임에 스냅되는 부자연스러움이 없다. */
	private fun adjustTopBarSpacer() {
		val scrollViewWidth = topBarScroll.width
		if (scrollViewWidth <= 0) return // 아직 첫 레이아웃 전. 크기가 확정되면 위 리스너가 다시 불러준다.

		var contentWidth = topBar.paddingStart + topBar.paddingEnd
		for (i in 0 until topBar.childCount) {
			val child = topBar.getChildAt(i)
			if (child === topBarSpacer) continue
			contentWidth += topBarChildWidth(child)
		}

		val remaining = scrollViewWidth - contentWidth
		val newSpacerWidth = remaining.coerceAtLeast(0)
		val params = topBarSpacer.layoutParams
		if (params.width != newSpacerWidth) {
			params.width = newSpacerWidth
			topBarSpacer.layoutParams = params
		}
		// 다 들어가는 경우엔 혹시 이전에 스크롤돼 있던 위치가 있어도 원위치로 되돌린다.
		if (remaining >= 0) topBarScroll.scrollTo(0, 0)
	}

	/** child가 GONE이면 0. 그 외엔 (margin 포함) 실제로 차지하는 너비.
	 * 아이콘들은 전부 layout_width가 dp 고정값이라 layoutParams.width를 그대로 쓰면 되고,
	 * 제목(text_current_location)만 wrap_content라 Paint로 지금 텍스트의 실제 너비를 잰다. */
	private fun topBarChildWidth(child: android.view.View): Int {
		if (child.visibility == android.view.View.GONE) return 0
		val lp = child.layoutParams
		val margins = lp as? android.view.ViewGroup.MarginLayoutParams
		val marginSum = (margins?.marginStart ?: 0) + (margins?.marginEnd ?: 0)
		val intrinsicWidth =
			if (child is TextView && lp.width == android.view.ViewGroup.LayoutParams.WRAP_CONTENT) {
				kotlin.math.ceil(
					child.paint.measureText(child.text?.toString().orEmpty()).toDouble()
				).toInt() +
						child.paddingStart + child.paddingEnd
			} else {
				lp.width
			}
		return intrinsicWidth + marginSum
	}

	private fun visible(show: Boolean) =
		if (show) android.view.View.VISIBLE else android.view.View.GONE

	override fun navigateToBibleChapter(bookId: Int, chapter: Int, verse: Int?) {
		val existing = bibleFragment
		if (existing != null && existing.isAdded) {
			switchTo(existing, TAG_BIBLE, navBible)
			existing.navigateTo(bookId, chapter, verse)
		} else {
			val fragment = BibleFragment.newInstance(bookId, chapter, verse)
			bibleFragment = fragment
			switchTo(fragment, TAG_BIBLE, navBible)
		}
	}

}