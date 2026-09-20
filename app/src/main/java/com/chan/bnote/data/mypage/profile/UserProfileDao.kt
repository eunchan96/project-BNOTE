package com.chan.bnote.data.mypage.profile

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserProfileDao {

	@Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
	suspend fun get(): UserProfile?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(profile: UserProfile)

	@Query("DELETE FROM user_profile")
	suspend fun deleteAll()
}