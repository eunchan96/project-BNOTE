package com.chan.bnote.ui.bible

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.Translation
import com.chan.bnote.ui.common.SimpleListAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TranslationPickerBottomSheet(
	private val currentPrimary: Translation
) : BottomSheetDialogFragment() {

	// primary는 항상 값 있음, secondary는 "선택 안 함"일 경우 null
	var onTranslationsSelected: ((primary: Translation, secondary: Translation?) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var titleView: TextView
	private lateinit var backButton: TextView
	private var selectedPrimary: Translation = currentPrimary

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_translation, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_translation_picker)
		titleView = view.findViewById(R.id.text_sheet_title)
		backButton = view.findViewById(R.id.btn_back)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		showPrimaryList()
	}

	private fun showPrimaryList() {
		titleView.text = "주성경 선택"
		backButton.visibility = View.GONE

		val names = Translation.values().map { it.displayName }
		recyclerView.adapter = SimpleListAdapter(names) { position ->
			selectedPrimary = Translation.values()[position]
			showSecondaryList()
		}
	}

	private fun showSecondaryList() {
		titleView.text = "함께보기 선택"
		backButton.visibility = View.VISIBLE
		backButton.setOnClickListener { showPrimaryList() }

		// 주성경으로 고른건 함께보기 목록에서 제외
		val options = Translation.values().filter { it != selectedPrimary }
		val labels = options.map { it.displayName } + "선택 안 함" // "선택 안 함"을 맨 뒤로

		recyclerView.adapter = SimpleListAdapter(labels) { position ->
			if (position == options.size) {
				// 마지막 항목 = "선택 안 함"
				onTranslationsSelected?.invoke(selectedPrimary, null)
			} else {
				onTranslationsSelected?.invoke(selectedPrimary, options[position])
			}
			dismiss()
		}
	}
}