package com.chan.bnote.data.sermon

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** 마이페이지 "최근 활동"에서 설교 하나를 그 열람 시각과 함께 보여줄 때 쓴다. */
data class SermonWithViewedAt(
	@Embedded val sermon: Sermon,
	val viewedAt: Long
)

@Dao
interface SermonViewDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(view: SermonView)

	// 지운 설교의 기록이 남아있어도 INNER JOIN에서 자연히 빠진다.
	@Query(
		"""
		SELECT sermons.*, sermon_views.viewedAt AS viewedAt FROM sermon_views
		INNER JOIN sermons ON sermons.id = sermon_views.sermonId
		ORDER BY sermon_views.viewedAt DESC
		LIMIT :limit
		"""
	)
	suspend fun getRecentSermons(limit: Int): List<SermonWithViewedAt>
}