package com.chan.bnote.data.mypage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReadingProgressDao {

	@Query("SELECT * FROM reading_progress WHERE bookId = :bookId AND chapter = :chapter LIMIT 1")
	suspend fun get(bookId: Int, chapter: Int): ReadingProgress?

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsert(progress: ReadingProgress)

	@Query("DELETE FROM reading_progress WHERE bookId = :bookId AND chapter = :chapter")
	suspend fun delete(bookId: Int, chapter: Int)

	// 설정 화면(D단계)에서 "읽음 기록 초기화" 버튼에 연결할 함수
	@Query("DELETE FROM reading_progress")
	suspend fun resetAll()

	@Query("SELECT * FROM reading_progress")
	suspend fun getAll(): List<ReadingProgress>
}