package com.chan.bnote.data.sermon

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sermons")
data class Sermon(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0,
	val title: String,
	val preacherId: Long?,
	val sermonDate: Long,
	val categoryId: Long?,
	val memo: String = "",
	// 검색 전용 순수 텍스트. memo는 굵게/밑줄/색 서식이 하나라도 있으면 안드로이드 Html.toHtml()이 한글을 포함한 모든 글자를 HTML 문자 참조(&#45208; 등)로 바꿔버려서,
	// SQL의 LIKE로 그 안의 실제 글자를 찾을 수 없었다(서식 없는 메모만 우연히 검색되던 원인).
	// 저장할 때 서식은 memo에 그대로 두고, 화면에 보이는 글자만 따로 여기 담아서 검색은 이 칸을 본다.
	val memoSearchText: String = "",
	val link: String? = null,
	val createdAt: Long = System.currentTimeMillis()
)