package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
	entities = [BibleVerse::class, BibleBookmark::class], // BibleBookmark 추가
	version = 2, // 1 -> 2
	exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {

	abstract fun bibleDao(): BibleDao
	abstract fun bookmarkDao(): BookmarkDao // 추가

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