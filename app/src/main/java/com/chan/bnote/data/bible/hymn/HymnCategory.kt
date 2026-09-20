package com.chan.bnote.data.bible.hymn

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hymn_categories")
data class HymnCategory(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val parentId: Long? = null, // null이면 대분류, 값이 있으면 그 대분류에 속한 소분류
	val sortOrder: Int = 0
)