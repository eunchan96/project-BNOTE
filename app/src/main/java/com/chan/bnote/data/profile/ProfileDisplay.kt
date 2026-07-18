package com.chan.bnote.data.profile

/** 마이페이지 썸네일, 내 정보 화면에서 공통으로 쓰는 표시용 텍스트 포맷. */
object ProfileDisplay {

	fun nameText(profile: UserProfile?): String =
		profile?.name?.takeIf { it.isNotBlank() } ?: "이름을 등록해주세요"

	/** "교회 · 부서 · 직분" 형태로, 비어있는 항목은 건너뛴다. */
	fun metaText(profile: UserProfile?): String {
		val parts = listOfNotNull(
			profile?.church?.takeIf { it.isNotBlank() },
			profile?.department?.takeIf { it.isNotBlank() },
			profile?.position?.takeIf { it.isNotBlank() }
		)
		return if (parts.isEmpty()) "탭해서 내 정보를 등록해보세요" else parts.joinToString(" · ")
	}
}