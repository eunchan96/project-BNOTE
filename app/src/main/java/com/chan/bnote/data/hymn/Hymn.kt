package com.chan.bnote.data.hymn

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hymns")
data class Hymn(
	@PrimaryKey
	val number: Int, // 장 번호, 1~645
	val title: String,
	val categoryId: Long, // 소분류 id (HymnCategory.id, parentId가 있는 쪽)
	val imageFileName: String, // assets/hymns/images/ 아래 파일명, ex) "001장_만복의_근원_하나님.jpg"
	val youtubeSongUrl: String,
	val youtubeMrUrl: String
)