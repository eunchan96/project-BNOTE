package com.chan.bnote.ui.sermon

import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef

/** 설교 목록 화면들(캘린더/성경별/설교자별)에서 공통으로 쓰는 정렬 도우미. */
object SermonSortUtils {

	/** categoryId -> sortOrder. 미분류(null)는 제일 뒤로 가도록 아주 큰 값을 준다. */
	suspend fun loadCategoryOrderMap(db: BibleDatabase): Map<Long?, Int> {
		val categories = db.sermonCategoryDao().getAll()
		val map = categories.associate { it.id as Long? to it.sortOrder }
		return map + (null to Int.MAX_VALUE)
	}

	/** 설교 하나당 본문 첫 구절(시작 책/장/절)을 가져온다. 본문이 없으면 맨 뒤로 가도록 최댓값을 준다. */
	suspend fun loadFirstRefs(
		db: BibleDatabase,
		sermons: List<Sermon>
	): Map<Long, SermonBibleRef?> {
		return sermons.associate { it.id to db.sermonBibleRefDao().getFirstRef(it.id) }
	}

	fun byBibleOrder(firstRefs: Map<Long, SermonBibleRef?>): Comparator<Sermon> = compareBy(
		{ firstRefs[it.id]?.startBookId ?: Int.MAX_VALUE },
		{ firstRefs[it.id]?.startChapter ?: Int.MAX_VALUE },
		{ firstRefs[it.id]?.startVerse ?: Int.MAX_VALUE }
	)

	fun byCategoryOrder(categoryOrder: Map<Long?, Int>): Comparator<Sermon> =
		compareBy { categoryOrder[it.categoryId] ?: Int.MAX_VALUE }

	fun byDateDesc(): Comparator<Sermon> = compareByDescending { it.sermonDate }

	/** 추가순 = id순(먼저 추가한 게 먼저). */
	fun byAddedOrder(): Comparator<Sermon> = compareBy { it.id }
}