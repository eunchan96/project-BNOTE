package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "copy_format_presets")
data class CopyFormatPreset(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val name: String,
	val configJson: String,
	val createdAt: Long = System.currentTimeMillis()
) {
	fun toConfig(): CopyFormatConfig = CopyFormatConfig.fromJson(configJson)
}