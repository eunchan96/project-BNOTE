package com.chan.bnote.data.genealogy

data class GenealogyEntry(
	val name: String,
	val relation: String,
	val note: String
)

data class GenealogyChart(
	val id: String,
	val title: String,
	val description: String,
	val entries: List<GenealogyEntry>,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)