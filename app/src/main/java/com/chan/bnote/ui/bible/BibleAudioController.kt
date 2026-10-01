package com.chan.bnote.ui.bible

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleAudioLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 성경 탭의 음성 재생 버튼(개인용 숨김 기능, BibleAudioLibrary 참고).
 *
 * - 재생 버튼을 누르면 지금 보고 있는 장의 음성을 처음부터 재생한다.
 * - 재생 중에 다른 장으로 넘기면 그 장의 음성으로 바로 바뀐다.
 * - 한 장이 끝나면 다음 장으로 넘어가면서 이어서 재생한다(onChapterFinished).
 * - 일시정지한 뒤 다른 장으로 넘기면, 다음에 누를 때 새 장의 처음부터 재생한다.
 */
class BibleAudioController(
	private val context: Context,
	private val scope: CoroutineScope,
	private val button: ImageView,
	private val currentChapter: () -> Pair<Int, Int>,
	private val onChapterFinished: () -> Unit
) {

	private var player: MediaPlayer? = null
	private var playerChapter: Pair<Int, Int>? = null

	/** prepareAsync가 끝나기 전에는 start/pause를 부르면 안 되므로 따로 기억한다. */
	private var isPrepared = false

	/** 사용자가 "듣는 중" 상태로 둔 것인지. 장이 바뀌거나 끝났을 때 이어서 재생할지의 기준이다. */
	private var isListening = false

	/** 파일 찾기/준비가 비동기라서, 그 사이에 다른 장 요청이 들어오면 앞의 요청은 버리기 위한 번호. */
	private var requestToken = 0

	init {
		button.setOnClickListener { onButtonClicked() }
		updateIcon()
	}

	/** 앱 정보에서 폴더를 고르거나 해제하고 돌아왔을 때 버튼 표시 여부를 다시 맞춘다. */
	fun refreshAvailability() {
		val available = BibleAudioLibrary.isConfigured(context)
		button.visibility = if (available) View.VISIBLE else View.GONE
		if (!available) stop()
	}

	fun onChapterChanged(bookId: Int, chapter: Int) {
		if (playerChapter == bookId to chapter) return
		if (isListening) {
			play(bookId to chapter)
		} else {
			releasePlayer()
		}
	}

	fun stop() {
		isListening = false
		releasePlayer()
		updateIcon()
	}

	fun release() {
		stop()
	}

	private fun onButtonClicked() {
		val current = player
		when {
			// 파일을 찾는 중(아직 플레이어가 없음)에 다시 누르면 취소.
			current == null && isListening -> stop()

			current != null && playerChapter == currentChapter() -> {
				if (isListening) {
					if (isPrepared && current.isPlaying) current.pause()
					isListening = false
				} else {
					isListening = true
					// 아직 준비 중이면 준비가 끝나는 순간(onPrepared) 알아서 시작된다.
					if (isPrepared) current.start()
				}
			}

			else -> play(currentChapter())
		}
		updateIcon()
	}

	private fun play(chapterKey: Pair<Int, Int>) {
		releasePlayer()
		isListening = true
		updateIcon()
		val token = ++requestToken

		scope.launch {
			val uri = BibleAudioLibrary.findChapter(context, chapterKey.first, chapterKey.second)
			if (token != requestToken) return@launch
			if (uri == null) {
				Toast.makeText(context, "이 장의 음성 파일이 없어요", Toast.LENGTH_SHORT).show()
				stop()
				return@launch
			}

			try {
				val newPlayer = MediaPlayer().apply {
					setAudioAttributes(
						AudioAttributes.Builder()
							.setUsage(AudioAttributes.USAGE_MEDIA)
							.setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
							.build()
					)
					setDataSource(context, uri)
					setOnPreparedListener {
						if (token != requestToken) return@setOnPreparedListener
						isPrepared = true
						if (isListening) it.start()
					}
					setOnCompletionListener {
						if (token != requestToken) return@setOnCompletionListener
						releasePlayer()
						// isListening은 그대로 둔다 — 다음 장으로 넘어가면 onChapterChanged에서 이어서 재생된다.
						onChapterFinished()
					}
					setOnErrorListener { _, _, _ ->
						if (token == requestToken) {
							Toast.makeText(context, "음성 파일을 재생할 수 없어요", Toast.LENGTH_SHORT).show()
							stop()
						}
						true
					}
				}
				player = newPlayer
				playerChapter = chapterKey
				newPlayer.prepareAsync()
			} catch (e: Exception) {
				Toast.makeText(context, "음성 파일을 재생할 수 없어요", Toast.LENGTH_SHORT).show()
				stop()
			}
		}
	}

	private fun releasePlayer() {
		requestToken++
		player?.let {
			try {
				it.stop()
			} catch (e: Exception) {
				// 아직 준비 중이던 플레이어 — 바로 해제하면 된다.
			}
			it.release()
		}
		player = null
		playerChapter = null
		isPrepared = false
	}

	private fun updateIcon() {
		button.setImageResource(if (isListening) R.drawable.ic_pause else R.drawable.ic_play)
		button.contentDescription = if (isListening) "음성 일시정지" else "음성 재생"
	}
}