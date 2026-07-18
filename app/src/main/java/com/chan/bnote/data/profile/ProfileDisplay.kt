package com.chan.bnote.data.profile

/** 마이페이지 썸네일, 내 정보 화면에서 공통으로 쓰는 표시용 텍스트 포맷. */
object ProfileDisplay {

	fun nameText(profile: UserProfile?): String =
		profile?.name?.takeIf { it.isNotBlank() } ?: "이름을 등록해주세요"

	/** 이름 옆에 붙는 직분 텍스트. 이름이 등록되지 않았으면 붙이지 않는다. */
	fun positionText(profile: UserProfile?): String {
		if (profile?.name.isNullOrBlank()) return ""
		return profile?.position?.takeIf { it.isNotBlank() } ?: ""
	}

	/** 마이페이지 썸네일용: "교회 · 부서" 형태. 둘 다 없으면 탭을 유도하는 문구. */
	fun thumbnailMetaText(profile: UserProfile?): String {
		val parts = listOfNotNull(
			profile?.church?.takeIf { it.isNotBlank() },
			profile?.department?.takeIf { it.isNotBlank() }
		)
		return if (parts.isEmpty()) "탭해서 내 정보를 등록해보세요" else parts.joinToString(" · ")
	}

	/**
	 * 내 정보 화면용: 채워진 항목("교회 · 부서")은 그대로 보여주고,
	 * 비어있는 항목은 "OO을 등록해주세요" 식으로 안내한다.
	 * 이미 이름 옆으로 옮긴 직분도, 등록이 안 되어 있으면 여기서 다시 안내 대상에 포함한다.
	 */
	fun profilePageMetaText(profile: UserProfile?): String {
		val filled = listOfNotNull(
			profile?.church?.takeIf { it.isNotBlank() },
			profile?.department?.takeIf { it.isNotBlank() }
		)
		val missing = buildList {
			if (profile?.church.isNullOrBlank()) add("교회")
			if (profile?.department.isNullOrBlank()) add("부서")
			if (profile?.position.isNullOrBlank()) add("직분")
		}
		if (missing.isEmpty()) return filled.joinToString(" · ")

		val missingPhrase = if (missing.size == 1) {
			missing[0]
		} else {
			val allButLast = missing.dropLast(1)
			val connector = andParticle(allButLast.last())
			allButLast.joinToString(", ") + connector + " " + missing.last()
		}
		val guide = "$missingPhrase${objectParticle(missing.last())} 등록해주세요"
		return if (filled.isEmpty()) guide else filled.joinToString(" · ") + " · " + guide
	}

	/** 한글 종성(받침) 유무에 따라 을/를, 와/과 조사를 고른다. */
	private fun hasFinalConsonant(word: String): Boolean {
		val last = word.lastOrNull() ?: return false
		if (last.code !in 0xAC00..0xD7A3) return false
		return (last.code - 0xAC00) % 28 != 0
	}

	private fun objectParticle(word: String) = if (hasFinalConsonant(word)) "을" else "를"
	private fun andParticle(word: String) = if (hasFinalConsonant(word)) "과" else "와"
}