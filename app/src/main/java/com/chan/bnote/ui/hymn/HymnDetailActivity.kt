package com.chan.bnote.ui.hymn

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

			findViewById<ImageView>(R.id.image_hymn_sheet)
				.load("file:///android_asset/hymns/images/${hymn.imageFileName}")

			setupVideoCard(findViewById(R.id.card_song), hymn.youtubeSongUrl)
			setupVideoCard(findViewById(R.id.card_mr), hymn.youtubeMrUrl)
		}
	}

	private fun setupVideoCard(cardRoot: View, youtubeUrl: String) {
		val thumbnail = cardRoot.findViewById<ImageView>(R.id.image_thumbnail)
		val playOverlay = cardRoot.findViewById<ImageView>(R.id.icon_play_overlay)
		val webView = cardRoot.findViewById<WebView>(R.id.webview_player)
		val overflowBtn = cardRoot.findViewById<ImageView>(R.id.btn_video_overflow)

		val videoId = extractYoutubeId(youtubeUrl)

		if (videoId != null) {
			thumbnail.load("https://img.youtube.com/vi/$videoId/hqdefault.jpg")
		}

		val playClickListener = View.OnClickListener {
			if (videoId == null) return@OnClickListener
			thumbnail.visibility = View.GONE
			playOverlay.visibility = View.GONE
			webView.visibility = View.VISIBLE

			webView.settings.javaScriptEnabled = true
			webView.settings.mediaPlaybackRequiresUserGesture = false
			webView.webChromeClient = WebChromeClient()
			webView.loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1")
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