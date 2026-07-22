package com.chan.bnote.data.backup

import android.content.Context
import android.net.Uri
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.bookmark.BibleBookmark
import com.chan.bnote.data.bible.memo.VerseMemo
import com.chan.bnote.data.bible.memo.WordMemo
import com.chan.bnote.data.bible.partialhighlight.PartialHighlight
import com.chan.bnote.data.bible.scrap.Scrap
import com.chan.bnote.data.bible.scrap.ScrapGroup
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.mypage.memorization.VerseMemorizationProgress
import com.chan.bnote.data.mypage.prayer.PrayerRequest
import com.chan.bnote.data.mypage.profile.UserProfile
import com.chan.bnote.data.mypage.readingplan.ReadingProgress
import com.chan.bnote.data.mypage.verseofyear.VerseOfYear
import com.chan.bnote.data.mypage.verseofyear.VerseOfYearRef
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.data.sermon.preacher.Preacher
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.chan.bnote.data.sermon.sermonphoto.SermonPhoto
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 사용자가 만든 데이터(성경 본문/찬송가 같은 내장 데이터 제외) 전체를 zip 하나로 내보내고 불러온다.
 * 사이드로딩 특성상 버전이 오를 때 DB 스키마가 바뀌어 데이터가 초기화될 수 있는데,
 * 그런 상황에서도 이 백업/복원으로 데이터를 지킬 수 있게 하는 게 목적이다.
 *
 * 불러오기는 "병합"이 아니라 "전체 교체"다 — 불러오기 전 기존 사용자 데이터를 모두 지우고
 * 백업 내용으로 다시 채운다 (기본 카테고리/그룹 중복 방지, id 꼬임 방지).
 */
object BackupManager {

	private const val JSON_ENTRY_NAME = "backup.json"
	private const val SERMON_PHOTOS_DIR = "sermon_photos"
	private const val PROFILE_PHOTO_DIR = "profile_photo"

	suspend fun export(context: Context, destination: Uri) {
		val db = BibleDatabase.getInstance(context.applicationContext)
		val json = buildBackupJson(db)

		context.contentResolver.openOutputStream(destination)?.use { out ->
			ZipOutputStream(out).use { zip ->
				zip.putNextEntry(ZipEntry(JSON_ENTRY_NAME))
				zip.write(json.toString().toByteArray(Charsets.UTF_8))
				zip.closeEntry()

				zipDirectory(zip, File(context.filesDir, SERMON_PHOTOS_DIR), SERMON_PHOTOS_DIR)
				zipDirectory(zip, File(context.filesDir, PROFILE_PHOTO_DIR), PROFILE_PHOTO_DIR)
			}
		} ?: throw IllegalStateException("파일을 열 수 없어요")
	}

	suspend fun import(context: Context, source: Uri) {
		val db = BibleDatabase.getInstance(context.applicationContext)
		var json: JSONObject? = null

		context.contentResolver.openInputStream(source)?.use { input ->
			ZipInputStream(input).use { zip ->
				var entry = zip.nextEntry
				while (entry != null) {
					val name = entry.name
					when {
						name == JSON_ENTRY_NAME -> {
							json = JSONObject(zip.readBytes().toString(Charsets.UTF_8))
						}

						name.startsWith("$SERMON_PHOTOS_DIR/") || name.startsWith("$PROFILE_PHOTO_DIR/") -> {
							val destFile = File(context.filesDir, name)
							destFile.parentFile?.mkdirs()
							destFile.outputStream().use { out -> zip.copyTo(out) }
						}
					}
					zip.closeEntry()
					entry = zip.nextEntry
				}
			}
		} ?: throw IllegalStateException("파일을 열 수 없어요")

		val data = json ?: throw IllegalStateException("올바른 백업 파일이 아니에요")
		restoreFromJson(context, db, data)
	}

	// --- 내보내기 ---

	private suspend fun buildBackupJson(db: BibleDatabase): JSONObject {
		val root = JSONObject()
		root.put("exportedAt", System.currentTimeMillis())

		root.put("bookmarks", JSONArray(db.bookmarkDao().getAll().map { it.toJson() }))
		root.put("highlights", JSONArray(db.partialHighlightDao().getAll().map { it.toJson() }))
		root.put("scrapGroups", JSONArray(db.scrapDao().getAllGroups().map { it.toJson() }))
		root.put("scraps", JSONArray(db.scrapDao().getAllScraps().map { it.toJson() }))
		root.put("verseMemos", JSONArray(db.verseMemoDao().getAll().map { it.toJson() }))
		root.put("wordMemos", JSONArray(db.wordMemoDao().getAll().map { it.toJson() }))
		root.put("preachers", JSONArray(db.preacherDao().getAll().map { it.toJson() }))
		root.put("sermonCategories", JSONArray(db.sermonCategoryDao().getAll().map { it.toJson() }))
		root.put("sermons", JSONArray(db.sermonDao().getAll().map { it.toJson() }))
		root.put("sermonBibleRefs", JSONArray(db.sermonBibleRefDao().getAll().map { it.toJson() }))
		root.put("sermonPhotos", JSONArray(db.sermonPhotoDao().getAll().map { it.toJson() }))
		root.put("prayerRequests", JSONArray(db.prayerRequestDao().getAll().map { it.toJson() }))
		root.put(
			"memorizationGroups",
			JSONArray(db.memorizationVerseDao().getAllGroups().map { it.toJson() })
		)
		root.put(
			"memorizationVerses",
			JSONArray(db.memorizationVerseDao().getAll().map { it.toJson() })
		)
		root.put(
			"memorizationProgress",
			JSONArray(db.verseMemorizationProgressDao().getAll().map { it.toJson() })
		)
		db.userProfileDao().get()?.let { root.put("userProfile", it.toJson()) }
		root.put("readingProgress", JSONArray(db.readingProgressDao().getAll().map { it.toJson() }))
		root.put("verseOfYears", JSONArray(db.verseOfYearDao().getAll().map { it.toJson() }))
		root.put(
			"verseOfYearRefs",
			JSONArray(db.verseOfYearRefDao().getAllRefs().map { it.toJson() })
		)

		return root
	}

	private fun zipDirectory(zip: ZipOutputStream, dir: File, entryPrefix: String) {
		if (!dir.exists()) return
		dir.listFiles()?.forEach { file ->
			if (file.isFile) {
				zip.putNextEntry(ZipEntry("$entryPrefix/${file.name}"))
				file.inputStream().use { it.copyTo(zip) }
				zip.closeEntry()
			}
		}
	}

	// --- 불러오기 ---

	private suspend fun restoreFromJson(context: Context, db: BibleDatabase, root: JSONObject) {
		// 1) 기존 사용자 데이터를 전부 지운다 (성경/찬송가 내장 데이터는 건드리지 않음).
		db.bookmarkDao().deleteAll()
		db.partialHighlightDao().deleteAll()
		db.scrapDao().deleteAllScraps()
		db.scrapDao().deleteAllGroups()
		db.verseMemoDao().deleteAll()
		db.wordMemoDao().deleteAll()
		db.sermonBibleRefDao().deleteAll()
		db.sermonPhotoDao().deleteAll()
		db.sermonDao().deleteAll()
		db.sermonCategoryDao().deleteAll()
		db.preacherDao().deleteAll()
		db.prayerRequestDao().deleteAll()
		db.verseMemorizationProgressDao().deleteAll()
		db.memorizationVerseDao().deleteAllVerses()
		db.memorizationVerseDao().deleteAllGroups()
		db.userProfileDao().deleteAll()
		db.readingProgressDao().resetAll()
		db.verseOfYearRefDao().deleteAll()
		db.verseOfYearDao().deleteAll()

		// 2) 백업 내용을 다시 채운다. id가 있는 참조 관계(그룹→항목, 설교자/카테고리→설교 등)는
		//    새로 생성되는 id로 다시 매핑해줘야 한다.

		for (i in 0 until root.optJSONArray("bookmarks")?.length().orZero()) {
			db.bookmarkDao()
				.upsert(bookmarkFromJson(root.getJSONArray("bookmarks").getJSONObject(i)))
		}

		for (i in 0 until root.optJSONArray("highlights")?.length().orZero()) {
			db.partialHighlightDao()
				.insert(highlightFromJson(root.getJSONArray("highlights").getJSONObject(i)))
		}

		val scrapGroupIdMap = mutableMapOf<Long, Long>()
		for (i in 0 until root.optJSONArray("scrapGroups")?.length().orZero()) {
			val obj = root.getJSONArray("scrapGroups").getJSONObject(i)
			val oldId = obj.getLong("id")
			val newId = db.scrapDao().insertGroup(
				ScrapGroup(
					name = obj.getString("name"),
					sortOrder = obj.optInt("sortOrder", 0)
				)
			)
			scrapGroupIdMap[oldId] = newId
		}
		for (i in 0 until root.optJSONArray("scraps")?.length().orZero()) {
			val obj = root.getJSONArray("scraps").getJSONObject(i)
			val newGroupId = scrapGroupIdMap[obj.getLong("groupId")] ?: continue
			db.scrapDao().insertScrap(scrapFromJson(obj, newGroupId))
		}

		for (i in 0 until root.optJSONArray("verseMemos")?.length().orZero()) {
			db.verseMemoDao()
				.upsert(verseMemoFromJson(root.getJSONArray("verseMemos").getJSONObject(i)))
		}
		for (i in 0 until root.optJSONArray("wordMemos")?.length().orZero()) {
			db.wordMemoDao()
				.insert(wordMemoFromJson(root.getJSONArray("wordMemos").getJSONObject(i)))
		}

		val preacherIdMap = mutableMapOf<Long, Long>()
		for (i in 0 until root.optJSONArray("preachers")?.length().orZero()) {
			val obj = root.getJSONArray("preachers").getJSONObject(i)
			val oldId = obj.getLong("id")
			val newId = db.preacherDao().insert(
				Preacher(
					name = obj.getString("name"),
					sortOrder = obj.optInt("sortOrder", 0)
				)
			)
			preacherIdMap[oldId] = newId
		}

		val categoryIdMap = mutableMapOf<Long, Long>()
		for (i in 0 until root.optJSONArray("sermonCategories")?.length().orZero()) {
			val obj = root.getJSONArray("sermonCategories").getJSONObject(i)
			val oldId = obj.getLong("id")
			val newId = db.sermonCategoryDao().insert(
				SermonCategory(
					name = obj.getString("name"),
					colorHex = obj.getString("colorHex"),
					isDefault = obj.optBoolean("isDefault", false),
					sortOrder = obj.optInt("sortOrder", 0)
				)
			)
			categoryIdMap[oldId] = newId
		}

		val sermonIdMap = mutableMapOf<Long, Long>()
		for (i in 0 until root.optJSONArray("sermons")?.length().orZero()) {
			val obj = root.getJSONArray("sermons").getJSONObject(i)
			val oldId = obj.getLong("id")
			val oldPreacherId = if (obj.has("preacherId")) obj.getLong("preacherId") else null
			val oldCategoryId = if (obj.has("categoryId")) obj.getLong("categoryId") else null
			val newId = db.sermonDao().insert(
				Sermon(
					title = obj.getString("title"),
					preacherId = oldPreacherId?.let { preacherIdMap[it] },
					sermonDate = obj.getLong("sermonDate"),
					categoryId = oldCategoryId?.let { categoryIdMap[it] },
					memo = obj.optString("memo", ""),
					link = if (obj.has("link")) obj.getString("link") else null,
					createdAt = obj.optLong("createdAt", System.currentTimeMillis())
				)
			)
			sermonIdMap[oldId] = newId
		}

		val refsToInsert = mutableListOf<SermonBibleRef>()
		for (i in 0 until root.optJSONArray("sermonBibleRefs")?.length().orZero()) {
			val obj = root.getJSONArray("sermonBibleRefs").getJSONObject(i)
			val newSermonId = sermonIdMap[obj.getLong("sermonId")] ?: continue
			refsToInsert.add(sermonBibleRefFromJson(obj, newSermonId))
		}
		if (refsToInsert.isNotEmpty()) db.sermonBibleRefDao().insertAll(refsToInsert)

		val photosToInsert = mutableListOf<SermonPhoto>()
		for (i in 0 until root.optJSONArray("sermonPhotos")?.length().orZero()) {
			val obj = root.getJSONArray("sermonPhotos").getJSONObject(i)
			val newSermonId = sermonIdMap[obj.getLong("sermonId")] ?: continue
			val relativePath = obj.getString("filePath")
			val absolutePath = File(context.filesDir, relativePath).absolutePath
			photosToInsert.add(
				SermonPhoto(
					sermonId = newSermonId,
					filePath = absolutePath,
					sortOrder = obj.optInt("sortOrder", 0)
				)
			)
		}
		if (photosToInsert.isNotEmpty()) db.sermonPhotoDao().insertAll(photosToInsert)

		for (i in 0 until root.optJSONArray("prayerRequests")?.length().orZero()) {
			db.prayerRequestDao()
				.insert(prayerRequestFromJson(root.getJSONArray("prayerRequests").getJSONObject(i)))
		}

		val memoGroupIdMap = mutableMapOf<Long, Long>()
		for (i in 0 until root.optJSONArray("memorizationGroups")?.length().orZero()) {
			val obj = root.getJSONArray("memorizationGroups").getJSONObject(i)
			val oldId = obj.getLong("id")
			val newId = db.memorizationVerseDao()
				.insertGroup(
					MemorizationGroup(
						name = obj.getString("name"),
						sortOrder = obj.optInt("sortOrder", 0)
					)
				)
			memoGroupIdMap[oldId] = newId
		}

		val memoVerseIdMap = mutableMapOf<Long, Long>()
		for (i in 0 until root.optJSONArray("memorizationVerses")?.length().orZero()) {
			val obj = root.getJSONArray("memorizationVerses").getJSONObject(i)
			val oldId = obj.getLong("id")
			val newGroupId = memoGroupIdMap[obj.getLong("groupId")] ?: continue
			val newId = db.memorizationVerseDao().insert(memorizationVerseFromJson(obj, newGroupId))
			memoVerseIdMap[oldId] = newId
		}

		for (i in 0 until root.optJSONArray("memorizationProgress")?.length().orZero()) {
			val obj = root.getJSONArray("memorizationProgress").getJSONObject(i)
			val newVerseRefId = memoVerseIdMap[obj.getLong("verseRefId")] ?: continue
			db.verseMemorizationProgressDao().upsert(
				VerseMemorizationProgress(
					verseRefId = newVerseRefId,
					reviewCount = obj.optInt("reviewCount", 0),
					lastReviewedAt = if (obj.has("lastReviewedAt")) obj.getLong("lastReviewedAt") else null,
					isMastered = obj.optBoolean("isMastered", false)
				)
			)
		}

		root.optJSONObject("userProfile")?.let { obj ->
			val relativePhotoPath = if (obj.has("photoPath")) obj.getString("photoPath") else null
			val absolutePhotoPath =
				relativePhotoPath?.let { File(context.filesDir, it).absolutePath }
			db.userProfileDao().upsert(
				UserProfile(
					id = 1,
					photoPath = absolutePhotoPath,
					name = obj.optString("name", ""),
					church = obj.optString("church", ""),
					department = obj.optString("department", ""),
					position = obj.optString("position", "")
				)
			)
		}

		for (i in 0 until root.optJSONArray("readingProgress")?.length().orZero()) {
			db.readingProgressDao().upsert(
				readingProgressFromJson(
					root.getJSONArray("readingProgress").getJSONObject(i)
				)
			)
		}

		for (i in 0 until root.optJSONArray("verseOfYears")?.length().orZero()) {
			val obj = root.getJSONArray("verseOfYears").getJSONObject(i)
			db.verseOfYearDao()
				.upsert(VerseOfYear(year = obj.getInt("year"), note = obj.optString("note", "")))
		}
		for (i in 0 until root.optJSONArray("verseOfYearRefs")?.length().orZero()) {
			db.verseOfYearRefDao().insertAll(
				listOf(
					verseOfYearRefFromJson(
						root.getJSONArray("verseOfYearRefs").getJSONObject(i)
					)
				)
			)
		}
	}

	private fun Int?.orZero() = this ?: 0

	// --- 엔티티 <-> JSON 변환 ---

	private fun BibleBookmark.toJson() = JSONObject().apply {
		put("bookId", bookId); put("chapter", chapter); put("verse", verse)
		put("isBookmarked", isBookmarked); put("isHighlighted", isHighlighted); put(
		"updatedAt",
		updatedAt
	)
	}

	private fun bookmarkFromJson(o: JSONObject) = BibleBookmark(
		bookId = o.getInt("bookId"), chapter = o.getInt("chapter"), verse = o.getInt("verse"),
		isBookmarked = o.optBoolean("isBookmarked", false),
		isHighlighted = o.optBoolean("isHighlighted", false),
		updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
	)

	private fun PartialHighlight.toJson() = JSONObject().apply {
		put("translation", translation); put("bookId", bookId); put(
		"chapter",
		chapter
	); put("verse", verse)
		put("startOffset", startOffset); put("endOffset", endOffset); put("colorHex", colorHex)
	}

	private fun highlightFromJson(o: JSONObject) = PartialHighlight(
		translation = o.getString("translation"),
		bookId = o.getInt("bookId"),
		chapter = o.getInt("chapter"),
		verse = o.getInt("verse"),
		startOffset = o.getInt("startOffset"),
		endOffset = o.getInt("endOffset"),
		colorHex = o.optString("colorHex", "#FFF9C4")
	)

	private fun ScrapGroup.toJson() = JSONObject().apply {
		put("id", id); put("name", name); put("sortOrder", sortOrder)
	}

	private fun Scrap.toJson() = JSONObject().apply {
		put("groupId", groupId); put("bookId", bookId); put("chapter", chapter)
		put("startVerse", startVerse); put("endVerse", endVerse); put(
		"verseText",
		verseText
	); put("createdAt", createdAt)
	}

	private fun scrapFromJson(o: JSONObject, newGroupId: Long) = Scrap(
		groupId = newGroupId,
		bookId = o.getInt("bookId"),
		chapter = o.getInt("chapter"),
		startVerse = o.getInt("startVerse"),
		endVerse = o.getInt("endVerse"),
		verseText = o.getString("verseText"),
		createdAt = o.optLong("createdAt", System.currentTimeMillis())
	)

	private fun VerseMemo.toJson() = JSONObject().apply {
		put("bookId", bookId); put("chapter", chapter); put("verse", verse); put("text", text); put(
		"updatedAt",
		updatedAt
	)
	}

	private fun verseMemoFromJson(o: JSONObject) = VerseMemo(
		bookId = o.getInt("bookId"), chapter = o.getInt("chapter"), verse = o.getInt("verse"),
		text = o.getString("text"), updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
	)

	private fun WordMemo.toJson() = JSONObject().apply {
		put("translation", translation); put("bookId", bookId); put(
		"chapter",
		chapter
	); put("verse", verse)
		put("startOffset", startOffset); put("endOffset", endOffset); put("text", text)
		sourceLabel?.let { put("sourceLabel", it) }
		put("updatedAt", updatedAt)
	}

	private fun wordMemoFromJson(o: JSONObject) = WordMemo(
		translation = o.getString("translation"),
		bookId = o.getInt("bookId"),
		chapter = o.getInt("chapter"),
		verse = o.getInt("verse"),
		startOffset = o.getInt("startOffset"),
		endOffset = o.getInt("endOffset"),
		text = o.getString("text"),
		sourceLabel = if (o.has("sourceLabel")) o.getString("sourceLabel") else null,
		updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
	)

	private fun Preacher.toJson() = JSONObject().apply {
		put("id", id); put("name", name); put("sortOrder", sortOrder)
	}

	private fun SermonCategory.toJson() = JSONObject().apply {
		put("id", id); put("name", name); put("colorHex", colorHex); put(
		"isDefault",
		isDefault
	); put("sortOrder", sortOrder)
	}

	private fun Sermon.toJson() = JSONObject().apply {
		put("id", id); put("title", title)
		preacherId?.let { put("preacherId", it) }
		put("sermonDate", sermonDate)
		categoryId?.let { put("categoryId", it) }
		put("memo", memo)
		link?.let { put("link", it) }
		put("createdAt", createdAt)
	}

	private fun SermonBibleRef.toJson() = JSONObject().apply {
		put("sermonId", sermonId); put("startBookId", startBookId); put(
		"startChapter",
		startChapter
	)
		put("startVerse", startVerse); put("endBookId", endBookId); put(
		"endChapter",
		endChapter
	); put("endVerse", endVerse)
	}

	private fun sermonBibleRefFromJson(o: JSONObject, newSermonId: Long) = SermonBibleRef(
		sermonId = newSermonId,
		startBookId = o.getInt("startBookId"),
		startChapter = o.getInt("startChapter"),
		startVerse = o.getInt("startVerse"),
		endBookId = o.getInt("endBookId"),
		endChapter = o.getInt("endChapter"),
		endVerse = o.getInt("endVerse")
	)

	private fun SermonPhoto.toJson() = JSONObject().apply {
		put("sermonId", sermonId)
		// 절대경로 대신 zip 안 상대경로("sermon_photos/파일명")로 저장한다.
		put("filePath", "$SERMON_PHOTOS_DIR/${File(filePath).name}")
		put("sortOrder", sortOrder)
	}

	private fun PrayerRequest.toJson() = JSONObject().apply {
		put("content", content); put("createdAt", createdAt); put("isAnswered", isAnswered)
		answeredAt?.let { put("answeredAt", it) }
	}

	private fun prayerRequestFromJson(o: JSONObject) = PrayerRequest(
		content = o.getString("content"),
		createdAt = o.optLong("createdAt", System.currentTimeMillis()),
		isAnswered = o.optBoolean("isAnswered", false),
		answeredAt = if (o.has("answeredAt")) o.getLong("answeredAt") else null
	)

	private fun MemorizationGroup.toJson() = JSONObject().apply {
		put("id", id); put("name", name); put("sortOrder", sortOrder)
	}

	private fun MemorizationVerse.toJson() = JSONObject().apply {
		put("id", id); put("groupId", groupId)
		put("startBookId", startBookId); put("startChapter", startChapter); put(
		"startVerse",
		startVerse
	)
		put("endBookId", endBookId); put("endChapter", endChapter); put("endVerse", endVerse)
		put("verseText", verseText); put("note", note); put("createdAt", createdAt)
	}

	private fun memorizationVerseFromJson(o: JSONObject, newGroupId: Long) = MemorizationVerse(
		groupId = newGroupId,
		startBookId = o.getInt("startBookId"),
		startChapter = o.getInt("startChapter"),
		startVerse = o.getInt("startVerse"),
		endBookId = o.getInt("endBookId"),
		endChapter = o.getInt("endChapter"),
		endVerse = o.getInt("endVerse"),
		verseText = o.getString("verseText"),
		note = o.optString("note", ""),
		createdAt = o.optLong("createdAt", System.currentTimeMillis())
	)

	private fun VerseMemorizationProgress.toJson() = JSONObject().apply {
		put("verseRefId", verseRefId); put("reviewCount", reviewCount)
		lastReviewedAt?.let { put("lastReviewedAt", it) }
		put("isMastered", isMastered)
	}

	private fun UserProfile.toJson() = JSONObject().apply {
		photoPath?.let { put("photoPath", "$PROFILE_PHOTO_DIR/${File(it).name}") }
		put("name", name); put("church", church); put("department", department); put(
		"position",
		position
	)
	}

	private fun ReadingProgress.toJson() = JSONObject().apply {
		put("bookId", bookId); put("chapter", chapter); put("readAt", readAt)
	}

	private fun readingProgressFromJson(o: JSONObject) = ReadingProgress(
		bookId = o.getInt("bookId"),
		chapter = o.getInt("chapter"),
		readAt = o.optLong("readAt", System.currentTimeMillis())
	)

	private fun VerseOfYear.toJson() = JSONObject().apply {
		put("year", year); put("note", note)
	}

	private fun VerseOfYearRef.toJson() = JSONObject().apply {
		put("year", year); put("startBookId", startBookId); put(
		"startChapter",
		startChapter
	); put("startVerse", startVerse)
		put("endBookId", endBookId); put("endChapter", endChapter); put(
		"endVerse",
		endVerse
	); put("verseText", verseText)
	}

	private fun verseOfYearRefFromJson(o: JSONObject) = VerseOfYearRef(
		year = o.getInt("year"),
		startBookId = o.getInt("startBookId"),
		startChapter = o.getInt("startChapter"),
		startVerse = o.getInt("startVerse"),
		endBookId = o.getInt("endBookId"),
		endChapter = o.getInt("endChapter"),
		endVerse = o.getInt("endVerse"),
		verseText = o.getString("verseText")
	)
}