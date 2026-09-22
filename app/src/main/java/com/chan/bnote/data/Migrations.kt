package com.chan.bnote.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 실제 Room 마이그레이션 목록.
 *
 * 규칙: 이제부터 DB 버전을 올릴 때마다(BibleDatabase의 version = N) 여기에
 * Migration(N-1, N) 객체를 반드시 추가한다. fallbackToDestructiveMigration()은
 * 안전망으로만 남겨둔다 (마이그레이션을 깜빡했을 때 최소한 앱이 죽지 않고 켜지긴 하도록).
 *
 * 버전 22 미만에서 넘어오는 경우는 신경 쓰지 않아도 된다 — 아직 실사용자가 없어서
 * 실제로 설치돼 있는 가장 오래된 버전이 22이기 때문이다 (이 버전이 사실상 배포 기준점).
 */
val MIGRATIONS: Array<Migration> = arrayOf(
	object : Migration(22, 23) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 단어 메모 "다른 구절에도 추가"로 생긴 복사본의 출처("창 1:1" 형태)를 담는 컬럼.
			db.execSQL("ALTER TABLE word_memos ADD COLUMN sourceLabel TEXT")
		}
	},
	object : Migration(23, 24) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 구절 메모도 단어 메모처럼 한 구절에 메모를 여러 개 넣을 수 있도록 유니크 제약을 없앤다.
			db.execSQL("DROP INDEX IF EXISTS index_verse_memos_bookId_chapter_verse")
		}
	},
	object : Migration(24, 25) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 설교 detail 화면에 임베드할 링크(주로 유튜브) 컬럼. 기존 설교는 전부 NULL(링크 없음)로 유지된다.
			db.execSQL("ALTER TABLE sermons ADD COLUMN link TEXT")
		}
	},
	object : Migration(25, 26) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 절 중간에 소제목이 오는 극소수 예외 구절(예: 창 35:22)을 위한 컬럼들.
			db.execSQL("ALTER TABLE bible_verses ADD COLUMN title2 TEXT")
			db.execSQL("ALTER TABLE bible_verses ADD COLUMN text2 TEXT")
			// 기존 단어메모/부분하이라이트는 전부 앞부분(text, segment=0)을 가리키던 것들이라 기본값 0이면 안전하다.
			db.execSQL("ALTER TABLE word_memos ADD COLUMN segment INTEGER NOT NULL DEFAULT 0")
			db.execSQL("ALTER TABLE partial_highlights ADD COLUMN segment INTEGER NOT NULL DEFAULT 0")
		}
	},
	object : Migration(26, 27) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 사용자가 직접 만든 복사 형식을 이름 붙여 저장해두는 테이블.
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS copy_format_presets (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					name TEXT NOT NULL,
					configJson TEXT NOT NULL,
					createdAt INTEGER NOT NULL
				)
				""".trimIndent()
			)
		}
	},
	object : Migration(27, 28) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 적용(묵상하기/기도하기/순종하기) 탭 관련 테이블들.
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS application_categories (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					name TEXT NOT NULL,
					colorHex TEXT NOT NULL,
					isDefault INTEGER NOT NULL DEFAULT 0,
					sortOrder INTEGER NOT NULL DEFAULT 0
				)
				""".trimIndent()
			)
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS applications (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					title TEXT NOT NULL,
					categoryId INTEGER,
					applicationDate INTEGER NOT NULL,
					meditationMemo TEXT NOT NULL DEFAULT '',
					prayerMemo TEXT NOT NULL DEFAULT '',
					obedienceMemo TEXT NOT NULL DEFAULT '',
					createdAt INTEGER NOT NULL
				)
				""".trimIndent()
			)
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS application_bible_refs (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					applicationId INTEGER NOT NULL,
					startBookId INTEGER NOT NULL,
					startChapter INTEGER NOT NULL,
					startVerse INTEGER NOT NULL,
					endBookId INTEGER NOT NULL,
					endChapter INTEGER NOT NULL,
					endVerse INTEGER NOT NULL,
					isChapterOnly INTEGER NOT NULL DEFAULT 0
				)
				""".trimIndent()
			)
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS application_sermon_links (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					applicationId INTEGER NOT NULL,
					sermonId INTEGER NOT NULL
				)
				""".trimIndent()
			)
		}
	},
	object : Migration(28, 29) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 감사 노트: 날짜별로 하나의 노트, 그 안에 "✓ ____" 줄들이 여러 개.
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS gratitude_notes (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					date INTEGER NOT NULL,
					createdAt INTEGER NOT NULL
				)
				""".trimIndent()
			)
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS gratitude_entries (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					noteId INTEGER NOT NULL,
					text TEXT NOT NULL,
					sortOrder INTEGER NOT NULL DEFAULT 0
				)
				""".trimIndent()
			)
		}
	},
	object : Migration(29, 30) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 설교 검색용 순수 텍스트 컬럼. memo는 굵게/밑줄/색 서식이 하나라도 있으면
			// Html.toHtml()이 한글을 포함한 모든 글자를 HTML 문자 참조(&#45208; 등)로 바꿔서
			// 저장하는데, 그 안에서는 LIKE로 실제 글자를 찾을 수 없었다(서식 없는 메모만
			// 우연히 검색되던 원인). 여기 담을 순수 텍스트는 화면에 보여줄 때와 같은 방식
			// (RichTextUtils.toEditable과 동일한 로직)으로 기존 memo를 풀어서 채운다.
			db.execSQL("ALTER TABLE sermons ADD COLUMN memoSearchText TEXT NOT NULL DEFAULT ''")

			val cursor = db.query("SELECT id, memo FROM sermons")
			cursor.use {
				val idIndex = it.getColumnIndexOrThrow("id")
				val memoIndex = it.getColumnIndexOrThrow("memo")
				while (it.moveToNext()) {
					val id = it.getLong(idIndex)
					val memo = it.getString(memoIndex) ?: ""
					val plainText = decodeMemoToPlainText(memo)
					db.execSQL(
						"UPDATE sermons SET memoSearchText = ? WHERE id = ?",
						arrayOf(plainText, id)
					)
				}
			}
		}
	},
	object : Migration(30, 31) {
		override fun migrate(db: SupportSQLiteDatabase) {
			// 마이페이지 "최근 활동"에서 설교·적용을 "만들거나 고친 것"이 아니라 "열어 본 것" 기준으로 보여주기 위한 열람 기록 테이블.
			// 성경 장을 읽을 때 남기는 recent_chapter_views와 같은 구조다.
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS sermon_views (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					sermonId INTEGER NOT NULL,
					viewedAt INTEGER NOT NULL
				)
				""".trimIndent()
			)
			db.execSQL(
				"CREATE UNIQUE INDEX IF NOT EXISTS index_sermon_views_sermonId ON sermon_views (sermonId)"
			)
			db.execSQL(
				"""
				CREATE TABLE IF NOT EXISTS application_views (
					id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
					applicationId INTEGER NOT NULL,
					viewedAt INTEGER NOT NULL
				)
				""".trimIndent()
			)
			db.execSQL(
				"CREATE UNIQUE INDEX IF NOT EXISTS index_application_views_applicationId ON application_views (applicationId)"
			)
		}
	}
)

/** RichTextUtils.toEditable()과 같은 로직(HTML이면 풀고, sentinel 이후를 잘라내는 것)을
 * 마이그레이션에서도 써야 하는데, 그 파일은 UI 레이어(ui.sermon.addsermon)에 있어서 여기서
 * 직접 끌어다 쓰기보다 필요한 부분만 그대로 옮겨왔다 — Migrations.kt는 data 레이어라 UI 쪽
 * 클래스에 의존하지 않는 편이 맞다. 로직이 바뀌면 RichTextUtils.toEditable()도 함께 확인할 것. */
private fun decodeMemoToPlainText(raw: String): String {
	if (raw.isEmpty() || !Regex(
			"<(b|u|p|font)[ >]",
			RegexOption.IGNORE_CASE
		).containsMatchIn(raw)
	) {
		return raw
	}
	val restored =
		androidx.core.text.HtmlCompat.fromHtml(
			raw,
			androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
		)
			.toString()
	val sentinelIndex = restored.indexOf('\u200B')
	if (sentinelIndex >= 0) return restored.substring(0, sentinelIndex)
	return restored.trimEnd('\n')
}