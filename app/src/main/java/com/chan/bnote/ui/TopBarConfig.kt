package com.chan.bnote.ui

data class TopBarConfig(
	val title: String,
	val showTranslationButton: Boolean = false,
	val showSearch: Boolean = false,
	val showFavorites: Boolean = false,
	val showMenu: Boolean = false,
	val showChapterNav: Boolean = false,
	val showReadingPlanCheck: Boolean = false,
	val isChapterRead: Boolean = false,       // 추가: 현재 장이 읽음 처리됐는지
	val showAutoScrollButton: Boolean = false, // 추가: 상단바 스크롤 버튼 표시 여부
	val isAutoScrolling: Boolean = false       // 추가: 재생/정지 아이콘 전환용
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
	fun onAutoScrollButtonClicked() {}   // 추가
	fun onReadingPlanCheckClicked() {}   // 추가
}