package com.chan.bnote.data.sermon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preachers")
data class Preacher(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val sortOrder: Int = 0
)