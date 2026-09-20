package com.chan.bnote.ui

/**
 * 설교 · 적용 탭처럼 ViewPager2로 스와이프되는 서브탭에서, "+" 버튼을 화면 밖 고정 위치에
 * 하나만 두고(스와이프해도 안 움직이도록) 지금 보이는 서브탭 프래그먼트에게 클릭을 위임할 때 쓴다.
 */
interface FabAddHandler {
	fun onFabAddClicked()
}