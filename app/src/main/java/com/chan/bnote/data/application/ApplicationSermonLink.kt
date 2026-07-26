package com.chan.bnote.data.application

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 적용 화면에서 "설교 추가"로 붙인 설교들(칩으로 표시). */
@Entity(tableName = "application_sermon_links")
data class ApplicationSermonLink(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val applicationId: Long,
	val sermonId: Long
)