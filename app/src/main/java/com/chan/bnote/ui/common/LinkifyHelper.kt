package com.chan.bnote.ui.common

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.widget.TextView

/**
 * 메모(설교 메모, 구절/단어 메모 등)에 있는 링크를 눌렀을 때, 유튜브 링크면 유튜브 앱으로,
 * 아니면 인터넷 앱을 고를 수 있는 창으로 열어준다.
 */
object LinkifyHelper {

	private val youtubeRegex = Regex(
		"(?:youtube\\.com/(?:watch\\?v=|live/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{6,})"
	)

	/** 텍스트뷰(읽기 전용 표시, EditText 둘 다 가능) 안의 URL을 찾아서 탭하면 열리게 만든다. */
	fun applySmartLinks(textView: TextView) {
		val text = textView.text ?: return
		val spannable = SpannableString(text)

		// 우선 표준 Linkify로 URL 구간을 다 찾은 다음, 그 구간에 우리가 원하는 클릭 동작을 다시 입힌다.
		Linkify.addLinks(spannable, Linkify.WEB_URLS)
		val urlSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)

		for (urlSpan in urlSpans) {
			val start = spannable.getSpanStart(urlSpan)
			val end = spannable.getSpanEnd(urlSpan)
			val url = urlSpan.url
			spannable.removeSpan(urlSpan)
			spannable.setSpan(
				object : ClickableSpan() {
					override fun onClick(widget: View) {
						openLink(widget, url)
					}
				},
				start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}

		textView.text = spannable
		if (urlSpans.isNotEmpty()) {
			textView.movementMethod = LinkMovementMethod.getInstance()
		}
	}

	private fun openLink(view: View, url: String) {
		val context = view.context
		val videoId = youtubeRegex.find(url)?.groupValues?.get(1)

		if (videoId != null) {
			// 유튜브 링크면 유튜브 앱으로 먼저 시도하고, 앱이 없으면 그냥 링크를 연다.
			try {
				val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
				appIntent.setPackage("com.google.android.youtube")
				context.startActivity(appIntent)
				return
			} catch (e: ActivityNotFoundException) {
				// 유튜브 앱이 없으면 아래 일반 링크 열기로 넘어간다.
			}
		}

		try {
			val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
			context.startActivity(Intent.createChooser(viewIntent, "링크 열기"))
		} catch (e: ActivityNotFoundException) {
			// 열 수 있는 앱이 아예 없으면 조용히 무시한다.
		}
	}
}