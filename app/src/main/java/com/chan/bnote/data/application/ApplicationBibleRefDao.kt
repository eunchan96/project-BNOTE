package com.chan.bnote.data.application

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ApplicationBibleRefDao {

	@Insert
	suspend fun insertAll(refs: List<ApplicationBibleRef>)

	@Query("SELECT * FROM application_bible_refs WHERE applicationId = :applicationId")
	suspend fun getByApplication(applicationId: Long): List<ApplicationBibleRef>

	@Query("DELETE FROM application_bible_refs WHERE applicationId = :applicationId")
	suspend fun deleteByApplication(applicationId: Long)

	@Query("DELETE FROM application_bible_refs")
	suspend fun deleteAll()
}