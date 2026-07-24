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
	}
)