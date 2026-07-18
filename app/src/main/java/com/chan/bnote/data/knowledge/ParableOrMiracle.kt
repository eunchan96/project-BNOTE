package com.chan.bnote.data.knowledge

data class ParableOrMiracle(
	val id: String,
	val title: String,
	val type: String, // "비유" 또는 "이적"
	val summary: String,
	val description: String,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)