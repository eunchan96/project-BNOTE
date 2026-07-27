package com.chan.bnote.ui.sermon.detail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.CitationParser
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.sermonphoto.SermonPhotoStorage
import com.chan.bnote.ui.sermon.addsermon.AddSermonActivity
import com.chan.bnote.ui.sermon.addsermon.PhotoViewerActivity
import com.chan.bnote.ui.sermon.addsermon.RichTextUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SermonDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_SERMON_ID = "extra_sermon_id"

		fun start(context: Context, sermonId: Long) {
			context.startActivity(createIntent(context, sermonId))
		}

		fun createIntent(context: Context, sermonId: Long): Intent {
			val intent = Intent(context, SermonDetailActivity::class.java)
			intent.putExtra(EXTRA_SERMON_ID, sermonId)
			return intent
		}
	}

	private var changed = false
	private lateinit var sermon: Sermon

	private val editSermonLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			changed = true
			loadSermon()
		}
	}

	override fun onResume() {
		super.onResume()
		// 적용하러 가기/적용 보러 가기 눌렀다가 돌아왔을 수도 있으니, 그 상태를 다시 확인한다.
		if (::sermon.isInitialized) loadSermon()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_sermon_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sermon_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finishWithResult() }

		findViewById<ImageView>(R.id.btn_edit_sermon).setOnClickListener {
			editSermonLauncher.launch(AddSermonActivity.editIntent(this, sermon.id))
		}

		findViewById<ImageView>(R.id.btn_delete_sermon).setOnClickListener {
			MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
				.setTitle("설교 삭제")
				.setMessage("'${sermon.title}'을(를) 삭제할까요?")
				.setPositiveButton("삭제") { _, _ ->
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.sermonBibleRefDao().deleteBySermon(sermon.id)
						db.sermonPhotoDao().getBySermon(sermon.id).forEach { photo ->
							SermonPhotoStorage.deleteFile(photo.filePath)
						}
						db.sermonPhotoDao().deleteBySermon(sermon.id)
						db.sermonDao().delete(sermon)
						changed = true
						finishWithResult()
					}
				}
				.setNegativeButton("취소", null)
				.show()
		}

		onBackPressedDispatcher.addCallback(this) {
			finishWithResult()
		}

		loadSermon()
	}

	private fun finishWithResult() {
		setResult(if (changed) Activity.RESULT_OK else Activity.RESULT_CANCELED)
		finish()
	}

	private fun loadSermon() {
		val sermonId = intent.getLongExtra(EXTRA_SERMON_ID, -1L)
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val loaded = db.sermonDao().getById(sermonId)
			if (loaded == null) {
				finishWithResult()
				return@launch
			}
			sermon = loaded
			render(db)
		}
	}

	private suspend fun render(db: com.chan.bnote.data.BibleDatabase) {
		findViewById<TextView>(R.id.text_top_bar_title).text = sermon.title

		val memoView = findViewById<TextView>(R.id.text_sermon_memo)

		val preacherName =
			sermon.preacherId?.let { db.preacherDao().getById(it)?.name } ?: "설교자 미지정"
		val category = sermon.categoryId?.let { db.sermonCategoryDao().getById(it) }
		val dateLabel = DateUtils.formatDate(sermon.sermonDate)

		// 상단바: 제목 대신 "2026년 7월 24일 [주일 낮예배]" 형태로
		findViewById<TextView>(R.id.text_top_bar_title).text = if (category != null) {
			"$dateLabel [${category.name}]"
		} else {
			dateLabel
		}

		val refs = db.sermonBibleRefDao().getBySermon(sermon.id)

		// 본문 정보: "제목 : ~ / 본문 : ~(밑줄, 누르면 이동) / 설교 : ~"
		val infoView = findViewById<TextView>(R.id.text_sermon_info)
		val infoBuilder = SpannableStringBuilder()
		infoBuilder.append("제목 : ${sermon.title}\n")
		infoBuilder.append("본문 : ")

		val refClickRanges =
			mutableListOf<Triple<Int, Int, com.chan.bnote.data.sermon.SermonBibleRef>>()
		if (refs.isEmpty()) {
			infoBuilder.append("없음")
		} else {
			for ((index, ref) in refs.withIndex()) {
				val start = infoBuilder.length
				infoBuilder.append(ref.toDisplayLabel())
				val end = infoBuilder.length
				refClickRanges.add(Triple(start, end, ref))
				infoBuilder.setSpan(
					android.text.style.UnderlineSpan(),
					start,
					end,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				if (index != refs.lastIndex) infoBuilder.append(", ")
			}
		}
		infoBuilder.append("\n설교 : $preacherName")

		for ((start, end, ref) in refClickRanges) {
			infoBuilder.setSpan(
				object : android.text.style.ClickableSpan() {
					override fun onClick(widget: View) {
						val intent =
							Intent(this@SermonDetailActivity, MainActivity::class.java).apply {
								putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, ref.startBookId)
								putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, ref.startChapter)
								putExtra(MainActivity.EXTRA_NAVIGATE_VERSE, ref.startVerse)
								flags =
									Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
							}
						startActivity(intent)
					}

					override fun updateDrawState(ds: android.text.TextPaint) {
						super.updateDrawState(ds)
						ds.color = androidx.core.content.ContextCompat.getColor(
							this@SermonDetailActivity, R.color.brown_primary
						)
						ds.isUnderlineText = true
					}
				},
				start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
		infoView.text = infoBuilder
		infoView.movementMethod = android.text.method.LinkMovementMethod.getInstance()

		// 메모 (굵게/밑줄 서식 복원 + 인용 구절 강조 + 롱프레스 말풍선)
		if (sermon.memo.isBlank()) {
			memoView.text = "메모가 없어요"
		} else {
			val restored = RichTextUtils.toEditable(sermon.memo)
			val plainText = restored.toString()

			val bookCitations = CitationParser.findCitations(plainText)
			// 본문(성경 구절)이 정확히 하나일 때만, "1절"처럼 절 번호만 있는 표기나 "(2:1)"처럼
			// 책 표기 없는 장:절 표기도 그 본문 기준으로 찾아준다.
			val contextualCitations = if (refs.size == 1) {
				val verseOnly = CitationParser.findVerseOnlyCitations(
					plainText,
					refs[0].startBookId,
					refs[0].startChapter
				)
				val chapterVerse =
					CitationParser.findChapterVerseCitations(plainText, refs[0].startBookId)
				(verseOnly + chapterVerse).filter { candidate ->
					bookCitations.none { existing ->
						existing.range.first <= candidate.range.last && candidate.range.first <= existing.range.last
					}
				}
			} else {
				emptyList()
			}
			val citations = bookCitations + contextualCitations

			val spannable = if (restored is Spannable) restored else SpannableString(restored)
			for (citation in citations) {
				val end = (citation.range.last + 1).coerceAtMost(spannable.length)
				if (citation.range.first >= end) continue
				// 사용자가 직접 준 굵게/밑줄과 헷갈리지 않도록, 인용구는 밑줄이 아니라 강조색+굵게로 표시한다.
				spannable.setSpan(
					ForegroundColorSpan(
						androidx.core.content.ContextCompat.getColor(
							this,
							R.color.brown_primary
						)
					),
					citation.range.first, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
				spannable.setSpan(
					StyleSpan(Typeface.BOLD),
					citation.range.first, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
			memoView.text = spannable
			CitationBubbleHelper.attachTouchHandling(memoView, { citations }, lifecycleScope)
			com.chan.bnote.ui.common.LinkifyHelper.applySmartLinks(memoView)
		}

		// 첨부 사진
		val photoScroll = findViewById<View>(R.id.scroll_detail_photos)
		val photoContainer = findViewById<LinearLayout>(R.id.container_detail_photos)
		photoContainer.removeAllViews()
		val photos = db.sermonPhotoDao().getBySermon(sermon.id)
		photoScroll.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
		for (photo in photos) {
			val thumb = LayoutInflater.from(this)
				.inflate(R.layout.item_sermon_detail_photo, photoContainer, false) as ImageView
			thumb.load(photo.filePath)
			thumb.setOnClickListener { PhotoViewerActivity.start(this, photo.filePath) }
			photoContainer.addView(thumb)
		}

		// 링크 (유튜브면 바로 임베드, 아니면 눌러서 브라우저로 열기)
		val webView = findViewById<android.webkit.WebView>(R.id.webview_sermon_link)
		val plainLinkView = findViewById<TextView>(R.id.text_sermon_link_plain)
		val link = sermon.link
		if (link.isNullOrBlank()) {
			webView.visibility = View.GONE
			plainLinkView.visibility = View.GONE
		} else {
			val videoId = extractYoutubeId(link)
			if (videoId != null) {
				webView.visibility = View.VISIBLE
				plainLinkView.visibility = View.GONE
				webView.settings.javaScriptEnabled = true
				val embedOrigin = "https://bnote.app"
				val html = """
					<html><body style="margin:0;padding:0;">
					<iframe width="100%" height="100%"
						src="https://www.youtube.com/embed/$videoId?playsinline=1&origin=$embedOrigin"
						frameborder="0" allowfullscreen></iframe>
					</body></html>
				""".trimIndent()
				webView.loadDataWithBaseURL(embedOrigin, html, "text/html", "utf-8", null)
			} else {
				webView.visibility = View.GONE
				plainLinkView.visibility = View.VISIBLE
				plainLinkView.setOnClickListener {
					startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link)))
				}
			}
		}

		// 이 설교에 이미 연결된 적용이 있으면 "적용 보러 가기", 없으면 "적용하러 가기".
		val existingApplication = db.applicationDao().getFirstBySermonId(sermon.id)
		val btnApplication = findViewById<TextView>(R.id.btn_go_to_application)
		if (existingApplication != null) {
			btnApplication.text = "적용 보러 가기"
			btnApplication.setOnClickListener {
				com.chan.bnote.ui.application.ApplicationDetailActivity.start(
					this@SermonDetailActivity, existingApplication.id
				)
			}
		} else {
			btnApplication.text = "적용하러 가기"
			btnApplication.setOnClickListener {
				startActivity(
					com.chan.bnote.ui.application.addapplication.AddApplicationActivity
						.createIntentForSermon(this@SermonDetailActivity, sermon.id)
				)
			}
		}
	}

	/** youtube.com/watch?v=ID, youtu.be/ID, youtube.com/live/ID, youtube.com/shorts/ID 형식 모두 지원. */
	private fun extractYoutubeId(url: String): String? {
		val watchRegex = Regex("[?&]v=([a-zA-Z0-9_-]{6,})")
		watchRegex.find(url)?.let { return it.groupValues[1] }

		val shortRegex = Regex("youtu\\.be/([a-zA-Z0-9_-]{6,})")
		shortRegex.find(url)?.let { return it.groupValues[1] }

		val liveOrShortsRegex = Regex("youtube\\.com/(?:live|shorts)/([a-zA-Z0-9_-]{6,})")
		liveOrShortsRegex.find(url)?.let { return it.groupValues[1] }

		return null
	}
}