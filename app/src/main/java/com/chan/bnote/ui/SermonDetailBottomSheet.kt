package com.chan.bnote.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleBooks
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.Sermon
import kotlinx.coroutines.launch

class SermonDetailBottomSheet(private val sermon: Sermon) : DraggableBottomSheet() {

	override val peekHeightRatio = 0.6f

	var onChanged: (() -> Unit)? = null // 수정/삭제 후 목록 갱신용

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.bottom_sheet_sermon_detail, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<TextView>(R.id.text_sermon_title).text = sermon.title
		view.findViewById<TextView>(R.id.text_sermon_meta).text =
			"${sermon.preacher} · ${DateUtils.formatDate(sermon.sermonDate)}"
		view.findViewById<TextView>(R.id.text_sermon_memo).text =
			sermon.memo.ifBlank { "메모가 없어요" }

		val flexbox =
			view.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flexbox_detail_refs)
		val dotView = view.findViewById<View>(R.id.detail_category_dot)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			sermon.categoryId?.let { catId ->
				val category = db.sermonCategoryDao().getById(catId)
				if (category != null) {
					val drawable = GradientDrawable()
					drawable.shape = GradientDrawable.OVAL
					drawable.setColor(Color.parseColor(category.colorHex))
					dotView.background = drawable
					dotView.visibility = View.VISIBLE
				} else {
					dotView.visibility = View.GONE
				}
			} ?: run { dotView.visibility = View.GONE }

			val refs = db.sermonBibleRefDao().getBySermon(sermon.id)
			for (ref in refs) {
				val chip = LayoutInflater.from(requireContext())
					.inflate(R.layout.item_bible_ref_chip, flexbox, false)
				chip.findViewById<TextView>(R.id.text_chip_label).text =
					"${BibleBooks.nameOf(ref.startBookId)} ${ref.startChapter}:${ref.startVerse}~${ref.endChapter}:${ref.endVerse}"
				chip.findViewById<View>(R.id.btn_remove_chip).visibility =
					View.GONE // 상세보기에서는 삭제 버튼 숨김
				flexbox.addView(chip)
			}
		}

		view.findViewById<TextView>(R.id.btn_edit_sermon).setOnClickListener {
			val editSheet = AddSermonBottomSheet(existingSermon = sermon)
			editSheet.onSaved = { onChanged?.invoke() }
			editSheet.show(parentFragmentManager, "edit_sermon")
			dismiss()
		}

		view.findViewById<TextView>(R.id.btn_delete_sermon).setOnClickListener {
			AlertDialog.Builder(requireContext())
				.setTitle("설교 삭제")
				.setMessage("'${sermon.title}'을(를) 삭제할까요?")
				.setPositiveButton("삭제") { _, _ ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(requireContext().applicationContext)
						db.sermonBibleRefDao().deleteBySermon(sermon.id)
						db.sermonDao().delete(sermon)
						onChanged?.invoke()
						dismiss()
					}
				}
				.setNegativeButton("취소", null)
				.show()
		}
	}
}