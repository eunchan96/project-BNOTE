package com.chan.bnote.ui

import com.chan.bnote.R

data class TopBarConfig(
	val title: String,
	val showTranslationButton: Boolean = false,
	val showSearch: Boolean = false,
	val showBookmarks: Boolean = false,
	val showMenu: Boolean = false,
	val menuIconRes: Int = R.drawable.ic_menu,
	val showChapterNav: Boolean = false,
	val showReadingPlanCheck: Boolean = false,
	val isChapterRead: Boolean = false,
	val showAutoScrollButton: Boolean = false,
	val isAutoScrolling: Boolean = false,
	val showSermonIcon: Boolean = false,
	val showApplicationButton: Boolean = false
)

interface TopBarActionHandler {
	fun getTopBarConfig(): TopBarConfig
	fun onLocationClicked() {}
	fun onTranslationClicked() {}
	fun onSearchClicked() {}
	fun onBookmarksClicked() {}
	fun onMenuClicked() {}
	fun onPrevChapterClicked() {}
	fun onNextChapterClicked() {}
	fun onAutoScrollButtonClicked() {}
	fun onReadingPlanCheckClicked() {}
	fun onSermonIconClicked() {}
	fun onApplicationButtonClicked() {}
}