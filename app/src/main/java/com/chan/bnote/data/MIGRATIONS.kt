package com.chan.bnote.data

import androidx.room.migration.Migration

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
	// 예시(다음 버전 올릴 때 이런 식으로 추가):
	// object : Migration(22, 23) {
	//     override fun migrate(db: SupportSQLiteDatabase) {
	//         db.execSQL("ALTER TABLE sermons ADD COLUMN newColumn TEXT NOT NULL DEFAULT ''")
	//     }
	// }
)