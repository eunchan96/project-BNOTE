package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scrap_groups")
data class ScrapGroup(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val sortOrder: Int = 0
)