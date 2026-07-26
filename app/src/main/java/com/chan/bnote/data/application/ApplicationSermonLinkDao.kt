package com.chan.bnote.data.application

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ApplicationSermonLinkDao {

	@Insert
	suspend fun insert(link: ApplicationSermonLink): Long

	@Query("SELECT * FROM application_sermon_links WHERE applicationId = :applicationId")
	suspend fun getByApplication(applicationId: Long): List<ApplicationSermonLink>

	@Query("DELETE FROM application_sermon_links WHERE applicationId = :applicationId")
	suspend fun deleteByApplication(applicationId: Long)

	@Query("DELETE FROM application_sermon_links")
	suspend fun deleteAll()

	@Query(
		"""
        SELECT COUNT(*) FROM application_sermon_links
        WHERE sermonId = :sermonId
        """
	)
	suspend fun countBySermonId(sermonId: Long): Int
}