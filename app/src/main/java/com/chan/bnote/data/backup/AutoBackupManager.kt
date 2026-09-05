package com.chan.bnote.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.chan.bnote.data.AppSettings

/**
 * "자동 데이터 내보내기" 기능의 판단(주기가 지났는지)과 저장 위치 관리를 맡는다.
 * 실제 압축/저장(export)은 BackupManager를 그대로 재사용한다 — Uri 하나만 만들어서 넘겨주면 된다.
 *
 * 저장 위치는 설정 화면에서 폴더 하나를 미리 골라두는 방식(SAF 폴더 선택, OpenDocumentTree)을
 * 쓴다. 한 번 고르고 나면 그 폴더에 대한 접근 권한을 계속 유지할 수 있어서(persistable
 * permission), 그 다음부터는 자동 백업 때마다 위치를 다시 고를 필요가 없다.
 */
object AutoBackupManager {

	/** 설정에서 켜져 있고, 마지막 확인 이후 고른 주기만큼 지났으면 true. */
	fun shouldPromptNow(context: Context): Boolean {
		if (!AppSettings.isAutoBackupEnabled(context)) return false
		val intervalMillis = AppSettings.getAutoBackupIntervalDays(context) * 24L * 60 * 60 * 1000
		val lastCheck = AppSettings.getAutoBackupLastCheck(context)
		return System.currentTimeMillis() - lastCheck >= intervalMillis
	}

	/** 알림을 띄운 뒤(예/아니요 무엇을 골랐든) 반드시 불러서 기준 시각을 지금으로 다시 맞춘다.
	 * 안 그러면 "아니요"를 고른 다음 날 앱을 열 때마다 매번 또 뜨게 된다. */
	fun markChecked(context: Context) {
		AppSettings.setAutoBackupLastCheck(context, System.currentTimeMillis())
	}

	/** 미리 골라둔 저장 폴더의 Uri. 아직 안 골랐으면 null. */
	fun getFolderUri(context: Context): Uri? {
		val raw = AppSettings.getAutoBackupFolderUri(context) ?: return null
		return Uri.parse(raw)
	}

	/** 폴더 선택 창(OpenDocumentTree)에서 돌아온 결과를 저장하고, 앱을 재시작하거나 기기를
	 * 재부팅해도 그 폴더에 계속 쓸 수 있도록 권한을 지속시킨다. */
	fun saveFolderUri(context: Context, treeUri: Uri) {
		context.contentResolver.takePersistableUriPermission(
			treeUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
		)
		AppSettings.setAutoBackupFolderUri(context, treeUri.toString())
	}

	/** 저장해둔 폴더 권한이 지금도 유효한지 확인한다(사용자가 폴더를 지웠거나 권한을 회수했을 수
	 * 있어서 방어적으로 체크). */
	fun hasValidFolderPermission(context: Context, folderUri: Uri): Boolean {
		return context.contentResolver.persistedUriPermissions.any {
			it.uri == folderUri && it.isWritePermission
		}
	}

	/** 골라둔 폴더 안에 새 zip 파일을 만들고 그 Uri를 돌려준다. DocumentsContract는 안드로이드
	 * SDK에 내장돼 있어서 별도 라이브러리 없이 쓸 수 있다. */
	fun createBackupFileInFolder(context: Context, folderUri: Uri): Uri? {
		val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
			folderUri, DocumentsContract.getTreeDocumentId(folderUri)
		)
		val fileName = "bnote_backup_auto_${System.currentTimeMillis()}.zip"
		return try {
			DocumentsContract.createDocument(
				context.contentResolver, parentDocumentUri, "application/zip", fileName
			)
		} catch (e: Exception) {
			null
		}
	}

	/** 폴더 이름을 화면에 보여줄 때 쓴다(전체 경로 대신 마지막 부분만 짧게). */
	fun displayNameFor(folderUri: Uri): String {
		val docId = try {
			DocumentsContract.getTreeDocumentId(folderUri)
		} catch (e: Exception) {
			null
		}
		val afterColon = docId?.substringAfterLast(':')
		return afterColon?.takeIf { it.isNotBlank() } ?: (folderUri.lastPathSegment ?: "선택한 폴더")
	}
}