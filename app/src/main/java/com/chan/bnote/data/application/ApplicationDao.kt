package com.chan.bnote.data.application

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ApplicationDao {

	@Insert
	suspend fun insert(application: Application): Long

	@Update
	suspend fun update(application: Application)

	@Delete
	suspend fun delete(application: Application)

	@Query("SELECT * FROM applications WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): Application?

	@Query("SELECT * FROM applications")
	suspend fun getAll(): List<Application>

	@Query("DELETE FROM applications")
	suspend fun deleteAll()

	@Query("SELECT * FROM applications WHERE applicationDate = :dateMillis ORDER BY createdAt")
	suspend fun getByDate(dateMillis: Long): List<Application>

	@Query("SELECT * FROM applications WHERE categoryId = :categoryId ORDER BY applicationDate DESC")
	suspend fun getByCategory(categoryId: Long): List<Application>

	@Query("SELECT * FROM applications WHERE categoryId IS NULL ORDER BY applicationDate DESC")
	suspend fun getUncategorized(): List<Application>

	@Query(
		"""
        SELECT a.applicationDate as applicationDate, c.colorHex as colorHex
        FROM applications a
        LEFT JOIN application_categories c ON a.categoryId = c.id
        WHERE a.applicationDate >= :startMillis AND a.applicationDate < :endMillis
        ORDER BY a.applicationDate, a.createdAt
        """
	)
	suspend fun getMarkersInRange(startMillis: Long, endMillis: Long): List<ApplicationMarker>

	// 설교 detail 화면에서 "이 설교에 이미 연결된 적용이 있는지" 확인할 때 사용.
	@Query(
		"""
        SELECT a.* FROM applications a
        INNER JOIN application_sermon_links l ON l.applicationId = a.id
        WHERE l.sermonId = :sermonId
        LIMIT 1
        """
	)
	suspend fun getFirstBySermonId(sermonId: Long): Application?
}

data class ApplicationMarker(val applicationDate: Long, val colorHex: String?)