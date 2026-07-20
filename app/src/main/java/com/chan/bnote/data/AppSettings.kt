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
	private const val KEY_SERMON_SORT_MODE = "sermon_sort_mode" // "DATE" or "BIBLE"
	private const val KEY_BIBLE_SEARCH_HISTORY = "bible_search_history" // 구분자로 이어붙인 문자열, 최신순
	private const val SEARCH_HISTORY_DELIMITER = "\u001E"
	private const val MAX_SEARCH_HISTORY = 20

	private const val DEFAULT_FONT_SIZE = 16
	const val MIN_FONT_SIZE = 12
	const val MAX_FONT_SIZE = 28

	private const val KEY_SCROLL_SPEED = "scroll_speed" // 1(느림)~5(빠름)
	private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
	private const val KEY_COPY_INCLUDE_SECONDARY = "copy_include_secondary"
	private const val KEY_COPY_REFERENCE_STYLE = "copy_reference_style" // NONE | SHORT | LONG
	private const val KEY_LAST_READ_BOOK_ID = "last_read_book_id"
	private const val KEY_LAST_READ_CHAPTER = "last_read_chapter"
	private const val KEY_LAST_TAB = "last_tab"

	private const val KEY_DAILY_VERSE_NOTI_ENABLED = "daily_verse_noti_enabled"
	private const val KEY_DAILY_VERSE_NOTI_HOUR = "daily_verse_noti_hour"
	private const val KEY_DAILY_VERSE_NOTI_MINUTE = "daily_verse_noti_minute"
	private const val KEY_READING_REMINDER_ENABLED = "reading_reminder_enabled"
	private const val KEY_READING_REMINDER_HOUR = "reading_reminder_hour"
	private const val KEY_READING_REMINDER_MINUTE = "reading_reminder_minute"

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

	// --- 성경 검색 기록 (최신순, 최대 MAX_SEARCH_HISTORY개, 중복 없음) ---

	fun getBibleSearchHistory(context: Context): List<String> {
		val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_BIBLE_SEARCH_HISTORY, "") ?: ""
		return if (raw.isBlank()) emptyList() else raw.split(SEARCH_HISTORY_DELIMITER)
			.filter { it.isNotBlank() }
	}

	fun addBibleSearchHistory(context: Context, keyword: String) {
		val trimmed = keyword.trim()
		if (trimmed.isEmpty()) return
		val updated = mutableListOf(trimmed)
		updated.addAll(getBibleSearchHistory(context).filter { it != trimmed })
		saveBibleSearchHistory(context, updated.take(MAX_SEARCH_HISTORY))
	}

	fun removeBibleSearchHistory(context: Context, keyword: String) {
		saveBibleSearchHistory(context, getBibleSearchHistory(context).filter { it != keyword })
	}

	fun clearBibleSearchHistory(context: Context) {
		saveBibleSearchHistory(context, emptyList())
	}

	private fun saveBibleSearchHistory(context: Context, history: List<String>) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit()
			.putString(KEY_BIBLE_SEARCH_HISTORY, history.joinToString(SEARCH_HISTORY_DELIMITER))
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

	fun isCopyIncludeSecondary(context: Context): Boolean {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getBoolean(KEY_COPY_INCLUDE_SECONDARY, false)
	}

	fun setCopyIncludeSecondary(context: Context, enabled: Boolean) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putBoolean(KEY_COPY_INCLUDE_SECONDARY, enabled).apply()
	}

	fun getCopyReferenceStyle(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_COPY_REFERENCE_STYLE, "NONE") ?: "NONE"
	}

	fun setCopyReferenceStyle(context: Context, style: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_COPY_REFERENCE_STYLE, style).apply()
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
}