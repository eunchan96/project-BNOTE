package com.chan.bnote.data.application

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 마이페이지 "최근 활동"에 보여줄, 최근에 열어본 적용 기록. 내용을 고치지 않고 열어만 봐도
 * 갱신된다(설교의 SermonView와 같은 방식). */
@Entity(
	tableName = "application_views",
	indices = [Index(value = ["applicationId"], unique = true)]
)
data class ApplicationView(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val applicationId: Long,
	val viewedAt: Long = System.currentTimeMillis()
)