package com.chan.bnote.data.application

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ApplicationCategoryDao {

	@Query("SELECT COUNT(*) FROM application_categories")
	suspend fun count(): Int

	@Insert
	suspend fun insertAll(categories: List<ApplicationCategory>)

	@Insert
	suspend fun insert(category: ApplicationCategory): Long

	@Update
	suspend fun update(category: ApplicationCategory)

	@Delete
	suspend fun delete(category: ApplicationCategory)

	@Query("SELECT * FROM application_categories ORDER BY sortOrder")
	suspend fun getAll(): List<ApplicationCategory>

	@Query("DELETE FROM application_categories")
	suspend fun deleteAll()

	@Query("SELECT * FROM application_categories WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): ApplicationCategory?
}