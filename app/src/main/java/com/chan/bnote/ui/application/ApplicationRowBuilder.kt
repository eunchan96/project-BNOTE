package com.chan.bnote.ui.application

import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.application.Application

object ApplicationRowBuilder {
	suspend fun build(
		db: BibleDatabase,
		applications: List<Application>,
		useDateLabel: Boolean = true
	): List<ApplicationRowData> {
		return applications.map { app ->
			val refs = db.applicationBibleRefDao().getByApplication(app.id)
			val category = app.categoryId?.let { db.applicationCategoryDao().getById(it) }
			val rightTopLabel = if (useDateLabel) {
				DateUtils.formatDateShort(app.applicationDate)
			} else {
				category?.name ?: "미분류"
			}

			// 추가한 본문이 있으면 그걸, 없으면 연결한 설교 제목들을 보여준다.
			val bottomLabel = if (refs.isNotEmpty()) {
				refs.joinToString(", ") { it.toDisplayLabel() }
			} else {
				val links = db.applicationSermonLinkDao().getByApplication(app.id)
				val sermonTitles = links.mapNotNull { db.sermonDao().getById(it.sermonId)?.title }
				sermonTitles.joinToString(", ")
			}

			ApplicationRowData(
				application = app,
				colorHex = category?.colorHex,
				rightTopLabel = rightTopLabel,
				rightBottomLabel = bottomLabel
			)
		}
	}
}