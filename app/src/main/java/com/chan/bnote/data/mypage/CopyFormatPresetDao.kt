package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CopyFormatPresetDao {

	@Insert
	suspend fun insert(preset: CopyFormatPreset): Long

	@Update
	suspend fun update(preset: CopyFormatPreset)

	@Delete
	suspend fun delete(preset: CopyFormatPreset)

	@Query("SELECT * FROM copy_format_presets ORDER BY createdAt ASC")
	suspend fun getAll(): List<CopyFormatPreset>

	@Query("SELECT * FROM copy_format_presets WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): CopyFormatPreset?

	@Query("DELETE FROM copy_format_presets")
	suspend fun deleteAll()
}