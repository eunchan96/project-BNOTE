package com.chan.bnote.ui.sermon

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
import com.chan.bnote.ui.sermon.calendar.CalendarSermonFragment

class SermonFragment : Fragment(), TopBarActionHandler {

	private lateinit var subtabCalendar: TextView
	private lateinit var subtabByBook: TextView
	private lateinit var subtabByPreacher: TextView

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

		subtabCalendar.setOnClickListener { switchSubTab(CalendarSermonFragment(), subtabCalendar) }
		subtabByBook.setOnClickListener { switchSubTab(SermonByBookFragment(), subtabByBook) }
		subtabByPreacher.setOnClickListener {
			switchSubTab(
				SermonByPreacherFragment(),
				subtabByPreacher
			)
		}

		if (savedInstanceState == null) {
			switchSubTab(CalendarSermonFragment(), subtabCalendar)
		}
	}

	private fun switchSubTab(fragment: Fragment, selected: TextView) {
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
		dialog.onCategoryManageClicked = {
			CategoryManageBottomSheet().show(parentFragmentManager, "category_manage")
		}
		dialog.show(parentFragmentManager, "sermon_menu")
	}

	override fun onSearchClicked() {
		SermonSearchBottomSheet().show(parentFragmentManager, "sermon_search")
	}
}