package com.chan.bnote.data.application

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "application_categories")
data class ApplicationCategory(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val colorHex: String,
	val isDefault: Boolean = false,
	val sortOrder: Int = 0
)

object DefaultApplicationCategories {
	val list = listOf(
		ApplicationCategory(
			name = "통독",
			colorHex = "#8E24AA",
			isDefault = true,
			sortOrder = 0
		), // 보라색
		ApplicationCategory(
			name = "설교",
			colorHex = "#43A047",
			isDefault = true,
			sortOrder = 1
		), // 초록색
		ApplicationCategory(
			name = "교제",
			colorHex = "#FB8C00",
			isDefault = true,
			sortOrder = 2
		)  // 주황색
	)
}