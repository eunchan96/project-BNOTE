package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "verse_of_year")
data class VerseOfYear(
	@PrimaryKey
	val year: Int,
	val note: String = ""
)