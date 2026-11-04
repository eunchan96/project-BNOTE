package com.chan.bnote.ui.mypage.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import com.chan.bnote.R
import com.chan.bnote.data.CrashLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * "문의하기" 창. 문의 방법 두 가지(이메일, 1:1 오픈채팅)를 각각 어떤 용도인지 설명과 함께 보여준다(dialog_contact).
 * 앱 정보의 "문의하기"와 알림 상세의 "문의하기" 버튼이 함께 쓴다.
 */
object ContactDialog {

	private const val CONTACT_EMAIL = "taegwon02@gmail.com"
	private const val KAKAO_OPEN_CHAT_URL = "https://open.kakao.com/o/sfqUXEEi"

	fun show(activity: Activity) {
		val content = activity.layoutInflater.inflate(R.layout.dialog_contact, null)
		val dialog = MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("문의하기")
			.setView(content)
			.setNegativeButton("취소", null)
			.show()

		fun option(id: Int, action: () -> Unit) {
			content.findViewById<View>(id).setOnClickListener {
				dialog.dismiss()
				action()
			}
		}
		option(R.id.option_contact_email) { openEmail(activity) }
		option(R.id.option_contact_private_chat) { openKakaoOpenChat(activity) }
	}

	/** 최근 오류 기록이 있으면 첨부해서 메일 앱을 연다. */
	private fun openEmail(activity: Activity) {
		val logFile = CrashLogger.getLatestLogFile(activity)
		val intent = if (logFile != null) {
			val logUri =
				FileProvider.getUriForFile(
					activity,
					"${activity.packageName}.fileprovider",
					logFile
				)
			Intent(Intent.ACTION_SEND).apply {
				type = "text/plain"
				putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
				putExtra(Intent.EXTRA_SUBJECT, "[BNOTE] 문의/피드백")
				putExtra(Intent.EXTRA_TEXT, "최근 오류 기록을 함께 첨부했어요. 어떤 상황이었는지도 같이 적어주시면 도움이 많이 돼요!")
				putExtra(Intent.EXTRA_STREAM, logUri)
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
		} else {
			Intent(Intent.ACTION_SENDTO).apply {
				data = Uri.parse("mailto:")
				putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
				putExtra(Intent.EXTRA_SUBJECT, "[BNOTE] 문의/피드백")
			}
		}
		try {
			activity.startActivity(Intent.createChooser(intent, "메일 앱 선택"))
		} catch (e: Exception) {
			Toast.makeText(
				activity, "메일 앱을 찾을 수 없어요. $CONTACT_EMAIL 로 연락해주세요", Toast.LENGTH_LONG
			).show()
		}
	}

	private fun openKakaoOpenChat(activity: Activity) {
		try {
			activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(KAKAO_OPEN_CHAT_URL)))
		} catch (e: Exception) {
			Toast.makeText(activity, "링크를 열 수 없어요", Toast.LENGTH_SHORT).show()
		}
	}
}