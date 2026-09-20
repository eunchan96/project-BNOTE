package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentChapterViewDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(view: RecentChapterView)

	@Query("SELECT * FROM recent_chapter_views ORDER BY viewedAt DESC LIMIT :limit")
	suspend fun getRecent(limit: Int): List<RecentChapterView>
}