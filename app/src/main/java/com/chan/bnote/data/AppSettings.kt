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

	private const val DEFAULT_FONT_SIZE = 16
	const val MIN_FONT_SIZE = 12
	const val MAX_FONT_SIZE = 28

	private const val KEY_SCROLL_SPEED = "scroll_speed" // 1(느림)~5(빠름)
	private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
	private const val KEY_COPY_INCLUDE_SECONDARY = "copy_include_secondary"
	private const val KEY_COPY_REFERENCE_STYLE = "copy_reference_style" // NONE | SHORT | LONG

	fun getPrimaryTranslation(context: Context): String {
		return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_PRIMARY_TRANSLATION, "GAEYEOK") ?: "GAEYEOK"
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
}