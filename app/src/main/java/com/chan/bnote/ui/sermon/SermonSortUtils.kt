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

	/**
	 * 캘린더/성경별 화면 상단 그리드의 색깔 막대들은(하루 또는 한 장에 설교가 여러 개일 때) 원래
	 * DB에 저장된 순서(추가한 순)로만 나오고 있었는데, 그러면 아래 목록에서 고른 정렬 방식(성경순
	 * · 카테고리순 등)과 안 맞았다. 마커(SermonMarker/ChapterMarker)에도 같은 정렬을 적용해서
	 * 맞춰준다. 마커엔 설교 날짜가 없어서 "DATE" 모드는 없는 것으로 보고 추가순으로 대체한다.
	 */
	suspend fun <T> sortMarkers(
		db: BibleDatabase,
		markers: List<T>,
		sortMode: String,
		markerId: (T) -> Long,
		markerCategoryId: (T) -> Long?
	): List<T> {
		return when (sortMode) {
			"CATEGORY" -> {
				val categoryOrder = loadCategoryOrderMap(db)
				markers.sortedBy { categoryOrder[markerCategoryId(it)] ?: Int.MAX_VALUE }
			}

			"BIBLE" -> {
				val refs = markers.associate {
					markerId(it) to db.sermonBibleRefDao().getFirstRef(markerId(it))
				}
				markers.sortedWith(
					compareBy(
						{ refs[markerId(it)]?.startBookId ?: Int.MAX_VALUE },
						{ refs[markerId(it)]?.startChapter ?: Int.MAX_VALUE },
						{ refs[markerId(it)]?.startVerse ?: Int.MAX_VALUE }
					)
				)
			}

			else -> markers.sortedBy { markerId(it) }
		}
	}
}