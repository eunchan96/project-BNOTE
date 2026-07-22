package com.chan.bnote.ui.mypage.settings

/**
 * 업데이트 내역. 버전을 올려서 배포할 때마다(build.gradle.kts의 versionCode/versionName을 올릴 때)
 * 여기에 새 항목을 반드시 추가한다.
 */
object VersionHistory {

	data class Entry(val version: String, val changes: List<String>)

	val entries = listOf(
		Entry(
			version = "1.0",
			changes = listOf(
				"첫 정식 배포",
				"성경 읽기, 형광펜·북마크·스크랩, 구절/단어 메모, 설교노트, 찬송가, 부록, 성경 배경지식",
				"마이페이지(성경읽기표, 올해 약속의 말씀, 기도제목, 암송 구절), 알림, 데이터 내보내기/불러오기"
			)
		)
	)
}