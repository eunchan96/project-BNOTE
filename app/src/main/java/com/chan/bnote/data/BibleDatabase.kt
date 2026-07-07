package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BibleVerse::class], version = 1, exportSchema = false)
abstract class BibleDatabase : RoomDatabase() {

	abstract fun bibleDao(): BibleDao

	companion object {
		@Volatile
		private var INSTANCE: BibleDatabase? = null

		fun getInstance(context: Context): BibleDatabase {
			return INSTANCE ?: synchronized(this) {
				val instance = Room.databaseBuilder(
					context.applicationContext,
					BibleDatabase::class.java,
					"bnote.db"
				).build()
				INSTANCE = instance
				instance
			}
		}
	}
}