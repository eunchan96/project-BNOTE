package com.chan.bnote.data.places

data class BiblePlace(
	val id: String,
	val name: String,
	val otherNames: String,
	val category: String,
	val region: String,
	val summary: String,
	val description: String,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)