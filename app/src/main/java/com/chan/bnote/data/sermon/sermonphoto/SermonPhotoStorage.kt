package com.chan.bnote.data.sermon.sermonphoto

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * 설교 사진은 외부 content:// URI 권한이 재부팅/시간 경과로 사라질 수 있어서,
 * 앱 내부 저장소(files/sermon_photos/)로 복사해서 절대경로로 들고 있는다.
 */
object SermonPhotoStorage {

	private const val DIR_NAME = "sermon_photos"

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

	/** 카메라로 찍을 사진을 저장할 새 파일 + FileProvider Uri를 만든다. */
	fun createCaptureTarget(context: Context): Pair<File, Uri> {
		val file = File(photoDir(context), "${UUID.randomUUID()}.jpg")
		val uri = FileProvider.getUriForFile(
			context, "${context.packageName}.fileprovider", file
		)
		return file to uri
	}

	fun deleteFile(path: String) {
		try {
			File(path).delete()
		} catch (e: Exception) {
			// 무시 - 파일이 이미 없거나 지울 수 없어도 앱 동작엔 지장 없음
		}
	}
}