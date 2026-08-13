package com.chan.bnote.data.knowledge

data class BibleUnit(
	val id: String,
	val title: String,
	val category: String,
	/** 카테고리 안에서 더 세분화할 때만 쓰는 값(예: "부피"의 "액체"/"마른 곡물",
	 * "화폐"의 "은화"/"동전"/"주조화폐"/"무게 단위", "거리·길이"의 "거리"/"길이").
	 * 굳이 나눌 필요 없는 카테고리(무게, 시간)는 null. */
	val subcategory: String?,
	val summary: String,
	val description: String,
	val keyBookId: Int,
	val keyChapter: Int,
	val keyVerseLabel: String
)