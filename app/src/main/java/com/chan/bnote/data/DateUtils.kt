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
		return SimpleDateFormat("yy/MM/dd", Locale.KOREA).format(millis)
	}

	fun getMonthStartMillis(year: Int, month0: Int): Long {
		val cal = Calendar.getInstance()
		cal.set(year, month0, 1, 0, 0, 0)
		cal.set(Calendar.MILLISECOND, 0)
		return cal.timeInMillis
	}

	fun getMonthEndMillisExclusive(year: Int, month0: Int): Long {
		val cal = Calendar.getInstance()
		cal.set(year, month0, 1, 0, 0, 0)
		cal.set(Calendar.MILLISECOND, 0)
		cal.add(Calendar.MONTH, 1)
		return cal.timeInMillis
	}

	fun formatYearMonth(year: Int, month0: Int): String = "${year}년 ${month0 + 1}월"

	/**
	 * 캘린더 그리드는 1일 앞/31일 뒤로 이전·다음 달 날짜도 몇 칸 채워서 보여주는데(빈 칸 없이
	 * 주 단위를 맞추기 위해), 색깔 막대를 가져올 때 딱 이번 달 범위만 조회하면 그 앞뒤로 채워진
	 * 칸에는 막대가 하나도 안 보이는 문제가 있었다. 한 주는 최대 6칸까지 앞/뒤로 채워질 수 있어서,
	 * 앞뒤로 7일씩 넉넉히 여유를 둔 범위를 반환한다.
	 */
	fun getMonthGridRangeMillis(year: Int, month0: Int): Pair<Long, Long> {
		val start = Calendar.getInstance().apply {
			timeInMillis = getMonthStartMillis(year, month0)
			add(Calendar.DAY_OF_YEAR, -7)
		}.timeInMillis
		val end = Calendar.getInstance().apply {
			timeInMillis = getMonthEndMillisExclusive(year, month0)
			add(Calendar.DAY_OF_YEAR, 7)
		}.timeInMillis
		return start to end
	}
}