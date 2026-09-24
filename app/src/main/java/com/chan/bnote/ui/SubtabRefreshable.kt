package com.chan.bnote.ui

/**
 * 설교 · 적용 탭처럼 ViewPager2로 스와이프되는 서브탭에서, 이 서브탭이 "다시 보이게 됐을 때"
 * 데이터를 새로 불러와야 할 때 쓴다. 두 가지 경우에 필요하다.
 *
 * 1. 다른 서브탭에서 추가·수정·삭제를 하고 스와이프로 이 서브탭에 돌아왔을 때 — 서브탭마다
 *    각자의 화면 전환(등록 화면 결과 콜백 등)에서만 스스로 다시 불러오기 때문에, 다른 서브탭에서
 *    생긴 변경은 몰랐다.
 * 2. 성경 탭 · 마이페이지 탭 등 다른 하단 탭에 갔다가 이 탭으로 돌아왔을 때 — 하단 탭은
 *    hide()/show() 방식이라 onResume이 다시 안 불린다(FabAddHandler와 같은 이유).
 */
interface SubtabRefreshable {
	fun onSubtabBecameVisible()
}