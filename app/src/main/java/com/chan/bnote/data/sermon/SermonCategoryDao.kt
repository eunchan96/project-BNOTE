package com.chan.bnote.data.sermon

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SermonCategoryDao {

	@Query("SELECT COUNT(*) FROM sermon_categories")
	suspend fun count(): Int

	@Insert
	suspend fun insertAll(categories: List<SermonCategory>)

	@Insert
	suspend fun insert(category: SermonCategory): Long

	@Update
	suspend fun update(category: SermonCategory)

	@Delete
	suspend fun delete(category: SermonCategory)

	@Query("SELECT * FROM sermon_categories ORDER BY sortOrder")
	suspend fun getAll(): List<SermonCategory>

	@Query("DELETE FROM sermon_categories")
	suspend fun deleteAll()

	@Query("SELECT * FROM sermon_categories WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): SermonCategory?
}