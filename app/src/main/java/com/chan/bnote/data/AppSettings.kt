package com.chan.bnote.data

import android.content.Context

object AppSettings {
	private const val PREF_NAME = "bnote_settings"
	private const val KEY_FONT_SIZE = "font_size_sp"
	private const val KEY_DARK_MODE = "dark_mode"
	private const val KEY_READING_PLAN_ENABLED = "reading_plan_enabled"
	private const val KEY_AUTO_SCROLL_ENABLED = "auto_scroll_enabled"

	private const val KEY_PREACHER_SORT_MODE = "preacher_sort_mode" // "NAME" or "CUSTOM"
	private const val KEY_PREACHER_CUSTOM_ORDER = "preacher_custom_order" // 쉼표 구분 문자열
	private const val KEY_SERMON_SORT_MODE = "sermon_sort_mode" // "DATE" or "BIBLE"

	private const val DEFAULT_FONT_SIZE = 16
	const val MIN_FONT_SIZE = 12
	const val MAX_FONT_SIZE = 28

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

	fun getPreacherCustomOrder(context: Context): List<String> {
		val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_PREACHER_CUSTOM_ORDER, "") ?: ""
		return if (raw.isBlank()) emptyList() else raw.split(",")
	}

	fun setPreacherCustomOrder(context: Context, order: List<String>) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_PREACHER_CUSTOM_ORDER, order.joinToString(",")).apply()
	}

	fun getSermonSortMode(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_SERMON_SORT_MODE, "DATE") ?: "DATE"
	}

	fun setSermonSortMode(context: Context, mode: String) {
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.edit().putString(KEY_SERMON_SORT_MODE, mode).apply()
	}
}