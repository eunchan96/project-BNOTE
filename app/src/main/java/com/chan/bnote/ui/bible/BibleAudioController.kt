package com.chan.bnote.ui.bible

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.widget.Toast
import com.chan.bnote.data.bible.BibleAudioLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 성경 탭의 음성 재생(개인용 숨김 기능, BibleAudioLibrary 참고). 화면(버튼·툴바)은 갖지 않고
 * 재생 상태만 관리한다 — 하단바 헤드셋 버튼과 재생 툴바는 BibleFragment가 이 상태를 보고 그린다.
 *
 * - toggle(): 지금 보는 장을 재생하거나, 재생 중이면 일시정지한다(같은 장이면 멈춘 위치부터 이어서).
 * - 재생 중에 다른 장으로 넘기면 그 장의 음성으로 바로 바뀐다.
 * - 한 장이 끝나면 다음 장으로 넘어가면서 이어서 재생한다(onChapterFinished).
 * - 일시정지한 뒤 다른 장으로 넘기면, 다음에 누를 때 새 장의 처음부터 재생한다.
 */
class BibleAudioController(
	private val context: Context,
	private val scope: CoroutineScope,
	private val currentChapter: () -> Pair<Int, Int>,
	private val onChapterFinished: () -> Unit,
	private val onStateChanged: () -> Unit
) {

	private var player: MediaPlayer? = null
	private var playerChapter: Pair<Int, Int>? = null

	/** prepareAsync가 끝나기 전에는 start/pause/seekTo를 부르면 안 되므로 따로 기억한다. */
	private var isPrepared = false

	/** 사용자가 "듣는 중" 상태로 둔 것인지. 하단바 버튼 색과, 장이 바뀌거나 끝났을 때 이어서
	 * 재생할지의 기준이다. */
	var isListening = false
		private set

	/** 음성 폴더가 골라져 있어서 이 기능을 쓸 수 있는지(하단바 버튼 표시 기준). */
	var isAvailable = false
		private set

	var speed: Float = BibleAudioLibrary.getPlaybackSpeed(context)
		private set

	/** 파일 찾기/준비가 비동기라서, 그 사이에 다른 장 요청이 들어오면 앞의 요청은 버리기 위한 번호. */
	private var requestToken = 0

	/** 앱 정보에서 폴더를 고르거나 해제하고 돌아왔을 때 사용 가능 여부를 다시 맞춘다. */
	fun refreshAvailability() {
		val available = BibleAudioLibrary.isConfigured(context)
		if (available == isAvailable) return
		isAvailable = available
		if (!available) stopInternal()
		onStateChanged()
	}

	fun toggle() {
		val current = player
		when {
			// 파일을 찾는 중(아직 플레이어가 없음)에 다시 누르면 취소.
			current == null && isListening -> stopInternal()

			current != null && playerChapter == currentChapter() -> {
				if (isListening) {
					if (isPrepared && current.isPlaying) current.pause()
					isListening = false
				} else {
					isListening = true
					// 아직 준비 중이면 준비가 끝나는 순간(onPrepared) 알아서 시작된다.
					if (isPrepared) startWithSpeed(current)
				}
			}

			else -> play(currentChapter())
		}
		onStateChanged()
	}

	fun onChapterChanged(bookId: Int, chapter: Int) {
		if (playerChapter == bookId to chapter) return
		if (isListening) {
			play(bookId to chapter)
		} else {
			releasePlayer()
		}
		onStateChanged()
	}

	/** 재생바에 쓸 값. 준비된 플레이어가 없으면(아직 재생 전·파일 준비 중) 0. */
	fun durationMs(): Int = player?.takeIf { isPrepared }?.duration?.coerceAtLeast(0) ?: 0

	fun positionMs(): Int = player?.takeIf { isPrepared }?.currentPosition?.coerceAtLeast(0) ?: 0

	/** 재생바로 위치를 옮긴다. 일시정지 중이면 위치만 옮기고 멈춘 상태를 유지한다. */
	fun seekTo(positionMs: Int) {
		val current = player ?: return
		if (!isPrepared) return
		current.seekTo(positionMs.coerceIn(0, current.duration.coerceAtLeast(0)))
	}

	fun changeSpeed(newSpeed: Float) {
		speed = newSpeed
		BibleAudioLibrary.setPlaybackSpeed(context, newSpeed)
		// 일시정지 중에 setPlaybackParams를 부르면 재생이 시작돼버리므로, 재생 중일 때만 바로 적용한다.
		// 멈춰 있으면 다음에 재생을 시작할 때(startWithSpeed) 적용된다.
		val current = player
		if (current != null && isPrepared && current.isPlaying) applySpeed(current)
		onStateChanged()
	}

	fun release() {
		stopInternal()
	}

	private fun stopInternal() {
		isListening = false
		releasePlayer()
	}

	private fun play(chapterKey: Pair<Int, Int>) {
		releasePlayer()
		isListening = true
		val token = ++requestToken

		scope.launch {
			val uri = BibleAudioLibrary.findChapter(context, chapterKey.first, chapterKey.second)
			if (token != requestToken) return@launch
			if (uri == null) {
				Toast.makeText(context, "이 장의 음성 파일이 없어요", Toast.LENGTH_SHORT).show()
				stopInternal()
				onStateChanged()
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
						if (isListening) startWithSpeed(it)
						onStateChanged()
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
							stopInternal()
							onStateChanged()
						}
						true
					}
				}
				player = newPlayer
				playerChapter = chapterKey
				newPlayer.prepareAsync()
			} catch (e: Exception) {
				Toast.makeText(context, "음성 파일을 재생할 수 없어요", Toast.LENGTH_SHORT).show()
				stopInternal()
				onStateChanged()
			}
		}
	}

	private fun startWithSpeed(target: MediaPlayer) {
		target.start()
		applySpeed(target)
	}

	private fun applySpeed(target: MediaPlayer) {
		try {
			target.playbackParams = (target.playbackParams ?: PlaybackParams()).setSpeed(speed)
		} catch (e: Exception) {
			// 일부 기기에서 배속 변경을 지원하지 않는 경우 — 기본 속도로 계속 재생한다.
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
}