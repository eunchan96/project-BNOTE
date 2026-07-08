package com.chan.bnote.data

import android.content.Context

object AppSettings {
	private const val PREF_NAME = "bnote_settings"
	private const val KEY_FONT_SIZE = "font_size_sp"
	private const val KEY_DARK_MODE = "dark_mode"
	private const val KEY_READING_PLAN_ENABLED = "reading_plan_enabled"

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
}