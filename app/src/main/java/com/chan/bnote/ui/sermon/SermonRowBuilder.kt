package com.chan.bnote.ui.sermon

import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon

object SermonRowBuilder {
	suspend fun build(
		db: BibleDatabase,
		sermons: List<Sermon>,
		useDateLabel: Boolean = true
	): List<SermonRowData> {
		return sermons.map { sermon ->
			val firstRef = db.sermonBibleRefDao().getFirstRef(sermon.id)
			val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
			val rightTopLabel = if (useDateLabel) {
				DateUtils.formatDateShort(sermon.sermonDate)
			} else {
				sermon.preacherId?.let { db.preacherDao().getById(it)?.name } ?: "설교자 미지정"
			}
			SermonRowData(
				sermon = sermon,
				colorHex = category?.colorHex,
				dateLabel = rightTopLabel,
				bibleRefLabel = firstRef?.toShortLabel() ?: ""
			)
		}
	}
}