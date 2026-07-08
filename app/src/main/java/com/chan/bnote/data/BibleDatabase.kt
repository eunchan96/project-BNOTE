package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
	entities = [BibleVerse::class, BibleBookmark::class, ReadingProgress::class, Sermon::class],
	version = 5, // 4 -> 5
	exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {

	abstract fun bibleDao(): BibleDao
	abstract fun bookmarkDao(): BookmarkDao
	abstract fun readingProgressDao(): ReadingProgressDao
	abstract fun sermonDao(): SermonDao

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
				instance
			}
		}
	}
}