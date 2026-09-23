package com.chan.bnote.ui.sermon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.chan.bnote.R
import com.chan.bnote.ui.FabAddHandler
import com.chan.bnote.ui.SubtabRefreshable
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.sermon.category.CategoryManageActivity

class SermonFragment : Fragment(), TopBarActionHandler {

	private lateinit var subtabCalendar: TextView
	private lateinit var subtabByBook: TextView
	private lateinit var subtabApplication: TextView
	private lateinit var viewPager: ViewPager2

	private val subtabs get() = listOf(subtabCalendar, subtabByBook, subtabApplication)

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		subtabCalendar = view.findViewById(R.id.subtab_calendar)
		subtabByBook = view.findViewById(R.id.subtab_by_book)
		subtabApplication = view.findViewById(R.id.subtab_by_preacher)
		subtabApplication.text = "적용하기"

		viewPager = view.findViewById(R.id.sermon_view_pager)
		val pagerAdapter = SermonSubPagerAdapter(this)
		viewPager.adapter = pagerAdapter

		// 예전엔 어댑터가 자체적으로 만든 프래그먼트를 Map에 담아뒀다가 그걸로 FAB 클릭을
		// 처리했는데, ViewPager2가 화면 밖 페이지를 파괴했다가 다시 만드는 시점과 그 Map이
		// 어긋나면(예: 스와이프를 빠르게 여러 번 하거나 스크롤이 많이 밀렸다 온 경우) 이미
		// 뷰가 없어진 옛 프래그먼트 참조가 남아있어서 버튼을 눌러도 아무 반응이 없는 문제가
		// 있었다. FragmentManager에 실제로 붙어 있는(지금 화면에 보이는) 프래그먼트를 태그로
		// 직접 찾으면 이런 어긋남이 생기지 않는다. FragmentStateAdapter는 각 페이지를
		// "f" + position 태그로 등록해서 관리한다.
		view.findViewById<TextView>(R.id.fab_add_sermon_tab).setOnClickListener {
			val currentFragment =
				childFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
			(currentFragment as? FabAddHandler)?.onFabAddClicked()
		}

		subtabCalendar.setOnClickListener { viewPager.setCurrentItem(0, true) }
		subtabByBook.setOnClickListener { viewPager.setCurrentItem(1, true) }
		subtabApplication.setOnClickListener { viewPager.setCurrentItem(2, true) }

		viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateSelectedTab(position)
				// 다른 서브탭에서 방금 추가·수정·삭제했을 수 있으니, 스와이프로 넘어온 서브탭도
				// 다시 불러오게 한다(아래 refreshCurrentSubtab()과 같은 이유).
				refreshCurrentSubtab()
			}
		})

		updateSelectedTab(0)
	}

	/** 하단 탭은 hide()/show() 방식이라, 다른 탭(성경·마이페이지)에 갔다가 이 탭으로 돌아와도
	 * onResume은 다시 안 불린다. 그 사이 어느 서브탭에서든 생겼을 변경을 반영하도록, 돌아올 때마다
	 * 지금 보이는 서브탭을 다시 불러온다(MyPageFragment.onHiddenChanged와 같은 이유). */
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) refreshCurrentSubtab()
	}

	private fun refreshCurrentSubtab() {
		if (!::viewPager.isInitialized) return
		val currentFragment =
			childFragmentManager.findFragmentByTag("f${viewPager.currentItem}")
		(currentFragment as? SubtabRefreshable)?.onSubtabBecameVisible()
	}

	/** 내 정보 화면의 "설교노트"/"적용" 기록 카드처럼, 다른 화면에서 이 탭으로 이동하면서
	 * 특정 서브탭(캘린더=0, 성경별=1, 적용=2)까지 바로 보여주고 싶을 때 MainActivity가 부른다.
	 * onViewCreated가 아직 안 끝났으면(뷰페이저가 없으면) 조용히 무시한다 — switchTo(...)가
	 * commitNow()를 쓰므로 실제로는 이 프래그먼트가 막 add()된 경우에도 항상 뷰가 준비된 뒤에
	 * 불린다. */
	fun selectSubtab(index: Int) {
		if (!::viewPager.isInitialized) return
		viewPager.setCurrentItem(index, false)
	}

	private fun updateSelectedTab(position: Int) {
		subtabs.forEachIndexed { index, tab ->
			tab.setTextColor(
				resources.getColor(
					if (index == position) R.color.brown_primary else R.color.bottom_nav_unselected,
					null
				)
			)
		}
	}

	override fun getTopBarConfig() = TopBarConfig(
		title = "설교 · 적용",
		showMenu = true,
		showSearch = true
	)

	override fun onMenuClicked() {
		val dialog = SermonMenuDialogFragment()
		dialog.onCategoryManageClicked = {
			startActivity(Intent(requireContext(), CategoryManageActivity::class.java))
		}
		dialog.onPreacherManageClicked = {
			startActivity(
				Intent(
					requireContext(),
					com.chan.bnote.ui.sermon.bypreacher.PreacherManageActivity::class.java
				)
			)
		}
		dialog.onApplicationCategoryManageClicked = {
			startActivity(
				Intent(
					requireContext(),
					com.chan.bnote.ui.application.category.ApplicationCategoryManageActivity::class.java
				)
			)
		}
		dialog.show(parentFragmentManager, "sermon_menu")
	}

	override fun onSearchClicked() {
		startActivity(Intent(requireContext(), SermonSearchActivity::class.java))
	}
}