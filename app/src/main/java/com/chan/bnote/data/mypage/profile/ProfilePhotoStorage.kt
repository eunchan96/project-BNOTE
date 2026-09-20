package com.chan.bnote.data.mypage.profile

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * 프로필 사진은 외부 content:// URI 권한이 재부팅/시간 경과로 사라질 수 있어서,
 * 앱 내부 저장소(files/profile_photo/)로 복사해서 절대경로로 들고 있는다.
 * (SermonPhotoStorage와 동일한 패턴)
 */
object ProfilePhotoStorage {

	private const val DIR_NAME = "profile_photo"

	private fun photoDir(context: Context): File {
		val dir = File(context.filesDir, DIR_NAME)
		if (!dir.exists()) dir.mkdirs()
		return dir
	}

	/** 갤러리 등에서 고른 content:// Uri를 앱 내부 저장소로 복사하고 절대경로를 돌려준다. */
	fun copyToInternalStorage(context: Context, sourceUri: Uri): String? {
		return try {
			val destFile = File(photoDir(context), "${UUID.randomUUID()}.jpg")
			context.contentResolver.openInputStream(sourceUri)?.use { input ->
				destFile.outputStream().use { output -> input.copyTo(output) }
			}
			destFile.absolutePath
		} catch (e: Exception) {
			null
		}
	}

	fun deleteFile(path: String) {
		try {
			File(path).delete()
		} catch (e: Exception) {
			// 무시 - 파일이 이미 없거나 지울 수 없어도 앱 동작엔 지장 없음
		}
	}
}