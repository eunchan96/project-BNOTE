package com.chan.bnote.data.knowledge

data class VerseRef(
	val bookId: Int,
	val chapter: Int,
	val verseStart: Int,
	val verseEnd: Int
)

data class TopicalVerseGroup(
	val id: String,
	val title: String,
	val verses: List<VerseRef>
)