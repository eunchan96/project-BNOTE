package com.chan.bnote.ui.sermon

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.chan.bnote.ui.application.bycalendar.CalendarApplicationFragment
import com.chan.bnote.ui.sermon.bybook.SermonByBookFragment
import com.chan.bnote.ui.sermon.bycalendar.CalendarSermonFragment

/** 설교 탭의 캘린더 / 성경별 / 적용 서브탭을 스와이프로 넘길 수 있게 해주는 어댑터. */
class SermonSubPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

	// 지금 정렬 대상(SermonSortableFragment)을 찾을 때 내부 태그 규칙에 의존하지 않기 위해
	// 직접 만든 프래그먼트를 위치별로 기억해둔다.
	private val createdFragments = mutableMapOf<Int, Fragment>()

	override fun getItemCount(): Int = 3

	override fun createFragment(position: Int): Fragment {
		val fragment = when (position) {
			0 -> CalendarSermonFragment()
			1 -> SermonByBookFragment()
			else -> CalendarApplicationFragment()
		}
		createdFragments[position] = fragment
		return fragment
	}

	fun fragmentAt(position: Int): Fragment? = createdFragments[position]
}