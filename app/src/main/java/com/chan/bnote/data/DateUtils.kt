package com.chan.bnote.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DateUtils {
	// 시/분/초 제거해서 "그 날"만 남긴 millis로 정규화 (같은 날 비교용)
	fun normalizeToDayStart(millis: Long): Long {
		val cal = Calendar.getInstance()
		cal.timeInMillis = millis
		cal.set(Calendar.HOUR_OF_DAY, 0)
		cal.set(Calendar.MINUTE, 0)
		cal.set(Calendar.SECOND, 0)
		cal.set(Calendar.MILLISECOND, 0)
		return cal.timeInMillis
	}

	fun formatDate(millis: Long): String {
		return SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA).format(millis)
	}

	fun formatDateShort(millis: Long): String {
		return SimpleDateFormat("M/d", Locale.KOREA).format(millis)
	}
}