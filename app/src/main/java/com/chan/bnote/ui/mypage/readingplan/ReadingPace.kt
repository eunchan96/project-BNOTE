package com.chan.bnote.ui.mypage.readingplan

import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.mypage.readingplan.ReadingGoal
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 성경읽기표 상단의 진행률·하루 분량·계획 대비 문구를 읽기 목표 기준으로 계산한다.
 */
object ReadingPace {

	private const val DAY_MS = 24L * 60 * 60 * 1000

	/** [startDay]부터 [endDay]까지 며칠인지(양 끝 포함). 둘 다 그날 0시 값이어야 한다. */
	fun daysInclusive(startDay: Long, endDay: Long): Int =
		((endDay - startDay).toDouble() / DAY_MS).roundToInt() + 1

	private fun shortDate(millis: Long): String =
		SimpleDateFormat("yyyy.M.d", Locale.KOREA).format(millis)

	private fun monthDay(millis: Long): String =
		SimpleDateFormat("M월 d일", Locale.KOREA).format(millis)

	private fun oneDecimal(value: Double): String = String.format(Locale.KOREA, "%.1f", value)

	/** 진행률 줄과 프로그레스바 값(0~100). */
	fun progressLine(
		goal: ReadingGoal,
		maxChapterByBook: Map<Int, Int>,
		readList: List<ReadingProgress>
	): Pair<String, Int> {
		val total = goal.bookIds.sumOf { maxChapterByBook[it] ?: 0 }
		val read = readList.count { it.bookId in goal.bookIds }
		val percent = if (total > 0) read * 100.0 / total else 0.0
		val text = if (goal.isAllBooks) {
			String.format(
				Locale.KOREA,
				"전체 %,d / %,d 장 읽음 (%s%%)",
				read,
				total,
				oneDecimal(percent)
			)
		} else {
			String.format(
				Locale.KOREA, "목표 %d권 · %,d / %,d 장 읽음 (%s%%)",
				goal.bookIds.size, read, total, oneDecimal(percent)
			)
		}
		return text to percent.toInt()
	}

	/**
	 * 진행률 아래에 보여줄 안내 문구(여러 줄).
	 * - 기간과 남은 날짜, 종료일까지 다 읽으려면 하루 몇 장씩인지
	 * - 계획 대비: 시작일~종료일에 걸쳐 고르게 읽는다고 했을 때 "어제까지" 읽었어야 할 양과 비교.
	 *   시작일 전에 이미 읽어둔 장은 출발점으로 치고, 남은 양만 기간에 고르게 나눈다
	 *   (그래야 목표를 중간에 새로 정해도 처음부터 "앞서 있어요"로 크게 틀어지지 않는다).
	 *   오늘 분량은 아직 하루가 안 끝났으니 기준에 넣지 않는다 — 아침에 열자마자 "뒤처져 있어요"가 뜨지 않게.
	 */
	fun guideText(
		goal: ReadingGoal,
		maxChapterByBook: Map<Int, Int>,
		readList: List<ReadingProgress>,
		nowMillis: Long = System.currentTimeMillis()
	): String {
		val lines = mutableListOf<String>()
		val inGoal = readList.filter { it.bookId in goal.bookIds }
		val total = goal.bookIds.sumOf { maxChapterByBook[it] ?: 0 }
		val read = inGoal.size
		val remaining = total - read
		val today = DateUtils.normalizeToDayStart(nowMillis)
		val period = "기간 ${shortDate(goal.startMillis)} ~ ${shortDate(goal.endMillis)}"
		val totalDays = daysInclusive(goal.startMillis, goal.endMillis).coerceAtLeast(1)

		when {
			remaining <= 0 -> lines.add(
				if (goal.isAllBooks) "축하해요, 전체 성경을 다 읽으셨어요!"
				else "축하해요, 목표한 범위를 다 읽으셨어요!"
			)

			today < goal.startMillis -> {
				val pace = remaining.toDouble() / totalDays
				lines.add("$period · ${monthDay(goal.startMillis)}부터 시작해요")
				lines.add("기간 동안 다 읽으려면 하루 ${oneDecimal(pace)}장씩")
			}

			today > goal.endMillis -> {
				lines.add(period)
				lines.add(String.format(Locale.KOREA, "목표 기간이 끝났어요 · 남은 %,d장", remaining))
			}

			else -> {
				val daysLeft = daysInclusive(today, goal.endMillis).coerceAtLeast(1) // 오늘 포함
				val pace = remaining.toDouble() / daysLeft
				lines.add("$period · 남은 날짜 ${daysLeft}일")
				lines.add("다 읽으려면 하루 ${oneDecimal(pace)}장씩")

				val baseline = inGoal.count { it.readAt < goal.startMillis }
				val daysDone = daysInclusive(goal.startMillis, today) - 1 // 어제까지 지난 날 수
				val expected = baseline + (total - baseline) * daysDone.toDouble() / totalDays
				val diff = (read - expected).roundToInt()
				lines.add(
					when {
						diff > 0 -> "계획보다 ${diff}장 앞서 있어요"
						diff < 0 -> "계획보다 ${abs(diff)}장 뒤처져 있어요"
						else -> "계획대로 읽고 있어요"
					}
				)
			}
		}

		return lines.joinToString("\n")
	}
}