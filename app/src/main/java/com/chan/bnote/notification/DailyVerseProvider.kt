package com.chan.bnote.notification

import android.content.Context
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.notification.DailyVerseProvider.SHUFFLE_SEED
import com.chan.bnote.notification.DailyVerseProvider.epochDay
import java.util.Calendar
import java.util.Random
import java.util.TimeZone

/**
 * "오늘의 말씀"을 정하는 곳. 매일 말씀 알림이 여기서 구절을 받아 쓴다(위젯도 같은 곳을 쓰게 될
 * 예정이라, 알림과 위젯이 항상 같은 구절을 보여주도록 한 군데에서만 고른다).
 *
 * 예전에는 알림이 울릴 때마다 `.random()`으로 뽑아서 사용자마다, 그리고 날마다 제각각이었다.
 * 지금은 "날짜"만으로 구절이 정해진다 — 서버 없이도, 같은 버전의 앱을 쓰는 모든 사용자가
 * 같은 날에는 같은 구절을 받는다.
 *
 * 방식:
 * 1) 구절 목록을 (책, 장, 시작절, 끝절) 순으로 정렬한다. daily_verses.json 안의 나열 순서가
 *    바뀌어도 결과가 달라지지 않게 하려는 것이다.
 * 2) 고정된 시드([SHUFFLE_SEED])로 한 번 섞는다. 정렬 순서 그대로 내보내면 창세기부터 차례로
 *    나오기 때문이다. java.util.Random은 알고리즘이 규격으로 정해져 있어서 기기·OS 버전과
 *    상관없이 같은 결과가 나온다.
 * 3) 오늘 날짜를 "1970-01-01부터 며칠째인지"([epochDay])로 바꿔서, 섞은 목록의
 *    (일수 % 구절 수)번째 구절을 고른다. 구절이 2,000여 개라 5년 넘게 같은 구절이 반복되지 않는다.
 *
 * 주의:
 * - daily_verses.json에 구절을 넣거나 빼면 그날부터 배정이 통째로 바뀐다. 업데이트 전후 버전을
 *   쓰는 사용자끼리는 그동안 다른 구절을 받게 되니, 목록은 버전을 올리면서만 고칠 것.
 * - [SHUFFLE_SEED]는 절대 바꾸지 말 것(바꾸면 모든 날의 구절이 바뀐다).
 */
object DailyVerseProvider {

	/** 알림·위젯에 보여줄 오늘의 말씀 한 건. */
	data class DailyVerse(
		val bookId: Int,
		val chapter: Int,
		/** 탭했을 때 이동할 첫 절. */
		val startVerse: Int,
		val endVerse: Int,
		/** "창세기 1장 1절" 형태의 위치 표기. */
		val label: String,
		/** 본문. 여러 절이면 절마다 줄바꿈으로 이어붙인다. */
		val text: String
	)

	private const val SHUFFLE_SEED = 20260101L
	private const val MILLIS_PER_DAY = 86_400_000L

	// 그날 정해진 구절이 이 번역본 DB에 없으면(목록 오류 등) 다음 칸을 이어서 시도한다. 시도 횟수를
	// 넉넉히 제한해둔 건, DB가 비어있는 채로 목록 전체를 헛돌지 않게 하기 위해서다.
	private const val MAX_SKIP = 10

	@Volatile
	private var orderedCache: List<CuratedVerses.VerseRef>? = null

	/** 정렬한 뒤 고정 시드로 섞은 구절 목록. 앱이 켜져 있는 동안 한 번만 계산해서 재사용한다. */
	private fun orderedRefs(context: Context): List<CuratedVerses.VerseRef> {
		orderedCache?.let { return it }

		val list = CuratedVerses.load(context)
			.sortedWith(
				compareBy(
					{ it.bookId },
					{ it.chapter },
					{ it.startVerse },
					{ it.endVerse })
			)
			.toMutableList()

		// Fisher-Yates 셔플
		val random = Random(SHUFFLE_SEED)
		for (i in list.size - 1 downTo 1) {
			val j = random.nextInt(i + 1)
			val tmp = list[i]
			list[i] = list[j]
			list[j] = tmp
		}
		orderedCache = list
		return list
	}

	/**
	 * [millis]가 속한 "달력상의 날짜"가 1970-01-01부터 며칠째인지. 기기의 현재 시간대 기준 날짜를
	 * 뽑은 뒤 UTC 자정으로 옮겨서 세기 때문에, 시간대나 하루 중 몇 시인지와 상관없이 날짜가 같으면
	 * 항상 같은 값이다.
	 */
	fun epochDay(millis: Long = System.currentTimeMillis()): Long {
		val local = Calendar.getInstance().apply { timeInMillis = millis }
		val utcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
			clear()
			set(
				local.get(Calendar.YEAR),
				local.get(Calendar.MONTH),
				local.get(Calendar.DAY_OF_MONTH)
			)
		}
		return utcMidnight.timeInMillis.floorDiv(MILLIS_PER_DAY)
	}

	/** 오늘의 말씀. 성경 데이터가 아직 준비되지 않았으면(첫 설치 직후 등) null. */
	suspend fun getToday(context: Context, translation: String = "NKRV"): DailyVerse? =
		getForDay(context, epochDay(), translation)

	suspend fun getForDay(
		context: Context,
		epochDay: Long,
		translation: String = "NKRV"
	): DailyVerse? {
		val refs = orderedRefs(context)
		if (refs.isEmpty()) return null

		val dao = BibleDatabase.getInstance(context.applicationContext).bibleDao()
		// 성경 본문은 성경 탭을 처음 열 때 심어진다. 그 전이면 조회할 게 없다.
		if (dao.countForTranslation(translation) == 0) return null

		val start = epochDay.mod(refs.size.toLong()).toInt()
		for (offset in 0 until minOf(MAX_SKIP, refs.size)) {
			val ref = refs[(start + offset) % refs.size]
			val verses = dao.getVerses(translation, ref.bookId, ref.chapter)
				.filter { it.verse in ref.startVerse..ref.endVerse }
			if (verses.isNotEmpty()) return build(ref, verses)
		}
		return null
	}

	private fun build(ref: CuratedVerses.VerseRef, verses: List<BibleVerse>): DailyVerse {
		val unit = BibleBooks.chapterUnit(ref.bookId)
		val verseLabel = if (ref.startVerse == ref.endVerse) {
			"${ref.startVerse}절"
		} else {
			"${ref.startVerse}~${ref.endVerse}절"
		}

		// 절 중간에 소제목이 끼는 극소수 구절(예: 옵 1:1)은 앞부분(text)과 뒷부분(text2)으로 나뉘어
		// 저장돼 있다. 뒷부분을 빼면 문장이 중간에서 끊기므로, 복사할 때와 같은 방식(공백으로
		// 이어붙임)으로 합쳐서 보여준다.
		val text = verses.joinToString("\n") { verse ->
			if (!verse.text2.isNullOrBlank()) "${verse.text} ${verse.text2}" else verse.text
		}

		return DailyVerse(
			bookId = ref.bookId,
			chapter = ref.chapter,
			startVerse = verses.first().verse,
			endVerse = ref.endVerse,
			label = "${BibleBooks.nameOf(ref.bookId)} ${ref.chapter}${unit} $verseLabel",
			text = text
		)
	}
}