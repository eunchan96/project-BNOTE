package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chan.bnote.data.bible.BibleDao
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bookmark.BibleBookmark
import com.chan.bnote.data.bookmark.BookmarkDao
import com.chan.bnote.data.memo.VerseMemo
import com.chan.bnote.data.memo.VerseMemoDao
import com.chan.bnote.data.memo.WordMemo
import com.chan.bnote.data.memo.WordMemoDao
import com.chan.bnote.data.mypage.ReadingProgress
import com.chan.bnote.data.mypage.ReadingProgressDao
import com.chan.bnote.data.mypage.VerseOfYear
import com.chan.bnote.data.mypage.VerseOfYearDao
import com.chan.bnote.data.mypage.VerseOfYearRef
import com.chan.bnote.data.mypage.VerseOfYearRefDao
import com.chan.bnote.data.partialhighlight.PartialHighlight
import com.chan.bnote.data.partialhighlight.PartialHighlightDao
import com.chan.bnote.data.scrap.Scrap
import com.chan.bnote.data.scrap.ScrapDao
import com.chan.bnote.data.scrap.ScrapGroup
import com.chan.bnote.data.sermon.DefaultSermonCategories
import com.chan.bnote.data.sermon.Preacher
import com.chan.bnote.data.sermon.PreacherDao
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.data.sermon.SermonBibleRefDao
import com.chan.bnote.data.sermon.SermonCategory
import com.chan.bnote.data.sermon.SermonCategoryDao
import com.chan.bnote.data.sermon.SermonDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
	entities = [
		BibleVerse::class, BibleBookmark::class, ReadingProgress::class,
		Sermon::class, SermonCategory::class, SermonBibleRef::class,
		VerseOfYear::class, VerseOfYearRef::class, ScrapGroup::class, Scrap::class,
		PartialHighlight::class, VerseMemo::class, WordMemo::class,
		Preacher::class // 추가
	],
	version = 12, // 11 -> 12
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
	abstract fun verseOfYearRefDao(): VerseOfYearRefDao
	abstract fun scrapDao(): ScrapDao
	abstract fun partialHighlightDao(): PartialHighlightDao
	abstract fun verseMemoDao(): VerseMemoDao
	abstract fun wordMemoDao(): WordMemoDao
	abstract fun preacherDao(): PreacherDao

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

				CoroutineScope(Dispatchers.IO).launch {
					if (instance.sermonCategoryDao().count() == 0) {
						instance.sermonCategoryDao().insertAll(DefaultSermonCategories.list)
					}
					if (instance.scrapDao().countGroups() == 0) {
						instance.scrapDao().insertGroup(ScrapGroup(name = "기본", sortOrder = 0))
					}
				}
				instance
			}
		}
	}
}