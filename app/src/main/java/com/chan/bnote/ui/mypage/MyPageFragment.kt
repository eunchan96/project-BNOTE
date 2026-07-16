package com.chan.bnote.ui.mypage

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

class MyPageFragment : Fragment(), TopBarActionHandler {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_mypage, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.menu_settings).setOnClickListener {
			startActivity(Intent(requireContext(), SettingsActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_reading_plan).setOnClickListener {
			startActivity(Intent(requireContext(), ReadingPlanActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_verse_of_year).setOnClickListener {
			startActivity(Intent(requireContext(), VerseOfYearActivity::class.java))
		}
	}

	override fun getTopBarConfig() = TopBarConfig(title = "마이페이지", showMenu = false)
}