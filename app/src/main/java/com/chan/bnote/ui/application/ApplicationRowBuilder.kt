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

			// 제목을 안 적었으면 "OO월 OO일 적용"으로 대신 보여준다.
			val displayTitle =
				app.title.ifBlank { "${DateUtils.formatDateShort(app.applicationDate)} 적용" }

			// 추가한 본문이 있으면 그걸, 없으면 연결한 설교 제목들을 보여준다(둘 다 없으면 빈 채로 둔다).
			val bottomLabel = if (refs.isNotEmpty()) {
				refs.joinToString(", ") { it.toDisplayLabel() }
			} else {
				val links = db.applicationSermonLinkDao().getByApplication(app.id)
				val sermonTitles = links.mapNotNull { db.sermonDao().getById(it.sermonId)?.title }
				sermonTitles.joinToString(", ")
			}

			ApplicationRowData(
				application = app,
				displayTitle = displayTitle,
				colorHex = category?.colorHex,
				rightTopLabel = rightTopLabel,
				rightBottomLabel = bottomLabel
			)
		}
	}
}