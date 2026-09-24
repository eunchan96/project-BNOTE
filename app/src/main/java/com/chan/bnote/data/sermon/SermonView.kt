package com.chan.bnote.data.sermon

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 마이페이지 "최근 활동"에 보여줄, 최근에 열어본 설교 기록. 내용을 고치지 않고 열어만 봐도
 * 갱신된다(성경 장의 RecentChapterView와 같은 방식). */
@Entity(
	tableName = "sermon_views",
	indices = [Index(value = ["sermonId"], unique = true)]
)
data class SermonView(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val sermonId: Long,
	val viewedAt: Long = System.currentTimeMillis()
)