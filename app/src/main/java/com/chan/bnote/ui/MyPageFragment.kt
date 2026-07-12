package com.chan.bnote.ui

import TopBarActionHandler
import TopBarConfig
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chan.bnote.R

class MyPageFragment : Fragment(), TopBarActionHandler {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_placeholder, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.findViewById<android.widget.TextView>(R.id.text_placeholder).text =
			"마이페이지 (설정/성경읽기표/올해의 말씀 - D단계 예정)"
	}

	// 마이페이지는 햄버거 없음 -> showMenu = false
	override fun getTopBarConfig() = TopBarConfig(title = "마이페이지", showMenu = false)
}