data class TopBarConfig(
	val title: String,
	val showTranslationButton: Boolean = false,
	val showSearch: Boolean = false,
	val showFavorites: Boolean = false,
	val showMenu: Boolean = false,
	val showChapterNav: Boolean = false,
	val showReadingPlanCheck: Boolean = false,
	val isChapterRead: Boolean = false,
	val showAutoScrollButton: Boolean = false,
	val isAutoScrolling: Boolean = false,
	val showSermonIcon: Boolean = false
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
	fun onAutoScrollButtonClicked() {}
	fun onReadingPlanCheckClicked() {}
	fun onSermonIconClicked() {}
}