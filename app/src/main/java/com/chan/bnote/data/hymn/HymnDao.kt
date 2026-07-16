package com.chan.bnote.data.hymn

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface HymnDao {

	@Insert
	suspend fun insertCategories(categories: List<HymnCategory>)

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