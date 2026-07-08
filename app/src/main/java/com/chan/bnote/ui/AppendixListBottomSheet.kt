package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AppendixListBottomSheet : BottomSheetDialogFragment() {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(
			R.layout.bottom_sheet_book_chapter,
			container,
			false
		) // 책 선택과 같은 뼈대 재사용
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<android.widget.TextView>(R.id.text_sheet_title).text = "부록"
		view.findViewById<View>(R.id.btn_back).visibility = View.GONE
		view.findViewById<View>(R.id.scroll_book_grid).visibility = View.GONE

		val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_picker)
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		val items = listOf("주기도문", "사도신경", "십계명", "교독문")
		recyclerView.adapter = SimpleListAdapter(items) { position ->
			// TODO: 실제 본문 화면 연결 (번역본/버전 정하신 뒤 텍스트 채워주세요)
			Toast.makeText(requireContext(), "${items[position]} (본문 준비 중)", Toast.LENGTH_SHORT)
				.show()
		}
	}
}