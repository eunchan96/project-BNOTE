package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chan.bnote.R

class SermonFragment : Fragment(), TopBarActionHandler {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_placeholder, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.findViewById<android.widget.TextView>(R.id.text_placeholder).text =
			"설교 탭 (캘린더/성경별/설교자 - C단계 예정)"
	}

	// TODO C단계: 캘린더 / 성경별 / 설교자 서브탭
	override fun getTopBarConfig() = TopBarConfig(title = "설교")
}