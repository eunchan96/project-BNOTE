package com.chan.bnote.ui.sermon

/** 설교 탭의 서브탭(캘린더/성경별/설교자별)이 이 인터페이스를 구현하면, 상단바 메뉴(≡)에 그 탭에
 * 맞는 정렬 옵션이 자동으로 나타난다. */
interface SermonSortableFragment {
	/** (내부 코드, 화면에 보일 이름) 목록. 화면에 표시될 순서 그대로. */
	fun getSortOptions(): List<Pair<String, String>>
	fun getCurrentSortMode(): String
	fun setSortMode(mode: String)
}