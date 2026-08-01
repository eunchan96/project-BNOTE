package com.chan.bnote.ui.bible.picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.bible.Translation
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import com.chan.bnote.ui.common.SimpleListAdapter

class TranslationPickerBottomSheet(
	private val currentPrimary: Translation,
	private val currentSecondary: Translation?
) : FixedBottomSheetDialogFragment() {

	// primary는 항상 값 있음, secondary는 "선택 안 함"일 경우 null
	var onTranslationsSelected: ((primary: Translation, secondary: Translation?) -> Unit)? = null

	private lateinit var recyclerView: RecyclerView
	private lateinit var titleView: TextView
	private lateinit var tabBarContainer: LinearLayout

	// 이미 주성경이 정해진 상태로 열리므로, 처음부터 "함께보기" 탭으로 바로 넘어갈 수 있다.
	private var selectedPrimary: Translation = currentPrimary

	private enum class Step { PRIMARY, SECONDARY }

	private var currentStep = Step.PRIMARY

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_translation, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		recyclerView = view.findViewById(R.id.recycler_translation_picker)
		titleView = view.findViewById(R.id.text_sheet_title)
		tabBarContainer = view.findViewById(R.id.container_tab_bar)
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		goToStep(Step.PRIMARY)
	}

	/** 탭 이동(주성경 → 함께보기)과 화면 갱신을 함께 처리하는 진입점. */
	private fun goToStep(step: Step) {
		currentStep = step
		updateHeader()
		renderTabs()
		when (step) {
			Step.PRIMARY -> showPrimaryList()
			Step.SECONDARY -> showSecondaryList()
		}
	}

	private fun updateHeader() {
		titleView.text = when (currentStep) {
			Step.PRIMARY -> "주성경 선택"
			Step.SECONDARY -> "${selectedPrimary.displayName} + 함께보기 선택"
		}
	}

	private fun renderTabs() {
		val tabs = listOf(
			PickerTab(label = "주성경", enabled = true, selected = currentStep == Step.PRIMARY) {
				goToStep(Step.PRIMARY)
			},
			PickerTab(label = "함께보기", enabled = true, selected = currentStep == Step.SECONDARY) {
				goToStep(Step.SECONDARY)
			}
		)
		renderPickerTabs(requireContext(), tabBarContainer, tabs)
	}

	private fun showPrimaryList() {
		val names = Translation.values().map { it.displayName }
		val selectedIndex = Translation.values().indexOf(selectedPrimary)
		recyclerView.adapter = SimpleListAdapter(names, selectedIndex) { position ->
			selectedPrimary = Translation.values()[position]
			goToStep(Step.SECONDARY)
		}
	}

	private fun showSecondaryList() {
		// 주성경으로 고른건 함께보기 목록에서 제외
		val options = Translation.values().filter { it != selectedPrimary }
		val labels = options.map { it.displayName } + "선택 안 함" // "선택 안 함"을 맨 뒤로
		val selectedIndex =
			if (currentSecondary == null) options.size else options.indexOf(currentSecondary)

		recyclerView.adapter = SimpleListAdapter(labels, selectedIndex) { position ->
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