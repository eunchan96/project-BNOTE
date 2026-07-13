package com.chan.bnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermon_categories")
data class SermonCategory(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val colorHex: String, // "#FF9800" 형태
	val isDefault: Boolean = false, // 기본 제공 카테고리인지 (구분용, 삭제 제한 등에 활용 가능)
	val sortOrder: Int = 0
)

object DefaultSermonCategories {
	val list = listOf(
		SermonCategory(name = "주일 낮예배", colorHex = "#FB8C00", isDefault = true, sortOrder = 0),
		SermonCategory(name = "주일 오후 예배", colorHex = "#FDD835", isDefault = true, sortOrder = 1),
		SermonCategory(name = "금요예배", colorHex = "#8E24AA", isDefault = true, sortOrder = 2),
		SermonCategory(name = "새벽예배", colorHex = "#795548", isDefault = true, sortOrder = 3),
		SermonCategory(name = "송구영신예배", colorHex = "#212121", isDefault = true, sortOrder = 4),
		SermonCategory(name = "성탄절예배", colorHex = "#E53935", isDefault = true, sortOrder = 5)
	)
}