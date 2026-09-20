package com.chan.bnote.data.bible.hymn

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HymnDao {

	@Insert
	suspend fun insertCategory(category: HymnCategory): Long

	@Insert
	suspend fun insertHymns(hymns: List<Hymn>)

	@Query("SELECT COUNT(*) FROM hymns")
	suspend fun count(): Int

	@Query("SELECT * FROM hymn_categories WHERE parentId IS NULL ORDER BY sortOrder ASC")
	suspend fun getMajorCategories(): List<HymnCategory>

	@Query("SELECT * FROM hymn_categories WHERE parentId = :majorId ORDER BY sortOrder ASC")
	suspend fun getMinorCategories(majorId: Long): List<HymnCategory>

	@Query("SELECT * FROM hymn_categories WHERE id = :id LIMIT 1")
	suspend fun getCategoryById(id: Long): HymnCategory?

	@Query("SELECT * FROM hymns ORDER BY number ASC")
	suspend fun getAll(): List<Hymn>

	@Query("SELECT * FROM hymns WHERE categoryId = :minorCategoryId ORDER BY number ASC")
	suspend fun getByCategory(minorCategoryId: Long): List<Hymn>

	@Query("SELECT * FROM hymns WHERE number = :number LIMIT 1")
	suspend fun getByNumber(number: Int): Hymn?

	/** 소분류 하나에 속한 찬송들의 최소~최대 장번호. */
	@Query("SELECT MIN(number) as minNumber, MAX(number) as maxNumber FROM hymns WHERE categoryId = :minorCategoryId")
	suspend fun getRangeForMinorCategory(minorCategoryId: Long): HymnNumberRange?

	/** 대분류 하나에 속한 모든 소분류의 찬송을 합친 최소~최대 장번호. */
	@Query(
		"""
        SELECT MIN(number) as minNumber, MAX(number) as maxNumber FROM hymns
        WHERE categoryId IN (SELECT id FROM hymn_categories WHERE parentId = :majorId)
        """
	)
	suspend fun getRangeForMajorCategory(majorId: Long): HymnNumberRange?

	@Query(
		"""
        SELECT * FROM hymns
        WHERE CAST(number AS TEXT) LIKE '%' || :keyword || '%'
           OR REPLACE(title, ' ', '') LIKE '%' || REPLACE(:keyword, ' ', '') || '%'
        ORDER BY number ASC
        """
	)
	suspend fun search(keyword: String): List<Hymn>
}

data class HymnNumberRange(
	val minNumber: Int,
	val maxNumber: Int
)