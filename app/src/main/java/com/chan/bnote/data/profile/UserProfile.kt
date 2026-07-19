package com.chan.bnote.data.profile

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 앱 내 유일한 사용자 프로필 (id는 항상 1로 고정되는 싱글턴 로우). */
@Entity(tableName = "user_profile")
data class UserProfile(
	@PrimaryKey
	val id: Int = 1,
	val photoPath: String? = null,
	val name: String = "",
	val church: String = "", // 교회
	val department: String = "", // 부서
	val position: String = "" // 직분
)