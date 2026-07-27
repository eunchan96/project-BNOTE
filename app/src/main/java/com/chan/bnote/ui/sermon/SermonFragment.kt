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
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.sermon.category.CategoryManageActivity

class SermonFragment : Fragment(), TopBarActionHandler {

	private lateinit var subtabCalendar: TextView
	private lateinit var subtabByBook: TextView
	private lateinit var subtabApplication: TextView
	private lateinit var viewPager: ViewPager2
	private var activeTabName: String? = null

	private val subtabs get() = listOf(subtabCalendar, subtabByBook, subtabApplication)
	private val tabNames = listOf("캘린더", "성경별", "적용하기")

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
		viewPager.adapter = SermonSubPagerAdapter(this)

		subtabCalendar.setOnClickListener { viewPager.setCurrentItem(0, true) }
		subtabByBook.setOnClickListener { viewPager.setCurrentItem(1, true) }
		subtabApplication.setOnClickListener { viewPager.setCurrentItem(2, true) }

		viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateSelectedTab(position)
			}
		})

		updateSelectedTab(0)
	}

	private fun updateSelectedTab(position: Int) {
		activeTabName = tabNames.getOrNull(position)
		subtabs.forEachIndexed { index, tab ->
			tab.setTextColor(
				resources.getColor(
					if (index == position) R.color.brown_primary else R.color.bottom_nav_unselected,
					null
				)
			)
		}
	}

	/** 지금 보이는 서브탭 프래그먼트(정렬 메뉴 대상 확인용). */
	private fun currentSubFragment(): Fragment? {
		return (viewPager.adapter as? SermonSubPagerAdapter)?.fragmentAt(viewPager.currentItem)
	}

	override fun getTopBarConfig() = TopBarConfig(
		title = "설교 · 적용",
		showMenu = true,
		showSearch = true
	)

	override fun onMenuClicked() {
		val dialog = SermonMenuDialogFragment()
		dialog.sortTarget = currentSubFragment() as? SermonSortableFragment
		dialog.sortTabName = activeTabName
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