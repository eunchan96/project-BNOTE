package com.chan.bnote.ui

data class TopBarConfig(
	val title: String,
	val showTranslationButton: Boolean = false,
	val showSearch: Boolean = false,
	val showFavorites: Boolean = false,
	val showMenu: Boolean = false,
	val showChapterNav: Boolean = false // 하단바 좌측 이전/다음 장 버튼
)

interface TopBarActionHandler {
	fun getTopBarConfig(): TopBarConfig
	fun onLocationClicked() {}
	fun onTranslationClicked() {}
	fun onSearchClicked() {}
	fun onFavoritesClicked() {}
	fun onMenuClicked() {}
	fun onPrevChapterClicked() {}
	fun onNextChapterClicked() {}
}