package com.chan.bnote.ui.mypage.notice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
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
import coil.load
import com.chan.bnote.R
import com.chan.bnote.data.notice.Notice
import com.chan.bnote.data.notice.NoticeBlock
import com.chan.bnote.data.notice.NoticeComment
import com.chan.bnote.data.notice.NoticeFormatter
import com.chan.bnote.data.notice.NoticeRepository
import com.chan.bnote.data.notice.NoticeType
import com.chan.bnote.ui.common.LinkifyHelper
import com.chan.bnote.ui.mypage.settings.UpdateDialog
import com.chan.bnote.ui.sermon.addsermon.PhotoViewerActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 알림(공지사항) 상세. 열면 읽음 처리된다.
 * - 본문의 링크는 눌러서 열 수 있다.
 * - 본문에 사진(<img> 태그나 ![](주소))이 있으면 그 자리에 사진으로 보여주고, 누르면 크게 본다.
 * - 업데이트 알림이면 아래에 "새 버전 받기" 버튼(설치 파일이 있는 구글 드라이브로 연결).
 * - 개발자가 이 공지(GitHub 이슈)에 댓글을 달았으면 "추가 안내"로 아래에 보여준다(인터넷이 필요).
 * - 상단 삭제 버튼: 이 기기에서만 지운다.
 */
class NoticeDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NOTICE_ID = "extra_notice_id"

		fun createIntent(context: Context, noticeId: Long): Intent =
			Intent(context, NoticeDetailActivity::class.java).putExtra(EXTRA_NOTICE_ID, noticeId)

		/** 목록 줄의 앞부분: (들여쓰기) + "・ " 또는 "1. " 같은 번호. */
		private val LIST_PREFIX = Regex("^\\s*(・|\\d+\\.)\\s+")
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

		// 끝까지 내렸을 때 마지막 글이 화면 맨 아래에 붙지 않도록, 성경 탭처럼 화면 높이의 30%만큼 여백을 둔다.
		findViewById<View>(R.id.container_notice_scroll_content).apply {
			setPadding(
				paddingLeft,
				paddingTop,
				paddingRight,
				(resources.displayMetrics.heightPixels * 0.3f).toInt()
			)
		}

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

		// 본문은 화면 양옆 16dp씩 여백 안에 있다.
		renderBlocks(
			findViewById(R.id.container_notice_body),
			notice.body,
			textSizeSp = 15f,
			lineSpacing = 1.4f,
			horizontalInsetDp = 32
		)

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
		val body = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { topMargin = (4 * density).toInt() }
		}
		// 추가 안내는 화면 여백(16dp × 2)에 박스 안쪽 여백(12dp × 2)과 테두리가 더해진다.
		renderBlocks(
			body,
			comment.body,
			textSizeSp = 14f,
			lineSpacing = 1.35f,
			horizontalInsetDp = 58
		)
		box.addView(body)
		return box
	}

	/**
	 * [markdown]을 글 · 사진 순서대로 [container]에 채운다.
	 * 이어서 붙어 있는 사진들은 사용 가이드처럼 한 줄에 두 장씩(좁은 화면은 한 장씩) 같은 크기 칸에 놓고,
	 * 설명(alt)이 있으면 사진 아래에 작게 보여준다. 사진을 누르면 이 덩어리(본문 또는 댓글 하나)의 사진들을
	 * 전체화면으로 넘겨볼 수 있다. 사진을 못 불러오면(인터넷 끊김 등) 그 칸은 숨긴다.
	 *
	 * [horizontalInsetDp]: 화면 폭에서 이 덩어리 양옆으로 빠지는 여백의 합. 사진 칸 너비 계산에 쓴다.
	 */
	private fun renderBlocks(
		container: LinearLayout,
		markdown: String,
		textSizeSp: Float,
		lineSpacing: Float,
		horizontalInsetDp: Int
	) {
		container.removeAllViews()
		val blocks = NoticeFormatter.toBlocks(markdown)
		val imageUrls = blocks.filterIsInstance<NoticeBlock.Image>().map { it.url }

		// 가이드(UserGuideItemDetailActivity)와 같은 규칙: 칸 너비를 고정 픽셀로 계산해서,
		// 사진이 한 장뿐이어도 칸 하나 크기만 차지하게 한다(사진마다 크기가 들쭉날쭉하지 않게).
		val columns = if (resources.configuration.smallestScreenWidthDp < 360) 1 else 2
		val gapPx = dp(10)
		val columnWidthPx =
			(resources.displayMetrics.widthPixels - dp(horizontalInsetDp) - gapPx * (columns - 1)) / columns

		// 간격: 글 ↔ 사진이 바뀌는 곳은 한 줄 정도(22dp) 띄우고, 사진 줄끼리는 가이드처럼 10dp.
		val textImageGapPx = dp(22)
		val imageRowGapPx = dp(10)

		var imageIndex = 0
		var i = 0
		while (i < blocks.size) {
			val topMargin = if (container.childCount == 0) 0 else textImageGapPx
			when (val block = blocks[i]) {
				is NoticeBlock.Text -> {
					val textView = TextView(this).apply {
						textSize = textSizeSp
						setLineSpacing(0f, lineSpacing)
						setTextColor(
							ContextCompat.getColor(this@NoticeDetailActivity, R.color.text_primary)
						)
						layoutParams = LinearLayout.LayoutParams(
							LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.WRAP_CONTENT
						).apply { this.topMargin = topMargin }
					}
					// textSize를 먼저 정해야 paint로 "・ " 폭을 정확히 잴 수 있다.
					textView.text = withHangingIndent(block.text, textView)
					LinkifyHelper.applySmartLinks(textView)
					container.addView(textView)
					i++
				}

				is NoticeBlock.Image -> {
					// 글 없이 이어진 사진들을 한 묶음으로 모은다.
					val run = mutableListOf<NoticeBlock.Image>()
					while (i < blocks.size && blocks[i] is NoticeBlock.Image) {
						run.add(blocks[i] as NoticeBlock.Image)
						i++
					}
					run.chunked(columns).forEachIndexed { rowIndex, rowImages ->
						val row = LinearLayout(this).apply {
							orientation = LinearLayout.HORIZONTAL
							layoutParams = LinearLayout.LayoutParams(
								LinearLayout.LayoutParams.MATCH_PARENT,
								LinearLayout.LayoutParams.WRAP_CONTENT
							).apply {
								// 묶음의 첫 줄은 위 글과의 간격, 그다음 줄부터는 사진 줄 사이 간격.
								this.topMargin = when {
									container.childCount == 0 -> 0
									rowIndex == 0 -> textImageGapPx
									else -> imageRowGapPx
								}
							}
						}
						rowImages.forEachIndexed { offsetInRow, image ->
							val startIndex = imageIndex
							val cell = buildImageCell(image) {
								PhotoViewerActivity.start(this, imageUrls, startIndex)
							}
							cell.layoutParams = LinearLayout.LayoutParams(
								columnWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT
							).apply { if (offsetInRow > 0) marginStart = gapPx }
							row.addView(cell)
							imageIndex++
						}
						container.addView(row)
					}
				}
			}
		}
	}

	/**
	 * "・ 항목"이나 "1. 항목"처럼 시작하는 줄이 길어서 두 줄 이상이 되면, 둘째 줄부터 기호가 아니라
	 * 글자 시작 위치에 맞춰 들여쓴다(업데이트 내역과 같은 방식). 앞에 들여쓰기 공백이 있으면 그것까지 포함한다.
	 */
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

	/**
	 * 사진 한 칸: 가이드 사진처럼 테두리 박스 안의 사진 + (있으면) 아래 작은 설명.
	 * 불러오는 동안은 빈 칸 높이만 잡아두고, 다 불러오면 원래 비율대로 보여준다.
	 */
	private fun buildImageCell(image: NoticeBlock.Image, onClick: () -> Unit): LinearLayout {
		val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
		val imageView = ImageView(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			)
			adjustViewBounds = true
			scaleType = ImageView.ScaleType.FIT_CENTER
			background =
				ContextCompat.getDrawable(this@NoticeDetailActivity, R.drawable.bg_book_button)
			setPadding(dp(4), dp(4), dp(4), dp(4))
			minimumHeight = dp(160)
			contentDescription = image.caption ?: "알림 사진"
			isClickable = true
			isFocusable = true
			setOnClickListener { onClick() }
			load(image.url) {
				crossfade(true)
				listener(
					onSuccess = { _, _ -> minimumHeight = 0 },
					onError = { _, _ -> cell.visibility = View.GONE }
				)
			}
		}
		cell.addView(imageView)

		if (image.caption != null) {
			cell.addView(TextView(this).apply {
				text = image.caption
				textSize = 12f
				gravity = android.view.Gravity.CENTER
				setTextColor(ContextCompat.getColor(this@NoticeDetailActivity, R.color.text_hint))
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { topMargin = dp(4) }
			})
		}
		return cell
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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