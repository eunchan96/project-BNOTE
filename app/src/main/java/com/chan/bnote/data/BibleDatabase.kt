package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
	entities = [BibleVerse::class, BibleBookmark::class, ReadingProgress::class],
	version = 4, // 3 -> 4
	exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {

	abstract fun bibleDao(): BibleDao
	abstract fun bookmarkDao(): BookmarkDao
	abstract fun readingProgressDao(): ReadingProgressDao

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