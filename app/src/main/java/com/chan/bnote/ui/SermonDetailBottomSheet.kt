package com.chan.bnote.ui

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
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

		val flexbox =
			view.findViewById<com.google.android.flexbox.FlexboxLayout>(R.id.flexbox_detail_refs)
		val dotView = view.findViewById<View>(R.id.detail_category_dot)
		val memoView = view.findViewById<TextView>(R.id.text_sermon_memo)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)

			// 설교자 이름 + 날짜
			val preacherName =
				sermon.preacherId?.let { db.preacherDao().getById(it)?.name } ?: "설교자 미지정"
			view.findViewById<TextView>(R.id.text_sermon_meta).text =
				"$preacherName · ${DateUtils.formatDate(sermon.sermonDate)}"

			// 카테고리 색상 점
			val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
			if (category != null) {
				val drawable = GradientDrawable()
				drawable.shape = GradientDrawable.OVAL
				drawable.setColor(Color.parseColor(category.colorHex))
				dotView.background = drawable
				dotView.visibility = View.VISIBLE
			} else {
				dotView.visibility = View.GONE
			}

			// 성경 구절 칩
			flexbox.removeAllViews()
			val refs = db.sermonBibleRefDao().getBySermon(sermon.id)
			for (ref in refs) {
				val chip = LayoutInflater.from(requireContext())
					.inflate(R.layout.item_bible_ref_chip, flexbox, false)
				chip.findViewById<TextView>(R.id.text_chip_label).text = ref.toDisplayLabel()
				chip.findViewById<View>(R.id.btn_remove_chip).visibility = View.GONE
				flexbox.addView(chip)
			}

			// 메모 (인용 구절 밑줄 + 롱프레스 말풍선)
			val memoText = sermon.memo.ifBlank { "메모가 없어요" }
			if (sermon.memo.isBlank()) {
				memoView.text = memoText
			} else {
				val (spanned, citations) = CitationBubbleHelper.buildSpannedText(memoText)
				memoView.text = spanned
				CitationBubbleHelper.attachTouchHandling(memoView, { citations }, lifecycleScope)
			}
		}

		// 수정/삭제 아이콘 버튼
		view.findViewById<ImageView>(R.id.btn_edit_sermon).setOnClickListener {
			val editSheet = AddSermonBottomSheet(existingSermon = sermon)
			editSheet.onSaved = { onChanged?.invoke() }
			editSheet.show(parentFragmentManager, "edit_sermon")
			dismiss()
		}

		view.findViewById<ImageView>(R.id.btn_delete_sermon).setOnClickListener {
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