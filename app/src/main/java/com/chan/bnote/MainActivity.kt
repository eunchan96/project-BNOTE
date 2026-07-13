package com.chan.bnote

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.data.AppSettings
import com.chan.bnote.ui.BibleNavigationHost
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.TopBarConfigListener
import com.chan.bnote.ui.bible.BibleFragment
import com.chan.bnote.ui.mypage.MyPageFragment
import com.chan.bnote.ui.sermon.SermonFragment

class MainActivity : AppCompatActivity(), TopBarConfigListener, BibleNavigationHost {

	private lateinit var textCurrentLocation: TextView
	private lateinit var btnTranslation: ImageView
	private lateinit var btnSearch: ImageView
	private lateinit var btnFavorites: ImageView
	private lateinit var btnMenu: ImageView
	private lateinit var btnPrevChapter: ImageView
	private lateinit var btnNextChapter: ImageView

	private lateinit var navBible: ImageView
	private lateinit var navSermon: ImageView
	private lateinit var navMyPage: ImageView

	private lateinit var btnAutoScroll: ImageView
	private lateinit var iconReadingPlanCheck: ImageView
	private lateinit var iconSermonIndicator: ImageView

	override fun onCreate(savedInstanceState: Bundle?) {
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

		if (savedInstanceState == null) {
			switchTab(BibleFragment(), navBible)
		} else {
			restoreCurrentTabUi() // 추가: recreate() 등으로 재생성됐을 때 현재 화면에 맞게 UI 동기화
		}
	}

	private fun bindTopBarViews() {
		textCurrentLocation = findViewById(R.id.text_current_location)
		btnTranslation = findViewById(R.id.btn_translation)
		btnSearch = findViewById(R.id.btn_search)
		btnFavorites = findViewById(R.id.btn_favorites)
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
		btnFavorites.setOnClickListener { currentHandler()?.onFavoritesClicked() }
		btnMenu.setOnClickListener { currentHandler()?.onMenuClicked() }
		btnAutoScroll.setOnClickListener { currentHandler()?.onAutoScrollButtonClicked() }
		iconReadingPlanCheck.setOnClickListener { currentHandler()?.onReadingPlanCheckClicked() }
		iconSermonIndicator.setOnClickListener { currentHandler()?.onSermonIconClicked() }
	}

	private fun setupBottomNavActions() {
		btnPrevChapter.setOnClickListener { currentHandler()?.onPrevChapterClicked() }
		btnNextChapter.setOnClickListener { currentHandler()?.onNextChapterClicked() }

		navBible.setOnClickListener { switchTab(BibleFragment(), navBible) }
		navSermon.setOnClickListener { switchTab(SermonFragment(), navSermon) }
		navMyPage.setOnClickListener { switchTab(MyPageFragment(), navMyPage) }
	}

	private fun switchTab(fragment: androidx.fragment.app.Fragment, selectedIcon: ImageView) {
		supportFragmentManager.beginTransaction()
			.replace(R.id.fragment_container, fragment)
			.commitNow()

		listOf(navBible, navSermon, navMyPage).forEach { icon ->
			icon.setColorFilter(
				if (icon == selectedIcon) getColor(R.color.bottom_nav_selected)
				else getColor(R.color.bottom_nav_unselected)
			)
		}

		(fragment as? TopBarActionHandler)?.let { onTopBarConfigChanged(it.getTopBarConfig()) }
	}

	private fun currentHandler(): TopBarActionHandler? {
		return supportFragmentManager.findFragmentById(R.id.fragment_container) as? TopBarActionHandler
	}

	override fun onTopBarConfigChanged(config: TopBarConfig) {
		textCurrentLocation.text = config.title
		btnTranslation.visibility = visible(config.showTranslationButton)
		btnSearch.visibility = visible(config.showSearch)
		btnFavorites.visibility = visible(config.showFavorites)
		btnMenu.visibility = visible(config.showMenu)
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
		switchTab(BibleFragment.newInstance(bookId, chapter), navBible)
	}

	private fun restoreCurrentTabUi() {
		val current = supportFragmentManager.findFragmentById(R.id.fragment_container) ?: return

		val selectedIcon = when (current) {
			is BibleFragment -> navBible
			is SermonFragment -> navSermon
			is MyPageFragment -> navMyPage
			else -> navBible
		}

		listOf(navBible, navSermon, navMyPage).forEach { icon ->
			icon.setColorFilter(
				if (icon == selectedIcon) getColor(R.color.bottom_nav_selected)
				else getColor(R.color.bottom_nav_unselected)
			)
		}

		(current as? TopBarActionHandler)?.let { onTopBarConfigChanged(it.getTopBarConfig()) }
	}
}