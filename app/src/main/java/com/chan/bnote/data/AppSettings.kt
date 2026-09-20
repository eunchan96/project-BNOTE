package com.chan.bnote.data

import android.content.Context

object AppSettings {
	private const val KEY_PRIMARY_TRANSLATION = "primary_translation"
	private const val KEY_SECONDARY_TRANSLATION = "secondary_translation" // 없으면 빈 문자열

	private const val PREF_NAME = "bnote_settings"
	private const val KEY_FONT_SIZE = "font_size_sp"
	private const val KEY_DARK_MODE = "dark_mode"
	private const val KEY_READING_PLAN_ENABLED = "reading_plan_enabled"
	private const val KEY_AUTO_SCROLL_ENABLED = "auto_scroll_enabled"

	private const val KEY_PREACHER_SORT_MODE = "preacher_sort_mode" // "NAME" or "CUSTOM"
	private const val KEY_PREACHER_CUSTOM_ORDER = "preacher_custom_order" // 쉼표 구분 문자열
	private const val KEY_SERMON_SORT_MODE = "sermon_sort_mode" // "DATE" or "BIBLE" (설교자 탭)
	private const val KEY_CALENDAR_SORT_MODE =
		"sermon_calendar_sort_mode" // "BIBLE"|"CATEGORY"|"ADDED"
	private const val KEY_BY_BOOK_SORT_MODE =
		"sermon_by_book_sort_mode" // "BIBLE"|"DATE"|"CATEGORY"|"ADDED"
	private const val KEY_BIBLE_SEARCH_HISTORY = "bible_search_history" // 구분자로 이어붙인 문자열, 최신순
	private const val KEY_SERMON_SEARCH_HISTORY = "sermon_search_history" // 구분자로 이어붙인 문자열, 최신순
	private const val SEARCH_HISTORY_DELIMITER = "\u001E"
	private const val MAX_SEARCH_HISTORY = 20

	private const val DEFAULT_FONT_SIZE = 16
	const val MIN_FONT_SIZE = 12
	const val MAX_FONT_SIZE = 28

	private const val KEY_SCROLL_SPEED = "scroll_speed" // 1(느림)~5(빠름)
	private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
	private const val KEY_CHAPTER_SWIPE_ENABLED = "chapter_swipe_enabled"
	private const val KEY_BIBLE_SCROLLBAR_ENABLED = "bible_scrollbar_enabled"
	private const val KEY_READING_CHECK_BOTTOM_BUTTON = "reading_check_bottom_button"
	private const val KEY_COPY_INCLUDE_SECONDARY = "copy_include_secondary"
	private const val KEY_ACTIVE_COPY_FORMAT = "active_copy_format_json"
	private const val KEY_LAST_READ_BOOK_ID = "last_read_book_id"
	private const val KEY_LAST_READ_CHAPTER = "last_read_chapter"
	private const val KEY_LAST_TAB = "last_tab"

	private const val KEY_DAILY_VERSE_NOTI_ENABLED = "daily_verse_noti_enabled"
	private const val KEY_DAILY_VERSE_NOTI_HOUR = "daily_verse_noti_hour"
	private const val KEY_DAILY_VERSE_NOTI_MINUTE = "daily_verse_noti_minute"
	private const val KEY_READING_REMINDER_ENABLED = "reading_reminder_enabled"
	private const val KEY_READING_REMINDER_HOUR = "reading_reminder_hour"
	private const val KEY_READING_REMINDER_MINUTE = "reading_reminder_minute"
	private const val KEY_SERMON_UNCATEGORIZED_POSITION = "sermon_uncategorized_position"
	private const val KEY_APPLICATION_UNCATEGORIZED_POSITION = "application_uncategorized_position"
	private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
	private const val KEY_AUTO_BACKUP_INTERVAL_DAYS = "auto_backup_interval_days"
	private const val KEY_AUTO_BACKUP_LAST_CHECK = "auto_backup_last_check"
	private const val KEY_AUTO_BACKUP_FOLDER_URI = "auto_backup_folder_uri"

	fun getPrimaryTranslation(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_PRIMARY_TRANSLATION, "NKRV") ?: "NKRV"
	}

	fun setPrimaryTranslation(context: Context, code: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_PRIMARY_TRANSLATION, code).apply()
	}

	fun getSecondaryTranslation(context: Context): String? {
		val value = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_SECONDARY_TRANSLATION, "") ?: ""
		return value.ifBlank { null }
	}

	fun setSecondaryTranslation(context: Context, code: String?) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_SECONDARY_TRANSLATION, code ?: "").apply()
	}

	fun getFontSize(context: Context): Int {
		val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		return prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
	}

	fun setFontSize(context: Context, size: Int) {
		val clamped = size.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putInt(KEY_FONT_SIZE, clamped).apply()
	}

	fun isDarkMode(context: Context): Boolean {
		val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		return prefs.getBoolean(KEY_DARK_MODE, false)
	}

	fun setDarkMode(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
	}

	fun isReadingPlanEnabled(context: Context): Boolean {
		val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		return prefs.getBoolean(KEY_READING_PLAN_ENABLED, false)
	}

	fun setReadingPlanEnabled(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_READING_PLAN_ENABLED, enabled).apply()
	}

	fun isAutoScrollEnabled(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_AUTO_SCROLL_ENABLED, false)
	}

	fun setAutoScrollEnabled(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_AUTO_SCROLL_ENABLED, enabled).apply()
	}

	fun getPreacherSortMode(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_PREACHER_SORT_MODE, "NAME") ?: "NAME"
	}

	fun setPreacherSortMode(context: Context, mode: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_PREACHER_SORT_MODE, mode).apply()
	}

	fun getPreacherCustomOrderIds(context: Context): List<Long> {
		val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_PREACHER_CUSTOM_ORDER, "") ?: ""
		return if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.toLongOrNull() }
	}

	fun setPreacherCustomOrderIds(context: Context, ids: List<Long>) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_PREACHER_CUSTOM_ORDER, ids.joinToString(",")).apply()
	}

	// --- 성경 검색 · 설교 검색 기록 (최신순, 최대 MAX_SEARCH_HISTORY개, 중복 없음) ---

	fun getBibleSearchHistory(context: Context): List<String> =
		getSearchHistory(context, KEY_BIBLE_SEARCH_HISTORY)

	fun addBibleSearchHistory(context: Context, keyword: String) =
		addSearchHistory(context, KEY_BIBLE_SEARCH_HISTORY, keyword)

	fun removeBibleSearchHistory(context: Context, keyword: String) =
		removeSearchHistory(context, KEY_BIBLE_SEARCH_HISTORY, keyword)

	fun clearBibleSearchHistory(context: Context) =
		saveSearchHistory(context, KEY_BIBLE_SEARCH_HISTORY, emptyList())

	fun getSermonSearchHistory(context: Context): List<String> =
		getSearchHistory(context, KEY_SERMON_SEARCH_HISTORY)

	fun addSermonSearchHistory(context: Context, keyword: String) =
		addSearchHistory(context, KEY_SERMON_SEARCH_HISTORY, keyword)

	fun removeSermonSearchHistory(context: Context, keyword: String) =
		removeSearchHistory(context, KEY_SERMON_SEARCH_HISTORY, keyword)

	fun clearSermonSearchHistory(context: Context) =
		saveSearchHistory(context, KEY_SERMON_SEARCH_HISTORY, emptyList())

	private fun getSearchHistory(context: Context, key: String): List<String> {
		val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(key, "") ?: ""
		return if (raw.isBlank()) emptyList() else raw.split(SEARCH_HISTORY_DELIMITER)
			.filter { it.isNotBlank() }
	}

	private fun addSearchHistory(context: Context, key: String, keyword: String) {
		val trimmed = keyword.trim()
		if (trimmed.isEmpty()) return
		val updated = mutableListOf(trimmed)
		updated.addAll(getSearchHistory(context, key).filter { it != trimmed })
		saveSearchHistory(context, key, updated.take(MAX_SEARCH_HISTORY))
	}

	private fun removeSearchHistory(context: Context, key: String, keyword: String) {
		saveSearchHistory(context, key, getSearchHistory(context, key).filter { it != keyword })
	}

	private fun saveSearchHistory(context: Context, key: String, history: List<String>) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(key, history.joinToString(SEARCH_HISTORY_DELIMITER))
			.apply()
	}

	fun getSermonSortMode(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_SERMON_SORT_MODE, "DATE") ?: "DATE"
	}

	fun setSermonSortMode(context: Context, mode: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_SERMON_SORT_MODE, mode).apply()
	}

	fun getCalendarSortMode(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_CALENDAR_SORT_MODE, "ADDED") ?: "ADDED"
	}

	fun setCalendarSortMode(context: Context, mode: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_CALENDAR_SORT_MODE, mode).apply()
	}

	fun getByBookSortMode(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_BY_BOOK_SORT_MODE, "ADDED") ?: "ADDED"
	}

	fun setByBookSortMode(context: Context, mode: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_BY_BOOK_SORT_MODE, mode).apply()
	}

	fun getScrollSpeed(context: Context): Int {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getInt(KEY_SCROLL_SPEED, 3)
	}

	fun setScrollSpeed(context: Context, speed: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putInt(KEY_SCROLL_SPEED, speed.coerceIn(1, 5)).apply()
	}

	fun isKeepScreenOn(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_KEEP_SCREEN_ON, false)
	}

	fun setKeepScreenOn(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_KEEP_SCREEN_ON, enabled).apply()
	}

	fun isChapterSwipeEnabled(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_CHAPTER_SWIPE_ENABLED, false)
	}

	fun setChapterSwipeEnabled(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_CHAPTER_SWIPE_ENABLED, enabled).apply()
	}

	/** 성경 탭 오른쪽에 뜨는 스크롤바를 보여줄지 여부. 기본은 켜짐(true). */
	fun isBibleScrollbarVisible(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_BIBLE_SCROLLBAR_ENABLED, true)
	}

	fun setBibleScrollbarVisible(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_BIBLE_SCROLLBAR_ENABLED, enabled).apply()
	}

	/** 기본은 상단바 체크 아이콘, 켜면 성경 탭 하단 버튼으로 표시한다. */
	fun isReadingCheckBottomButtonMode(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_READING_CHECK_BOTTOM_BUTTON, false)
	}

	fun setReadingCheckBottomButtonMode(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_READING_CHECK_BOTTOM_BUTTON, enabled).apply()
	}

	fun isCopyIncludeSecondary(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_COPY_INCLUDE_SECONDARY, false)
	}

	fun setCopyIncludeSecondary(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_COPY_INCLUDE_SECONDARY, enabled).apply()
	}

	/** 지금 복사할 때 실제로 쓰는 형식. 저장된 프리셋 중 하나일 수도, 임시로 만져본 값일 수도 있다. */
	fun getActiveCopyFormat(context: Context): com.chan.bnote.data.mypage.CopyFormatConfig {
		val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_ACTIVE_COPY_FORMAT, null)
			?: return com.chan.bnote.data.mypage.CopyFormatConfig()
		return com.chan.bnote.data.mypage.CopyFormatConfig.fromJson(raw)
	}

	fun setActiveCopyFormat(context: Context, config: com.chan.bnote.data.mypage.CopyFormatConfig) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_ACTIVE_COPY_FORMAT, config.toJson()).apply()
	}

	/** 마지막으로 읽던 위치 - 앱을 완전히 껐다 켜도 이 위치로 열리게 하기 위함. */
	fun getLastReadBookId(context: Context): Int {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getInt(KEY_LAST_READ_BOOK_ID, 1)
	}

	fun getLastReadChapter(context: Context): Int {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getInt(KEY_LAST_READ_CHAPTER, 1)
	}

	fun setLastRead(context: Context, bookId: Int, chapter: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putInt(KEY_LAST_READ_BOOK_ID, bookId)
			.putInt(KEY_LAST_READ_CHAPTER, chapter)
			.apply()
	}

	/** 마지막으로 보던 하단 탭 - MainActivity가 다크모드 전환 등으로 재생성돼도 이 탭으로 열리게 하기 위함. */
	fun getLastTab(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_LAST_TAB, "tab_bible") ?: "tab_bible"
	}

	fun setLastTab(context: Context, tab: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_LAST_TAB, tab).apply()
	}

	// --- 알림/리마인더 ---

	fun isDailyVerseNotiEnabled(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_DAILY_VERSE_NOTI_ENABLED, false)
	}

	fun setDailyVerseNotiEnabled(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_DAILY_VERSE_NOTI_ENABLED, enabled).apply()
	}

	/** 매일 말씀 알림 시간. 기본값 오전 8시. */
	fun getDailyVerseNotiTime(context: Context): Pair<Int, Int> {
		val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		return prefs.getInt(KEY_DAILY_VERSE_NOTI_HOUR, 8) to prefs.getInt(
			KEY_DAILY_VERSE_NOTI_MINUTE,
			0
		)
	}

	fun setDailyVerseNotiTime(context: Context, hour: Int, minute: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putInt(KEY_DAILY_VERSE_NOTI_HOUR, hour)
			.putInt(KEY_DAILY_VERSE_NOTI_MINUTE, minute)
			.apply()
	}

	fun isReadingReminderEnabled(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_READING_REMINDER_ENABLED, false)
	}

	fun setReadingReminderEnabled(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_READING_REMINDER_ENABLED, enabled).apply()
	}

	/** 통독 리마인더 시간. 기본값 오후 9시. */
	fun getReadingReminderTime(context: Context): Pair<Int, Int> {
		val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		return prefs.getInt(KEY_READING_REMINDER_HOUR, 21) to prefs.getInt(
			KEY_READING_REMINDER_MINUTE,
			0
		)
	}

	fun setReadingReminderTime(context: Context, hour: Int, minute: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putInt(KEY_READING_REMINDER_HOUR, hour)
			.putInt(KEY_READING_REMINDER_MINUTE, minute)
			.apply()
	}

	/** 설교 카테고리 관리에서 "미분류" 줄을 순서 목록의 몇 번째에 뒀는지(0부터). 기본은 맨 끝. */
	fun getSermonUncategorizedPosition(context: Context): Int {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getInt(KEY_SERMON_UNCATEGORIZED_POSITION, Int.MAX_VALUE)
	}

	fun setSermonUncategorizedPosition(context: Context, position: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putInt(KEY_SERMON_UNCATEGORIZED_POSITION, position).apply()
	}

	/** 적용 카테고리 관리에서 "미분류" 줄 위치(0부터). 기본은 맨 끝. */
	fun getApplicationUncategorizedPosition(context: Context): Int {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getInt(KEY_APPLICATION_UNCATEGORIZED_POSITION, Int.MAX_VALUE)
	}

	fun setApplicationUncategorizedPosition(context: Context, position: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putInt(KEY_APPLICATION_UNCATEGORIZED_POSITION, position).apply()
	}

	// --- 자동 데이터 내보내기 ---

	fun isAutoBackupEnabled(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
	}

	/** 켤 때는 그 시점을 기준(마지막 확인 시각)으로 새로 잡는다 — 그래야 "토글한 시점부터"
	 * 주기가 시작된다. 끌 때는 기준 시각을 건드릴 필요 없다(다시 켜면 그때부터 다시 잰다). */
	fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
		val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
		prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
		if (enabled) {
			prefs.edit().putLong(KEY_AUTO_BACKUP_LAST_CHECK, System.currentTimeMillis()).apply()
		}
	}

	/** 자동 내보내기 주기(일수). 기본은 30일(1개월). */
	fun getAutoBackupIntervalDays(context: Context): Int {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getInt(KEY_AUTO_BACKUP_INTERVAL_DAYS, 30)
	}

	fun setAutoBackupIntervalDays(context: Context, days: Int) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putInt(KEY_AUTO_BACKUP_INTERVAL_DAYS, days).apply()
	}

	/** 마지막으로 "주기가 지났는지" 확인(=알림을 띄웠는지)한 시각. 알림에 예/아니요 어느 쪽으로
	 * 답하든, 다음 확인은 여기서부터 다시 주기만큼 지나야 뜬다 — 답을 안 골랐다고 매일 뜨지 않는다. */
	fun getAutoBackupLastCheck(context: Context): Long {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getLong(KEY_AUTO_BACKUP_LAST_CHECK, System.currentTimeMillis())
	}

	fun setAutoBackupLastCheck(context: Context, timestamp: Long) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putLong(KEY_AUTO_BACKUP_LAST_CHECK, timestamp).apply()
	}

	/** 자동 내보내기가 저장될 폴더(SAF tree Uri를 문자열로). 아직 안 골랐으면 null. */
	fun getAutoBackupFolderUri(context: Context): String? {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_AUTO_BACKUP_FOLDER_URI, null)
	}

	fun setAutoBackupFolderUri(context: Context, uri: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_AUTO_BACKUP_FOLDER_URI, uri).apply()
	}
}