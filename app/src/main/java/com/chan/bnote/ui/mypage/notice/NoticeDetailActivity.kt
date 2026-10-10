package com.chan.bnote.ui.mypage.notice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.notice.Notice
import com.chan.bnote.data.notice.NoticeComment
import com.chan.bnote.data.notice.NoticeFormatter
import com.chan.bnote.data.notice.NoticeRepository
import com.chan.bnote.data.notice.NoticeType
import com.chan.bnote.ui.common.LinkifyHelper
import com.chan.bnote.ui.mypage.settings.UpdateDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 알림(공지사항) 상세. 열면 읽음 처리된다.
 * - 본문의 링크는 눌러서 열 수 있다.
 * - 업데이트 알림이면 아래에 "새 버전 받기" 버튼(설치 파일이 있는 구글 드라이브로 연결).
 * - 개발자가 이 공지(GitHub 이슈)에 댓글을 달았으면 "추가 안내"로 아래에 보여준다(인터넷이 필요).
 * - 상단 삭제 버튼: 이 기기에서만 지운다.
 */
class NoticeDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NOTICE_ID = "extra_notice_id"

		fun createIntent(context: Context, noticeId: Long): Intent =
			Intent(context, NoticeDetailActivity::class.java).putExtra(EXTRA_NOTICE_ID, noticeId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_notice_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notice_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val notice = NoticeRepository.find(this, intent.getLongExtra(EXTRA_NOTICE_ID, -1L))
		if (notice == null) {
			// 그 사이 공지가 내려갔거나(이슈 닫힘) 저장된 목록에 없으면 그냥 닫는다.
			finish()
			return
		}

		NoticeRepository.markRead(this, listOf(notice))
		bind(notice)
		findViewById<ImageView>(R.id.btn_notice_delete).setOnClickListener { confirmDelete(notice) }
		if (notice.commentCount > 0) loadComments(notice)
	}

	private fun bind(notice: Notice) {
		NoticeBadges.fill(findViewById(R.id.container_notice_badges), notice)
		findViewById<TextView>(R.id.text_notice_title).text = notice.title

		val dateText = buildString {
			append(NoticeFormatter.fullDate(notice.createdAt))
			// 올린 지 하루 넘게 지나서 고쳤거나 추가 안내가 달렸으면 마지막 수정 날짜도 같이 보여준다.
			if (notice.updatedAt - notice.createdAt > 24 * 60 * 60 * 1000L) {
				append(" · 수정 ")
				append(NoticeFormatter.listDate(notice.updatedAt))
			}
		}
		findViewById<TextView>(R.id.text_notice_date).text = dateText

		val body = findViewById<TextView>(R.id.text_notice_body)
		body.text = NoticeFormatter.toDisplayText(notice.body)
		LinkifyHelper.applySmartLinks(body)

		val download = findViewById<View>(R.id.btn_notice_download)
		download.visibility = if (notice.type == NoticeType.UPDATE) View.VISIBLE else View.GONE
		download.setOnClickListener { UpdateDialog.openDownload(this) }
	}

	private fun loadComments(notice: Notice) {
		val section = findViewById<View>(R.id.section_notice_comments)
		val status = findViewById<TextView>(R.id.text_notice_comments_status)
		val container = findViewById<LinearLayout>(R.id.container_notice_comments)
		section.visibility = View.VISIBLE
		status.visibility = View.VISIBLE
		status.text = "불러오는 중…"

		lifecycleScope.launch {
			val comments = NoticeRepository.fetchComments(notice.id)
			when {
				comments == null -> status.text = "추가 안내를 불러오지 못했어요. 인터넷 연결을 확인해 주세요"
				comments.isEmpty() -> section.visibility = View.GONE
				else -> {
					status.visibility = View.GONE
					container.removeAllViews()
					comments.forEach { container.addView(buildCommentView(it)) }
				}
			}
		}
	}

	private fun buildCommentView(comment: NoticeComment): View {
		val density = resources.displayMetrics.density
		val box = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			background =
				ContextCompat.getDrawable(this@NoticeDetailActivity, R.drawable.bg_book_button)
			setPadding(
				(12 * density).toInt(),
				(10 * density).toInt(),
				(12 * density).toInt(),
				(12 * density).toInt()
			)
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { topMargin = (8 * density).toInt() }
		}
		box.addView(TextView(this).apply {
			text = NoticeFormatter.fullDate(comment.createdAt)
			textSize = 12f
			setTextColor(ContextCompat.getColor(this@NoticeDetailActivity, R.color.text_hint))
		})
		val body = TextView(this).apply {
			text = NoticeFormatter.toDisplayText(comment.body)
			textSize = 14f
			setLineSpacing(0f, 1.35f)
			setTextColor(ContextCompat.getColor(this@NoticeDetailActivity, R.color.text_primary))
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { topMargin = (4 * density).toInt() }
		}
		LinkifyHelper.applySmartLinks(body)
		box.addView(body)
		return box
	}

	private fun confirmDelete(notice: Notice) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("알림 삭제")
			.setMessage("이 알림을 삭제할까요?\n이 기기에서만 지워지고, 다시 볼 수 없어요.")
			.setPositiveButton("삭제") { _, _ ->
				NoticeRepository.hide(this, listOf(notice.id))
				finish()
			}
			.setNegativeButton("취소", null)
			.show()
	}
}