package com.chan.bnote.notification

/**
 * 매일 말씀 알림에 쓸 구절 목록. 성경 전체에서 완전 무작위로 뽑으면 족보나 율법 조항처럼
 * 알림으로 보기에 뜬금없는 구절이 나올 수 있어서, 위로/격려가 되는 잘 알려진 구절들로 범위를 좁혔다.
 */
object CuratedVerses {

	data class VerseRef(val bookId: Int, val chapter: Int, val verse: Int)

	val list = listOf(
		VerseRef(bookId = 5, chapter = 31, verse = 6),   // 신명기 31:6
		VerseRef(bookId = 6, chapter = 1, verse = 9),    // 여호수아 1:9
		VerseRef(bookId = 19, chapter = 23, verse = 1),  // 시편 23:1
		VerseRef(bookId = 19, chapter = 27, verse = 1),  // 시편 27:1
		VerseRef(bookId = 19, chapter = 34, verse = 8),  // 시편 34:8
		VerseRef(bookId = 19, chapter = 46, verse = 1),  // 시편 46:1
		VerseRef(bookId = 19, chapter = 121, verse = 1), // 시편 121:1
		VerseRef(bookId = 20, chapter = 3, verse = 5),   // 잠언 3:5
		VerseRef(bookId = 20, chapter = 16, verse = 3),  // 잠언 16:3
		VerseRef(bookId = 23, chapter = 40, verse = 31), // 이사야 40:31
		VerseRef(bookId = 23, chapter = 41, verse = 10), // 이사야 41:10
		VerseRef(bookId = 24, chapter = 29, verse = 11), // 예레미야 29:11
		VerseRef(bookId = 40, chapter = 6, verse = 33),  // 마태복음 6:33
		VerseRef(bookId = 40, chapter = 11, verse = 28), // 마태복음 11:28
		VerseRef(bookId = 43, chapter = 3, verse = 16),  // 요한복음 3:16
		VerseRef(bookId = 43, chapter = 14, verse = 6),  // 요한복음 14:6
		VerseRef(bookId = 45, chapter = 8, verse = 28),  // 로마서 8:28
		VerseRef(bookId = 45, chapter = 12, verse = 2),  // 로마서 12:2
		VerseRef(bookId = 46, chapter = 13, verse = 4),  // 고린도전서 13:4
		VerseRef(bookId = 48, chapter = 2, verse = 20),  // 갈라디아서 2:20
		VerseRef(bookId = 49, chapter = 2, verse = 8),   // 에베소서 2:8
		VerseRef(bookId = 50, chapter = 4, verse = 6),   // 빌립보서 4:6
		VerseRef(bookId = 50, chapter = 4, verse = 13),  // 빌립보서 4:13
		VerseRef(bookId = 52, chapter = 5, verse = 16),  // 데살로니가전서 5:16
		VerseRef(bookId = 58, chapter = 13, verse = 8)   // 히브리서 13:8
	)
}