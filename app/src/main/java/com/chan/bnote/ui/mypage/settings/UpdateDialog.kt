package com.chan.bnote.ui.mypage.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.chan.bnote.R
import com.chan.bnote.data.update.UpdateChecker
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** 새 버전 안내 창. 앱을 열 때 자동 확인(MainActivity)과 앱 정보의 "새 버전 확인"이 함께 쓴다. */
object UpdateDialog {

	fun show(context: Context, release: UpdateChecker.Release) {
		val current = UpdateChecker.currentVersionName(context)
		val message = buildString {
			append("지금 버전: $current → 새 버전: ${release.versionName}\n")
			append("\"새 버전 받기\"를 누르면 설치 파일(APK)을 내려받을 수 있어요. 다 받은 뒤 파일을 열어 설치하면 돼요.")
			if (release.notes.isNotBlank()) {
				append("\n\n[업데이트 내역]\n")
				append(release.notes)
			}
		}
		MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 버전이 나왔어요")
			.setMessage(message)
			.setPositiveButton("새 버전 받기") { _, _ -> openDownload(context) }
			.setNegativeButton("나중에") { _, _ -> UpdateChecker.snooze(context, release) }
			.setCancelable(false)
			.show()
	}

	/** 새 버전 설치 파일(APK)이 있는 구글 드라이브 링크를 연다. */
	fun openDownload(context: Context) {
		try {
			context.startActivity(
				Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.APK_DOWNLOAD_URL))
			)
		} catch (e: Exception) {
			Toast.makeText(context, "링크를 열 수 없어요", Toast.LENGTH_SHORT).show()
		}
	}
}