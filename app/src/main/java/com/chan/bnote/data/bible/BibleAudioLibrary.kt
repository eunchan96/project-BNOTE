package com.chan.bnote.data.bible

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 개인용 숨김 기능: 기기에 직접 넣어둔 성경 음성 파일 폴더.
 *
 * 음성 파일은 앱(APK)에 들어있지 않고, 사용자가 자기 폰에 복사해둔 폴더를 앱 정보 화면의 숨김 메뉴
 * (버전 글자 7번 탭)에서 골라야만 쓸 수 있다. 폴더를 안 고른 기기에서는 성경 탭에 재생 버튼 자체가
 * 나타나지 않는다.
 *
 * 폴더 안에는 "책번호_장번호.확장자" 형식의 파일을 하위 폴더 없이 바로 넣어둔다.
 * 예) 01_001.mp3(창세기 1장), 19_023.ogg(시편 23편), 66_022.mp3(요한계시록 22장)
 * 책 번호는 BibleBooks와 같은 1~66 체계다.
 *
 * 설정 값은 AppSettings가 아니라 별도 SharedPreferences에 둔다 — 데이터 내보내기(백업)에 섞이지
 * 않게 하려는 것(다른 기기에서 백업을 불러와도 이 기능이 따라 켜지지 않는다).
 */
object BibleAudioLibrary {

	private const val PREF_NAME = "bible_audio"
	private const val KEY_FOLDER_URI = "folder_uri"

	private val FILE_NAME_PATTERN =
		Regex("""^(\d{1,2})_(\d{1,3})\.(mp3|ogg|opus|m4a)$""", RegexOption.IGNORE_CASE)

	/** (bookId, chapter) -> 파일 Uri. 폴더 안 파일 목록을 매번 다시 읽으면 느려서 한 번만 만들어둔다. */
	@Volatile
	private var index: Map<Pair<Int, Int>, Uri>? = null

	fun getFolderUri(context: Context): Uri? {
		val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
			.getString(KEY_FOLDER_URI, null) ?: return null
		return Uri.parse(raw)
	}

	/** 폴더가 골라져 있고, 그 폴더를 읽을 권한도 아직 살아있는지. 성경 탭의 재생 버튼 표시 기준이다. */
	fun isConfigured(context: Context): Boolean {
		val folderUri = getFolderUri(context) ?: return false
		return context.contentResolver.persistedUriPermissions.any {
			it.uri == folderUri && it.isReadPermission
		}
	}

	/** 폴더 선택 창(OpenDocumentTree)에서 돌아온 결과를 저장하고, 재부팅 후에도 읽을 수 있게 권한을 유지한다. */
	fun saveFolderUri(context: Context, treeUri: Uri) {
		context.contentResolver.takePersistableUriPermission(
			treeUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION
		)
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
			.putString(KEY_FOLDER_URI, treeUri.toString())
			.apply()
		index = null
	}

	fun clearFolder(context: Context) {
		val folderUri = getFolderUri(context)
		if (folderUri != null) {
			try {
				context.contentResolver.releasePersistableUriPermission(
					folderUri,
					Intent.FLAG_GRANT_READ_URI_PERMISSION
				)
			} catch (e: Exception) {
				// 이미 권한이 없어진 경우 — 무시하고 설정만 지운다.
			}
		}
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
			.remove(KEY_FOLDER_URI)
			.apply()
		index = null
	}

	/** 폴더에서 찾은 장 음성 파일 개수. 폴더를 고른 직후 제대로 들어있는지 확인시켜주는 용도. */
	suspend fun countChapters(context: Context): Int = loadIndex(context).size

	/** 해당 장의 음성 파일. 없으면 null. */
	suspend fun findChapter(context: Context, bookId: Int, chapter: Int): Uri? =
		loadIndex(context)[bookId to chapter]

	private suspend fun loadIndex(context: Context): Map<Pair<Int, Int>, Uri> {
		index?.let { return it }
		val folderUri = getFolderUri(context) ?: return emptyMap()
		val built = withContext(Dispatchers.IO) { buildIndex(context, folderUri) }
		index = built
		return built
	}

	/** 폴더 바로 아래 파일 목록을 한 번의 쿼리로 읽어서 이름 규칙에 맞는 것만 모은다. */
	private fun buildIndex(context: Context, treeUri: Uri): Map<Pair<Int, Int>, Uri> {
		val result = HashMap<Pair<Int, Int>, Uri>()
		try {
			val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
				treeUri,
				DocumentsContract.getTreeDocumentId(treeUri)
			)
			val projection = arrayOf(
				DocumentsContract.Document.COLUMN_DOCUMENT_ID,
				DocumentsContract.Document.COLUMN_DISPLAY_NAME
			)
			context.contentResolver.query(childrenUri, projection, null, null, null)
				?.use { cursor ->
					while (cursor.moveToNext()) {
						val documentId = cursor.getString(0) ?: continue
						val name = cursor.getString(1) ?: continue
						val match = FILE_NAME_PATTERN.matchEntire(name) ?: continue
						val bookId = match.groupValues[1].toInt()
						val chapter = match.groupValues[2].toInt()
						if (bookId !in 1..66 || chapter < 1) continue
						result[bookId to chapter] =
							DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
					}
				}
		} catch (e: Exception) {
			// 폴더가 지워졌거나 권한이 회수된 경우 — 빈 목록으로 취급한다.
		}
		return result
	}
}