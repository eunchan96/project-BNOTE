package com.chan.bnote.ui.bible.hymn

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import kotlinx.coroutines.launch

class HymnDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NUMBER = "extra_hymn_number"

		fun start(context: Context, number: Int) {
			val intent = Intent(context, HymnDetailActivity::class.java)
			intent.putExtra(EXTRA_NUMBER, number)
			context.startActivity(intent)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_hymn_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hymn_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val number = intent.getIntExtra(EXTRA_NUMBER, -1)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val hymn = db.hymnDao().getByNumber(number)
			if (hymn == null) {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_top_bar_title).text = "${hymn.number}장 ${hymn.title}"

			renderSheetImages(hymn.imageFileName)

			setupVideoCard(findViewById(R.id.card_song), hymn.youtubeSongUrl)
			setupVideoCard(findViewById(R.id.card_mr), hymn.youtubeMrUrl)
		}
	}

	private fun renderSheetImages(imageFileNames: String) {
		val container = findViewById<LinearLayout>(R.id.container_hymn_sheets)
		container.removeAllViews()

		val files = imageFileNames.split("|").filter { it.isNotBlank() }
		files.forEachIndexed { index, fileName ->
			val imageView = ImageView(this).apply {
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply {
					if (index > 0) topMargin = (8 * resources.displayMetrics.density).toInt()
				}
				adjustViewBounds = true
				scaleType = ImageView.ScaleType.FIT_CENTER
				contentDescription = "악보 ${index + 1}페이지"
			}
			imageView.load("file:///android_asset/hymns/images/$fileName")
			container.addView(imageView)
		}
	}

	private fun setupVideoCard(cardRoot: View, youtubeUrl: String) {
		val thumbnail = cardRoot.findViewById<ImageView>(R.id.image_thumbnail)
		val playOverlay = cardRoot.findViewById<ImageView>(R.id.icon_play_overlay)
		val webView = cardRoot.findViewById<WebView>(R.id.webview_player)
		val overflowBtn = cardRoot.findViewById<ImageView>(R.id.btn_video_overflow)

		// 화면 실제 폭에 맞춰 16:9 비율로 카드 높이를 다시 계산한다 (고정 200dp라 좌우가 비어 보이던 문제 수정).
		cardRoot.post {
			val width = cardRoot.width
			if (width > 0) {
				val height = width * 9 / 16
				cardRoot.layoutParams = cardRoot.layoutParams.apply { this.height = height }
				thumbnail.layoutParams = thumbnail.layoutParams.apply { this.height = height }
				webView.layoutParams = webView.layoutParams.apply { this.height = height }
				cardRoot.requestLayout()
			}
		}

		val videoId = extractYoutubeId(youtubeUrl)
		android.util.Log.d("HymnDetail", "youtubeUrl=$youtubeUrl -> videoId=$videoId")

		if (videoId != null) {
			thumbnail.load("https://img.youtube.com/vi/$videoId/hqdefault.jpg") {
				listener(
					onError = { _, result ->
						android.util.Log.e(
							"HymnDetail",
							"썸네일 로드 실패: videoId=$videoId", result.throwable
						)
					},
					onSuccess = { _, _ ->
						android.util.Log.d("HymnDetail", "썸네일 로드 성공: videoId=$videoId")
					}
				)
			}
		}

		val playClickListener = View.OnClickListener {
			if (videoId == null) return@OnClickListener
			thumbnail.visibility = View.GONE
			playOverlay.visibility = View.GONE
			webView.visibility = View.VISIBLE

			webView.settings.javaScriptEnabled = true
			webView.settings.mediaPlaybackRequiresUserGesture = false
			webView.settings.domStorageEnabled = true
			webView.settings.loadWithOverviewMode = true
			webView.settings.useWideViewPort = true
			webView.webViewClient = object : android.webkit.WebViewClient() {
				override fun onPageStarted(
					view: WebView?, url: String?, favicon: android.graphics.Bitmap?
				) {
					android.util.Log.d("HymnDetail", "WebView onPageStarted url=$url")
				}

				override fun onPageFinished(view: WebView?, url: String?) {
					android.util.Log.d("HymnDetail", "WebView onPageFinished url=$url")
				}

				override fun onReceivedError(
					view: WebView?,
					request: android.webkit.WebResourceRequest?,
					error: android.webkit.WebResourceError?
				) {
					android.util.Log.e(
						"HymnDetail",
						"WebView onReceivedError url=${request?.url} " +
								"errorCode=${error?.errorCode} description=${error?.description}"
					)
				}
			}
			webView.webChromeClient = WebChromeClient()

			val embedOrigin = "https://bnote.app"
			val html = """
				<html>
				<body style="margin:0;padding:0;background:#000;">
				<iframe width="100%" height="100%"
					src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&origin=$embedOrigin&enablejsapi=1"
					frameborder="0"
					allow="autoplay; encrypted-media"
					allowfullscreen></iframe>
				</body>
				</html>
			""".trimIndent()
			android.util.Log.d("HymnDetail", "재생 버튼 눌림, iframe 로드 시작 videoId=$videoId")
			webView.loadDataWithBaseURL(embedOrigin, html, "text/html", "utf-8", null)
		}
		thumbnail.setOnClickListener(playClickListener)
		playOverlay.setOnClickListener(playClickListener)

		overflowBtn.setOnClickListener { anchor ->
			showVideoOverflowMenu(anchor, youtubeUrl, videoId)
		}
	}

	private fun showVideoOverflowMenu(anchor: View, youtubeUrl: String, videoId: String?) {
		val popup = PopupMenu(this, anchor)
		popup.menu.add(0, 0, 0, "유튜브에서 보기")
		popup.menu.add(0, 1, 1, "링크 복사")
		popup.menu.add(0, 2, 2, "브라우저에서 열기")
		popup.setOnMenuItemClickListener { item ->
			when (item.itemId) {
				0 -> openInYoutubeApp(youtubeUrl, videoId)
				1 -> copyLinkToClipboard(youtubeUrl)
				2 -> openInBrowser(youtubeUrl)
			}
			true
		}
		popup.show()
	}

	private fun openInYoutubeApp(youtubeUrl: String, videoId: String?) {
		try {
			val intent = if (videoId != null) {
				Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
			} else {
				Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl))
			}
			startActivity(intent)
		} catch (e: ActivityNotFoundException) {
			openInBrowser(youtubeUrl)
		}
	}

	private fun copyLinkToClipboard(youtubeUrl: String) {
		val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		clipboard.setPrimaryClip(ClipData.newPlainText("youtube_url", youtubeUrl))
		Toast.makeText(this, "링크를 복사했어요", Toast.LENGTH_SHORT).show()
	}

	private fun openInBrowser(youtubeUrl: String) {
		startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)))
	}

	/** youtube.com/watch?v=ID 및 youtu.be/ID 형식 모두 지원. */
	private fun extractYoutubeId(url: String): String? {
		val watchRegex = Regex("[?&]v=([a-zA-Z0-9_-]{6,})")
		watchRegex.find(url)?.let { return it.groupValues[1] }

		val shortRegex = Regex("youtu\\.be/([a-zA-Z0-9_-]{6,})")
		shortRegex.find(url)?.let { return it.groupValues[1] }

		return null
	}
}