package com.chan.bnote.data.mypage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 마이페이지 "최근 활동"에 보여줄, 최근에 열어본 성경 장 기록. 읽음 표시 여부와 무관하게 열람 시마다 갱신된다. */
@Entity(
	tableName = "recent_chapter_views",
	indices = [Index(value = ["bookId", "chapter"], unique = true)]
)
data class RecentChapterView(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val bookId: Int,
	val chapter: Int,
	val viewedAt: Long = System.currentTimeMillis()
)