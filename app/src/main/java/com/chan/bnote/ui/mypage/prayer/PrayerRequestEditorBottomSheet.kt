package com.chan.bnote.ui.mypage.prayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.prayer.PrayerRequest
import com.chan.bnote.ui.FixedBottomSheetDialogFragment
import kotlinx.coroutines.launch

/** 기도제목 작성/수정 화면. 예전엔 plain AlertDialog+EditText였는데, 다른 편집 화면들과 같은
 * bottom sheet 톤으로 통일했다. */
class PrayerRequestEditorBottomSheet : FixedBottomSheetDialogFragment() {

	/** null이면 새로 작성, 있으면 그 항목을 수정한다. */
	var existing: PrayerRequest? = null
	var onChanged: (() -> Unit)? = null

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_prayer_editor, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val editText = view.findViewById<EditText>(R.id.edit_prayer_content)
		view.findViewById<TextView>(R.id.text_sheet_title).text =
			if (existing != null) "기도제목 수정" else "기도제목 추가"
		existing?.let { editText.setText(it.content) }
		editText.requestFocus()

		view.findViewById<TextView>(R.id.btn_save_prayer).setOnClickListener {
			val text = editText.text.toString().trim()
			if (text.isEmpty()) {
				dismiss()
				return@setOnClickListener
			}
			lifecycleScope.launch {
				val db = BibleDatabase.getInstance(requireContext().applicationContext)
				val current = existing
				if (current != null) {
					db.prayerRequestDao().update(current.copy(content = text))
				} else {
					db.prayerRequestDao().insert(PrayerRequest(content = text))
				}
				onChanged?.invoke()
				dismiss()
			}
		}
	}
}