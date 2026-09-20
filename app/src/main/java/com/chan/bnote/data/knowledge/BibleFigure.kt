package com.chan.bnote.data.knowledge

data class BibleFigure(
	val id: String,
	val name: String,
	val otherNames: String,
	val category: String,
	val era: String,
	val summary: String,
	val description: String,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)