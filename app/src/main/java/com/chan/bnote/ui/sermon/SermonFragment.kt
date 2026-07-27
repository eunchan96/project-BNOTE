package com.chan.bnote.ui.sermon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.chan.bnote.R
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import com.chan.bnote.ui.sermon.bybook.SermonByBookFragment
import com.chan.bnote.ui.sermon.bycalendar.CalendarSermonFragment
import com.chan.bnote.ui.sermon.bypreacher.SermonByPreacherFragment
import com.chan.bnote.ui.sermon.category.CategoryManageActivity

class SermonFragment : Fragment(), TopBarActionHandler {

	private lateinit var subtabCalendar: TextView
	private lateinit var subtabByBook: TextView
	private lateinit var subtabByPreacher: TextView
	private var activeSubFragment: Fragment? = null
	private var activeTabName: String? = null

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		subtabCalendar = view.findViewById(R.id.subtab_calendar)
		subtabByBook = view.findViewById(R.id.subtab_by_book)
		subtabByPreacher = view.findViewById(R.id.subtab_by_preacher)

		subtabCalendar.setOnClickListener {
			switchSubTab(
				CalendarSermonFragment(),
				subtabCalendar,
				"캘린더"
			)
		}
		subtabByBook.setOnClickListener {
			switchSubTab(
				SermonByBookFragment(),
				subtabByBook,
				"성경별"
			)
		}
		subtabByPreacher.setOnClickListener {
			switchSubTab(
				SermonByPreacherFragment(),
				subtabByPreacher,
				"설교자별"
			)
		}

		if (savedInstanceState == null) {
			switchSubTab(CalendarSermonFragment(), subtabCalendar, "캘린더")
		}
	}

	private fun switchSubTab(fragment: Fragment, selected: TextView, tabName: String) {
		activeSubFragment = fragment
		activeTabName = tabName
		childFragmentManager.beginTransaction()
			.replace(R.id.sermon_sub_container, fragment)
			.commit()

		listOf(subtabCalendar, subtabByBook, subtabByPreacher).forEach {
			val isSelected = it == selected
			it.setTextColor(
				resources.getColor(
					if (isSelected) R.color.brown_primary else R.color.bottom_nav_unselected,
					null
				)
			)
		}
	}

	override fun getTopBarConfig() = TopBarConfig(
		title = "설교",
		showMenu = true,
		showSearch = true
	)

	override fun onMenuClicked() {
		val dialog = SermonMenuDialogFragment()
		dialog.sortTarget = activeSubFragment as? SermonSortableFragment
		dialog.sortTabName = activeTabName
		dialog.onCategoryManageClicked = {
			startActivity(Intent(requireContext(), CategoryManageActivity::class.java))
		}
		dialog.onApplicationClicked = {
			startActivity(
				Intent(
					requireContext(),
					com.chan.bnote.ui.application.ApplicationActivity::class.java
				)
			)
		}
		dialog.show(parentFragmentManager, "sermon_menu")
	}

	override fun onSearchClicked() {
		startActivity(Intent(requireContext(), SermonSearchActivity::class.java))
	}
}