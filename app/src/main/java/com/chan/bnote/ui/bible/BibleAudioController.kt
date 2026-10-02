package com.chan.bnote.ui.bible

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.chan.bnote.data.bible.BibleAudioLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 성경 탭의 음성 재생(개인용 숨김 기능, BibleAudioLibrary 참고). 화면(툴바)은 갖지 않고
 * 재생 상태만 관리한다 — 재생 툴바(하단바 성경 탭 아이콘 길게 누르기)는 BibleFragment가 이 상태를 보고 그린다.
 *
 * - toggle(): 지금 보는 장을 재생하거나, 재생 중이면 일시정지한다(같은 장이면 멈춘 위치부터 이어서).
 * - 재생 중에 다른 장으로 넘기면 그 장의 음성으로 바로 바뀐다.
 * - 한 장이 끝나면 다음 장으로 넘어가면서 이어서 재생한다(onChapterFinished).
 * - 일시정지한 뒤 다른 장으로 넘기면, 다음에 누를 때 새 장의 처음부터 재생한다.
 * - 취침 타이머(setSleepTimer)를 걸어두면, 정한 시간이 지난 뒤 그때 듣던 장이 끝날 때 멈춘다.
 *   틀어둔 채 잠들어도 요한계시록까지 계속 넘어가지 않게 하려는 것. 장 중간에서 뚝 끊지 않아서,
 *   다음에 들을 때 그 다음 장 처음부터 자연스럽게 이어 들을 수 있다.
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

	/** 사용자가 "듣는 중" 상태로 둔 것인지. 툴바의 재생/일시정지 아이콘과, 장이 바뀌거나 끝났을 때
	 * 이어서 재생할지의 기준이다. */
	var isListening = false
		private set

	/** 음성 폴더가 골라져 있어서 이 기능을 쓸 수 있는지(재생 툴바를 열 수 있는지의 기준). */
	var isAvailable = false
		private set

	var speed: Float = BibleAudioLibrary.getPlaybackSpeed(context)
		private set

	/** 파일 찾기/준비가 비동기라서, 그 사이에 다른 장 요청이 들어오면 앞의 요청은 버리기 위한 번호. */
	private var requestToken = 0

	/**
	 * 지금 고른 취침 타이머. null = 끄기, 0 = 이 장이 끝나면, 양수 = 그 분만큼 지난 뒤 듣던 장이 끝나면.
	 * 앱을 다시 켜면 꺼진 상태로 시작한다(그날 그때만 쓰는 설정이라 저장하지 않는다).
	 */
	var sleepTimerMinutes: Int? = null
		private set

	/** 정한 시간이 다 돼서(또는 "이 장이 끝나면"을 골라서) 지금 장이 끝나면 멈춰야 하는 상태. */
	private var stopAtChapterEnd = false
	private var sleepTimerEndAt = 0L
	private val sleepTimerHandler = Handler(Looper.getMainLooper())
	private val sleepTimerRunnable = Runnable {
		if (isListening) {
			stopAtChapterEnd = true
		} else {
			// 이미 멈춰 있으면 기다릴 장이 없으니 그냥 끈다.
			clearSleepTimer()
		}
		onStateChanged()
	}

	val isSleepTimerOn: Boolean get() = sleepTimerMinutes != null

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

	/** 지금 위치에서 deltaMs만큼 앞(+)/뒤(-)로 옮긴다(툴바의 3초 앞으로/뒤로 버튼). 끝을 넘기면
	 * 끝으로 가서 그 장이 끝난 것으로 처리되고(다음 장으로 넘어감), 처음보다 앞이면 처음으로 간다. */
	fun seekBy(deltaMs: Int) {
		val current = player ?: return
		if (!isPrepared) return
		seekTo(current.currentPosition + deltaMs)
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

	/** 취침 타이머를 새로 건다(null이면 끈다). 고를 때마다 처음부터 다시 잰다. */
	fun setSleepTimer(minutes: Int?) {
		sleepTimerHandler.removeCallbacks(sleepTimerRunnable)
		sleepTimerMinutes = minutes
		stopAtChapterEnd = minutes == 0
		if (minutes != null && minutes > 0) {
			val delayMs = minutes * 60_000L
			sleepTimerEndAt = System.currentTimeMillis() + delayMs
			sleepTimerHandler.postDelayed(sleepTimerRunnable, delayMs)
		}
		onStateChanged()
	}

	/** 시간을 정해둔 타이머가 아직 도는 중이면 남은 분(올림). 그 외(꺼짐·이 장이 끝나면·시간 다 됨)는 null. */
	fun sleepTimerRemainingMinutes(): Int? {
		val minutes = sleepTimerMinutes ?: return null
		if (minutes <= 0 || stopAtChapterEnd) return null
		val remainingMs = (sleepTimerEndAt - System.currentTimeMillis()).coerceAtLeast(0L)
		return ((remainingMs + 59_999L) / 60_000L).toInt()
	}

	private fun clearSleepTimer() {
		sleepTimerHandler.removeCallbacks(sleepTimerRunnable)
		sleepTimerMinutes = null
		stopAtChapterEnd = false
	}

	fun release() {
		clearSleepTimer()
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
						if (stopAtChapterEnd) {
							// 취침 타이머: 다음 장으로 넘어가지 않고 여기서 멈춘다. 타이머도 다 쓴 것으로 끈다.
							clearSleepTimer()
							isListening = false
							onStateChanged()
						} else {
							// isListening은 그대로 둔다 — 다음 장으로 넘어가면 onChapterChanged에서 이어서 재생된다.
							onChapterFinished()
						}
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