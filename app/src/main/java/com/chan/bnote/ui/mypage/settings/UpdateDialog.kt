package com.chan.bnote.ui.mypage.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.widget.TextView
import android.widget.Toast
import com.chan.bnote.R
import com.chan.bnote.data.notice.NoticeRepository
import com.chan.bnote.data.notice.NoticeType
import com.chan.bnote.data.update.UpdateChecker
import com.chan.bnote.ui.mypage.notice.NoticeDetailActivity
import com.chan.bnote.ui.mypage.notice.NoticeListActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 새 버전 안내 창. 앱을 열 때 자동 확인(MainActivity)과 앱 정보의 "새 버전 확인"이 함께 쓴다.
 * 업데이트 내역은 GitHub 릴리스 노트를 그대로 보여주므로 릴리스 노트는 항목마다 한 줄로 짧게 쓰고,
 * 자세한 설명 · 설치 방법은 "자세히"로 여는 업데이트 알림(공지)에 둔다.
 */
object UpdateDialog {

	/** "・ " 같은 목록 줄 앞부분 — 줄이 길어 넘어가면 둘째 줄을 글자 시작 위치에 맞춘다. */
	private val LIST_PREFIX = Regex("^\\s*(・|※|\\d+\\.)\\s+")

	fun show(context: Context, release: UpdateChecker.Release) {
		val current = UpdateChecker.currentVersionName(context)
		val message = buildString {
			append("$current → ${release.versionName}")
			if (release.notes.isNotBlank()) {
				append("\n\n")
				append(release.notes)
			}
		}

		val dialog = MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 버전이 나왔어요")
			.setMessage(message)
			.setPositiveButton("새 버전 받기") { _, _ -> openDownload(context) }
			.setNegativeButton("나중에") { _, _ -> UpdateChecker.snooze(context, release) }
			// 설치 방법 · 전체 내역은 업데이트 알림(공지)에 있다.
			.setNeutralButton("자세히") { _, _ -> openUpdateNotice(context, release.versionName) }
			.setCancelable(false)
			.show()

		// 창이 뜬 뒤에야 메시지 TextView의 글꼴로 "・ " 폭을 잴 수 있다.
		dialog.findViewById<TextView>(android.R.id.message)?.let { messageView ->
			messageView.text = withHangingIndent(message, messageView)
		}
	}

	/**
	 * 이 버전의 업데이트 알림(제목에 버전이 들어간 업데이트 공지)을 연다. 아직 불러온 알림에 없으면 알림 목록을 연다.
	 * "자세히"를 눌러도 snooze는 하지 않는다 — 다음에 앱을 열면 다시 알려준다.
	 */
	private fun openUpdateNotice(context: Context, versionName: String) {
		val notice = NoticeRepository.visibleNotices(context).firstOrNull {
			it.type == NoticeType.UPDATE && it.title.contains(versionName)
		}
		val intent = if (notice != null) {
			NoticeDetailActivity.createIntent(context, notice.id)
		} else {
			Intent(context, NoticeListActivity::class.java)
		}
		context.startActivity(intent)
	}

	/** 목록 줄이 두 줄 이상이 되면 둘째 줄부터 기호가 아니라 글자 시작 위치에 맞춰 들여쓴다(알림 상세와 같은 방식). */
	private fun withHangingIndent(text: String, textView: TextView): CharSequence {
		val spannable = SpannableString(text)
		var lineStart = 0
		for (line in text.split('\n')) {
			val prefix = LIST_PREFIX.find(line)?.value
			if (prefix != null) {
				val margin = textView.paint.measureText(prefix).toInt()
				val end = (lineStart + line.length + 1).coerceAtMost(text.length)
				spannable.setSpan(
					LeadingMarginSpan.Standard(0, margin),
					lineStart, end,
					Spannable.SPAN_INCLUSIVE_EXCLUSIVE
				)
			}
			lineStart += line.length + 1
		}
		return spannable
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