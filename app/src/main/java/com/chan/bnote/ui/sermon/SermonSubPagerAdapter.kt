package com.chan.bnote.ui.sermon

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chan.bnote.ui.application.CalendarApplicationFragment
import com.chan.bnote.ui.sermon.bybook.SermonByBookFragment
import com.chan.bnote.ui.sermon.bycalendar.CalendarSermonFragment

/** 설교 탭의 캘린더 / 성경별 / 적용 서브탭을 스와이프로 넘길 수 있게 해주는 어댑터.
 *
 * 지금 화면에 보이는 프래그먼트가 필요할 땐(예: FAB 클릭, 정렬 버튼) 이 어댑터에 따로 캐시를
 * 두지 않고, 호출하는 쪽에서 FragmentManager.findFragmentByTag("f" + position)으로 직접
 * 찾는다 — FragmentStateAdapter가 각 페이지를 그 태그로 등록해서 관리하므로, 자체 캐시를
 * 따로 두면 ViewPager2가 화면 밖 페이지를 파괴/재생성하는 시점과 어긋나 옛 참조가 남는
 * 문제가 있었다. */
class SermonSubPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

	override fun getItemCount(): Int = 3

	override fun createFragment(position: Int): Fragment = when (position) {
		0 -> CalendarSermonFragment()
		1 -> SermonByBookFragment()
		else -> CalendarApplicationFragment()
	}
}