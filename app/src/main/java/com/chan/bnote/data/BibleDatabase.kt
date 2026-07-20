package com.chan.bnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chan.bnote.data.bible.BibleDao
import com.chan.bnote.data.bible.BibleVerse
import com.chan.bnote.data.bible.bookmark.BibleBookmark
import com.chan.bnote.data.bible.bookmark.BookmarkDao
import com.chan.bnote.data.bible.hymn.Hymn
import com.chan.bnote.data.bible.hymn.HymnCategory
import com.chan.bnote.data.bible.hymn.HymnDao
import com.chan.bnote.data.bible.hymn.HymnSeeder
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.VerseMemoDao
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.data.bible.memo.WordMemoDao
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import com.chan.bnote.data.bible.partialhighlight.PartialHighlightDao
import com.chan.bnote.data.bible.scrap.Scrap
import com.chan.bnote.data.bible.scrap.ScrapDao
import com.chan.bnote.data.bible.scrap.ScrapGroup
import com.chan.bnote.data.mypage.RecentChapterView
import com.chan.bnote.data.mypage.RecentChapterViewDao
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.mypage.memorization.MemorizationVerseDao
import com.chan.bnote.data.mypage.memorization.VerseMemorizationProgress
import com.chan.bnote.data.mypage.memorization.VerseMemorizationProgressDao
import com.chan.bnote.data.mypage.prayer.PrayerRequest
import com.chan.bnote.data.mypage.prayer.PrayerRequestDao
import com.chan.bnote.data.mypage.profile.UserProfile
import com.chan.bnote.data.mypage.profile.UserProfileDao
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import com.chan.bnote.data.mypage.readingplan.ReadingProgressDao
import com.chan.bnote.data.mypage.verseofyear.VerseOfYear
import com.chan.bnote.data.mypage.verseofyear.VerseOfYearDao
import com.chan.bnote.data.mypage.verseofyear.VerseOfYearRef
import com.chan.bnote.data.mypage.verseofyear.VerseOfYearRefDao
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.data.sermon.SermonBibleRefDao
import com.chan.bnote.data.sermon.SermonDao
import com.chan.bnote.data.sermon.preacher.Preacher
import com.chan.bnote.data.sermon.preacher.PreacherDao
import com.chan.bnote.data.sermon.sermoncategory.DefaultSermonCategories
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.chan.bnote.data.sermon.sermoncategory.SermonCategoryDao
import com.chan.bnote.data.sermon.sermonphoto.SermonPhoto
import com.chan.bnote.data.sermon.sermonphoto.SermonPhotoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
	entities = [
		BibleVerse::class, BibleBookmark::class, ReadingProgress::class,
		Sermon::class, SermonCategory::class, SermonBibleRef::class, SermonPhoto::class,
		VerseOfYear::class, VerseOfYearRef::class, ScrapGroup::class, Scrap::class,
		PartialHighlight::class, VerseMemo::class, WordMemo::class,
		Preacher::class, HymnCategory::class, Hymn::class, UserProfile::class,
		PrayerRequest::class, VerseMemorizationProgress::class, MemorizationVerse::class,
		MemorizationGroup::class, RecentChapterView::class
	],
	version = 24, // 23 -> 24 (verse_memos 유니크 제약 제거 - 한 구절에 메모 여러 개 허용)
	exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
	abstract fun bibleDao(): BibleDao
	abstract fun bookmarkDao(): BookmarkDao
	abstract fun readingProgressDao(): ReadingProgressDao
	abstract fun sermonDao(): SermonDao
	abstract fun sermonCategoryDao(): SermonCategoryDao
	abstract fun sermonBibleRefDao(): SermonBibleRefDao
	abstract fun sermonPhotoDao(): SermonPhotoDao
	abstract fun verseOfYearDao(): VerseOfYearDao
	abstract fun verseOfYearRefDao(): VerseOfYearRefDao
	abstract fun hymnDao(): HymnDao
	abstract fun scrapDao(): ScrapDao
	abstract fun partialHighlightDao(): PartialHighlightDao
	abstract fun verseMemoDao(): VerseMemoDao
	abstract fun wordMemoDao(): WordMemoDao
	abstract fun preacherDao(): PreacherDao
	abstract fun userProfileDao(): UserProfileDao
	abstract fun prayerRequestDao(): PrayerRequestDao
	abstract fun verseMemorizationProgressDao(): VerseMemorizationProgressDao
	abstract fun memorizationVerseDao(): MemorizationVerseDao
	abstract fun recentChapterViewDao(): RecentChapterViewDao

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
					.addMigrations(*MIGRATIONS)
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
					if (instance.memorizationVerseDao().countGroups() == 0) {
						instance.memorizationVerseDao()
							.insertGroup(MemorizationGroup(name = "기본", sortOrder = 0))
					}
					HymnSeeder.seedIfNeeded(context.applicationContext, instance.hymnDao())
				}
				instance
			}
		}
	}
}