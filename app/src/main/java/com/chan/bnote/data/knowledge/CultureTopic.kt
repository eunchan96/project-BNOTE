package com.chan.bnote.data.knowledge

data class CultureTopic(
	val id: String,
	val title: String,
	val category: String,
	val summary: String,
	val description: String,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)