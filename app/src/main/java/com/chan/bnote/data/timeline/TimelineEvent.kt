package com.chan.bnote.data.timeline

data class TimelineEvent(
	val id: String,
	val era: String,
	val period: String,
	val title: String,
	val description: String,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)