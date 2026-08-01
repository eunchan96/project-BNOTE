package com.chan.bnote.data.bible

import android.content.Context
import android.util.Log
import com.chan.bnote.data.BibleDatabase
import org.json.JSONArray
import java.io.FileNotFoundException

object BibleSeeder {

	private const val TAG = "BibleSeeder"
	private const val PREFS_NAME = "bible_seed_prefs"
	private const val KEY_SEED_VERSION = "seed_version"

	// 성경 본문 assets(JSON)를 고칠 때마다 이 숫자를 1씩 올린다.
	// 그러면 이미 앱을 쓰고 있던 사용자도 다음 실행 시 그 번역본만 자동으로 다시 심어진다.
	private const val SEED_VERSION = 4

	// 배포 전 오탈자 등을 계속 확인하는 동안엔 true로 두면 매번(앱 실행마다) 무조건 다시 심는다.
	// 실제 배포 전에는 반드시 false로 바꿔서, 위 SEED_VERSION 번호로만 재시딩되게 할 것.
	private const val FORCE_RESEED_EVERY_LAUNCH = true

	suspend fun seedIfEmpty(context: Context, db: BibleDatabase) {
		val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
		val appliedVersion = prefs.getInt(KEY_SEED_VERSION, -1)
		val needsReseed = FORCE_RESEED_EVERY_LAUNCH || appliedVersion != SEED_VERSION

		val allUnresolvedIssues = mutableListOf<SeedReconciliationReport.Issue>()

		for (translation in Translation.values()) {
			if (needsReseed) {
				allUnresolvedIssues += reconcileAndReseedTranslation(context, db, translation)
			} else {
				seedTranslationIfEmpty(context, db, translation)
			}
		}

		if (needsReseed) {
			prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()
			SeedReconciliationReport.replaceAll(context, allUnresolvedIssues)
		}
	}

	private data class VerseKey(val bookId: Int, val chapter: Int, val verse: Int)

	/** 절 하나의 두 조각(본문, 소제목 뒷부분) 텍스트. 대부분은 second가 null. */
	private data class VerseSegments(val first: String, val second: String?)

	/**
	 * 본문을 통째로 지우고 다시 심되, 단어 메모 · 부분 하이라이트(글자 위치로 anchor된 데이터)가
	 * 깨지지 않도록 최대한 다시 맞춰준다. segment(0=본문, 1=절이 소제목으로 쪼개진 경우의 뒷부분)별로
	 * 따로 다룬다.
	 *
	 * 방식: 지우기 전 각 anchor가 가리키던 "글자 조각"을 미리 기억해뒀다가, 새 본문의 같은 segment에서
	 * 같은 조각을 다시 찾아서 위치를 옮겨준다. 못 찾으면(그 단어 자체가 고쳐지면서 없어진 경우) 위치를
	 * 그대로 두고 로그·리포트에 남긴다 — 데이터를 임의로 지우기보다는, 사람이 나중에 직접 확인할 수 있게
	 * 남겨두는 쪽을 택했다.
	 */
	private suspend fun reconcileAndReseedTranslation(
		context: Context,
		db: BibleDatabase,
		translation: Translation
	): List<SeedReconciliationReport.Issue> {
		val oldVerses = db.bibleDao().getAllForTranslation(translation.code)
		if (oldVerses.isEmpty()) {
			// 처음 설치라 지울 것도 없는 경우 — 그냥 평소처럼 심으면 된다.
			seedTranslationIfEmpty(context, db, translation)
			return emptyList()
		}
		val oldSegmentsByKey = oldVerses.associate {
			VerseKey(it.bookId, it.chapter, it.verse) to VerseSegments(it.text, it.text2)
		}

		val wordMemos = db.wordMemoDao().getAll().filter { it.translation == translation.code }
		val highlights =
			db.partialHighlightDao().getAll().filter { it.translation == translation.code }

		val wordMemoOldSnippet = wordMemos.associateWith {
			snippetOf(
				oldSegmentsByKey,
				it.bookId,
				it.chapter,
				it.verse,
				it.segment,
				it.startOffset,
				it.endOffset
			)
		}
		val highlightOldSnippet = highlights.associateWith {
			snippetOf(
				oldSegmentsByKey,
				it.bookId,
				it.chapter,
				it.verse,
				it.segment,
				it.startOffset,
				it.endOffset
			)
		}

		db.bibleDao().deleteTranslation(translation.code)
		seedTranslationIfEmpty(context, db, translation)

		val newVerses = db.bibleDao().getAllForTranslation(translation.code)
		val newSegmentsByKey = newVerses.associate {
			VerseKey(it.bookId, it.chapter, it.verse) to VerseSegments(it.text, it.text2)
		}

		var relocated = 0
		val unresolvedIssues = mutableListOf<SeedReconciliationReport.Issue>()
		val now = System.currentTimeMillis()

		for (memo in wordMemos) {
			val oldSnippet = wordMemoOldSnippet[memo]
			if (oldSnippet.isNullOrEmpty()) continue
			val newText =
				textOf(newSegmentsByKey, memo.bookId, memo.chapter, memo.verse, memo.segment)
					?: continue

			val stillValid = memo.endOffset <= newText.length &&
					newText.substring(memo.startOffset, memo.endOffset) == oldSnippet
			if (stillValid) continue

			val foundIndex = newText.indexOf(oldSnippet)
			if (foundIndex >= 0) {
				db.wordMemoDao().update(
					memo.copy(startOffset = foundIndex, endOffset = foundIndex + oldSnippet.length)
				)
				relocated++
			} else {
				unresolvedIssues.add(
					SeedReconciliationReport.Issue(
						type = "word_memo",
						bookId = memo.bookId,
						chapter = memo.chapter,
						verse = memo.verse,
						oldSnippet = oldSnippet,
						memoText = memo.text,
						recordedAt = now
					)
				)
				Log.w(
					TAG,
					"단어 메모 위치를 다시 못 찾음: ${translation.code} ${memo.bookId}:${memo.chapter}:${memo.verse}(segment=${memo.segment})" +
							" 원래 단어=\"$oldSnippet\" 메모 내용=\"${memo.text}\" (위치는 그대로 둠, 직접 확인 필요)"
				)
			}
		}

		for (highlight in highlights) {
			val oldSnippet = highlightOldSnippet[highlight]
			if (oldSnippet.isNullOrEmpty()) continue
			val newText = textOf(
				newSegmentsByKey,
				highlight.bookId,
				highlight.chapter,
				highlight.verse,
				highlight.segment
			) ?: continue

			val stillValid = highlight.endOffset <= newText.length &&
					newText.substring(highlight.startOffset, highlight.endOffset) == oldSnippet
			if (stillValid) continue

			val foundIndex = newText.indexOf(oldSnippet)
			if (foundIndex >= 0) {
				db.partialHighlightDao().update(
					highlight.copy(
						startOffset = foundIndex,
						endOffset = foundIndex + oldSnippet.length
					)
				)
				relocated++
			} else {
				unresolvedIssues.add(
					SeedReconciliationReport.Issue(
						type = "highlight",
						bookId = highlight.bookId,
						chapter = highlight.chapter,
						verse = highlight.verse,
						oldSnippet = oldSnippet,
						memoText = null,
						recordedAt = now
					)
				)
				Log.w(
					TAG,
					"부분 하이라이트 위치를 다시 못 찾음: ${translation.code} ${highlight.bookId}:${highlight.chapter}:${highlight.verse}(segment=${highlight.segment})" +
							" 원래 글자=\"$oldSnippet\" (위치는 그대로 둠, 직접 확인 필요)"
				)
			}
		}

		if (relocated > 0 || unresolvedIssues.isNotEmpty()) {
			Log.i(
				TAG,
				"${translation.code} 재시딩 후 위치 재정렬: 자동 이동 $relocated 개, 못 찾음 ${unresolvedIssues.size} 개"
			)
		}

		return unresolvedIssues
	}

	private fun textOf(
		segmentsByKey: Map<VerseKey, VerseSegments>,
		bookId: Int, chapter: Int, verse: Int, segment: Int
	): String? {
		val segments = segmentsByKey[VerseKey(bookId, chapter, verse)] ?: return null
		return if (segment == 1) segments.second else segments.first
	}

	private fun snippetOf(
		segmentsByKey: Map<VerseKey, VerseSegments>,
		bookId: Int, chapter: Int, verse: Int, segment: Int, startOffset: Int, endOffset: Int
	): String {
		val text = textOf(segmentsByKey, bookId, chapter, verse, segment) ?: return ""
		val start = startOffset.coerceIn(0, text.length)
		val end = endOffset.coerceIn(start, text.length)
		return text.substring(start, end)
	}

	private suspend fun seedTranslationIfEmpty(
		context: Context,
		db: BibleDatabase,
		translation: Translation
	) {
		if (db.bibleDao().countForTranslation(translation.code) > 0) return

		val jsonText = try {
			context.assets.open(translation.assetFileName)
				.bufferedReader(Charsets.UTF_8)
				.use { it.readText() }
		} catch (e: FileNotFoundException) {
			return // 해당 번역본 파일이 assets에 없으면 조용히 스킵
		}

		val array = JSONArray(jsonText)
		val verses = if (translation.isNestedBookFormat) {
			parseNestedBookFormat(array, translation.code)
		} else {
			parseFlatFormat(array, translation.code)
		}

		db.bibleDao().insertAll(verses)
	}

	/**
	 * 기존 번역본들(NKRV 등)과 KJV.json이 쓰는 평평한 구조: 절 하나하나가
	 * {book(정수 1~66), chapter(정수), verse(정수), text, ...}를 직접 갖고 있다.
	 */
	private fun parseFlatFormat(array: JSONArray, translationCode: String): List<BibleVerse> {
		val verses = ArrayList<BibleVerse>(array.length())
		for (i in 0 until array.length()) {
			val obj = array.getJSONObject(i)
			verses.add(
				BibleVerse(
					translation = translationCode,
					bookId = obj.getInt("book"),
					chapter = obj.getInt("chapter"),
					verse = obj.getInt("verse"),
					text = obj.getString("text"),
					title = if (obj.has("title") && !obj.isNull("title")) obj.getString("title") else null,
					// 절 중간에 소제목이 오는 극소수 예외 구절(예: 창 35:22)에서만 쓰인다.
					title2 = if (obj.has("title_2") && !obj.isNull("title_2")) obj.getString("title_2") else null,
					text2 = if (obj.has("text_2") && !obj.isNull("text_2")) obj.getString("text_2") else null
				)
			)
		}
		return verses
	}

	/**
	 * NIV.json, ESV.json이 쓰는 중첩 구조: 최상위 배열이 책(총 66개, 창세기~요한계시록 순서 그대로)이고,
	 * 각 책 안에 장 목록, 각 장 안에 절 목록이 들어있다. book/chapter는 정수 ID가 아니라 영문 이름·문자열
	 * 이라서 아래 규칙으로 변환한다.
	 *
	 * - bookId: 책 이름으로 매칭하지 않고, 최상위 배열의 순서(1번째 = 창세기 = 1)를 그대로 쓴다 —
	 *   NIV.json·ESV.json 둘 다 표준 66권 순서 그대로 들어있어서 이 방식이 책 이름 표기 차이
	 *   (예: "Song Of Solomon" vs "Song of Solomon")에 영향을 받지 않아 더 안전하다.
	 * - chapter 번호: "chapter" 필드가 있으면(NIV.json) 그대로 쓰고, 없으면(ESV.json) "ID" 필드
	 *   (예: "OT:GEN.2")의 마지막 "." 뒤 숫자에서 가져온다.
	 * - verse: 같은 장 안에서 같은 절 번호가 연속으로 여러 번 나올 수 있는데(ESV.json 특유의 문제 —
	 *   시처럼 한 절이 여러 줄로 나뉘어 저장돼 있음), 순서가 보장돼 있으므로 연속된 같은 절 번호끼리는
	 *   그냥 띄어쓰기로 이어붙여 한 절로 합친다.
	 * - text_2: 절이 소제목으로 나뉘는 극소수 구절(예: 창 35:22)에서만 쓰이며, 이미 데이터에 반영돼
	 *   있으므로 있는 그대로 가져온다.
	 */
	private fun parseNestedBookFormat(array: JSONArray, translationCode: String): List<BibleVerse> {
		val verses = ArrayList<BibleVerse>()

		for (bookIndex in 0 until array.length()) {
			val bookId = bookIndex + 1
			val bookObj = array.getJSONObject(bookIndex)
			val chapters = bookObj.getJSONArray("chapters")

			for (chapterIndex in 0 until chapters.length()) {
				val chapterObj = chapters.getJSONObject(chapterIndex)
				val chapterNum = when {
					chapterObj.has("chapter") -> chapterObj.getString("chapter").toInt()
					chapterObj.has("ID") -> chapterObj.getString("ID").substringAfterLast('.')
						.toInt()

					else -> error("장 번호를 알 수 없는 chapter 객체(book=$bookId, index=$chapterIndex)")
				}

				val verseArray = chapterObj.getJSONArray("verses")
				var index = 0
				while (index < verseArray.length()) {
					val firstObj = verseArray.getJSONObject(index)
					val verseNumRaw = firstObj.getString("verse")

					val textParts = mutableListOf(firstObj.getString("text"))
					var text2: String? =
						if (firstObj.has("text_2") && !firstObj.isNull("text_2")) {
							firstObj.getString("text_2")
						} else {
							null
						}

					var next = index + 1
					while (next < verseArray.length() &&
						verseArray.getJSONObject(next).getString("verse") == verseNumRaw
					) {
						val obj = verseArray.getJSONObject(next)
						textParts.add(obj.getString("text"))
						if (text2 == null && obj.has("text_2") && !obj.isNull("text_2")) {
							text2 = obj.getString("text_2")
						}
						next++
					}

					verses.add(
						BibleVerse(
							translation = translationCode,
							bookId = bookId,
							chapter = chapterNum,
							verse = verseNumRaw.toInt(),
							text = textParts.joinToString(" "),
							text2 = text2
						)
					)

					index = next
				}
			}
		}

		return verses
	}
}