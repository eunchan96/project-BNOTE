package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
	entities = [
		BibleVerse::class, BibleBookmark::class, ReadingProgress::class,
		Sermon::class, SermonCategory::class, SermonBibleRef::class,
		VerseOfYear::class // 추가
	],
	version = 7, // 6 -> 7
	exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
	abstract fun bibleDao(): BibleDao
	abstract fun bookmarkDao(): BookmarkDao
	abstract fun readingProgressDao(): ReadingProgressDao
	abstract fun sermonDao(): SermonDao
	abstract fun sermonCategoryDao(): SermonCategoryDao
	abstract fun sermonBibleRefDao(): SermonBibleRefDao
	abstract fun verseOfYearDao(): VerseOfYearDao

	companion object {
		@Volatile
		private var INSTANCE: BibleDatabase? = null

		fun getInstance(context: Context): BibleDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					BibleDatabase::class.java,
					"bnote.db"
				)
					.fallbackToDestructiveMigration()
					.build()
				INSTANCE = instance

				// 최초 생성 시 기본 카테고리 자동 시딩
				CoroutineScope(Dispatchers.IO).launch {
					if (instance.sermonCategoryDao().count() == 0) {
						instance.sermonCategoryDao().insertAll(DefaultSermonCategories.list)
					}
				}
				instance
			}
		}
	}
}