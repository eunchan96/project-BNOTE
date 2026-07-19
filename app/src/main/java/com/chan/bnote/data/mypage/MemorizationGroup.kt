package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memorization_groups")
data class MemorizationGroup(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val sortOrder: Int = 0
)