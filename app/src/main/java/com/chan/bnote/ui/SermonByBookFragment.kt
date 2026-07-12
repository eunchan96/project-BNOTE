package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chan.bnote.R

class SermonByBookFragment : Fragment() {
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_grouped_list, container, false)
	}
	// TODO C-4: 책×장 그리드 UI로 교체
}