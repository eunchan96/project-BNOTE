package com.chan.bnote.ui

import TopBarActionHandler
import TopBarConfig
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.chan.bnote.R

class MyPageFragment : Fragment(), TopBarActionHandler {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_mypage, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.menu_settings).setOnClickListener {
			SettingsBottomSheet().show(parentFragmentManager, "settings")
		}
		view.findViewById<TextView>(R.id.menu_reading_plan).setOnClickListener {
			ReadingPlanBottomSheet().show(parentFragmentManager, "reading_plan")
		}
		view.findViewById<TextView>(R.id.menu_verse_of_year).setOnClickListener {
			VerseOfYearBottomSheet().show(parentFragmentManager, "verse_of_year")
		}
	}

	override fun getTopBarConfig() = TopBarConfig(title = "마이페이지", showMenu = false)
}