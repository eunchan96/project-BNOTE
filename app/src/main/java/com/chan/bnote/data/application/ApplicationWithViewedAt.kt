package com.chan.bnote.data.application

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** 마이페이지 "최근 활동"에서 적용 하나를 그 열람 시각과 함께 보여줄 때 쓴다. */
data class ApplicationWithViewedAt(
	@Embedded val application: Application,
	val viewedAt: Long
)

@Dao
interface ApplicationViewDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(view: ApplicationView)

	// 지운 적용의 기록이 남아있어도 INNER JOIN에서 자연히 빠진다.
	@Query(
		"""
		SELECT applications.*, application_views.viewedAt AS viewedAt FROM application_views
		INNER JOIN applications ON applications.id = application_views.applicationId
		ORDER BY application_views.viewedAt DESC
		LIMIT :limit
		"""
	)
	suspend fun getRecentApplications(limit: Int): List<ApplicationWithViewedAt>
}