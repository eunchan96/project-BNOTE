package com.chan.bnote.data.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 새 버전이 나왔는지 확인한다. BNOTE는 플레이스토어가 아니라 APK로 배포해서 업데이트 알림이 따로
 * 오지 않으므로, GitHub 릴리스의 최신 버전(태그 예: v1.13)을 지금 설치된 버전과 비교한다.
 *
 * - 앱을 열 때 자동으로 확인하는 건 하루에 한 번까지만(shouldAutoCheck).
 * - 새 버전 안내 창에서 "나중에"를 누르면 그 버전은 며칠 동안 다시 묻지 않는다(snooze).
 * - 앱을 열지 않아도 하루에 한 번 백그라운드에서 확인해서, 새 버전마다 한 번씩 알림을 보낸다(UpdateCheckWorker).
 * - 인터넷이 안 되거나 GitHub에 접속하지 못하면 조용히 넘어간다(null).
 *
 * 설정 값은 기기마다의 기록이라 데이터 내보내기(백업)와 상관없는 별도 SharedPreferences에 둔다.
 */
object UpdateChecker {

	private const val LATEST_RELEASE_API =
		"https://api.github.com/repos/eunchan96/project-BNOTE/releases/latest"

	/** 업데이트 소식을 알리는 공지용 오픈채팅방(자유 문의도 가능). 새 버전 APK도 여기서 안내한다. */
	const val NOTICE_OPEN_CHAT_URL = "https://open.kakao.com/o/gvthTBRi"

	private const val PREF_NAME = "update_check"
	private const val KEY_LAST_CHECK_AT = "last_check_at"
	private const val KEY_SNOOZED_TAG = "snoozed_tag"
	private const val KEY_SNOOZED_UNTIL = "snoozed_until"
	private const val KEY_NOTIFIED_TAG = "notified_tag"

	private const val AUTO_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
	private const val SNOOZE_MS = 3 * 24 * 60 * 60 * 1000L
	private const val TIMEOUT_MS = 5000

	/** GitHub 최신 릴리스. [notes]는 릴리스 노트에서 제목(#) 줄을 뺀 업데이트 내역. */
	data class Release(val tag: String, val versionName: String, val notes: String)

	/** 지금 설치된 앱의 버전 이름(예: "1.13"). */
	fun currentVersionName(context: Context): String =
		try {
			context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
		} catch (e: Exception) {
			""
		}

	/** 앱을 열 때 자동으로 확인해도 되는지(마지막 확인 후 하루가 지났는지). */
	fun shouldAutoCheck(context: Context): Boolean {
		val last = prefs(context).getLong(KEY_LAST_CHECK_AT, 0L)
		return System.currentTimeMillis() - last >= AUTO_CHECK_INTERVAL_MS
	}

	/** GitHub에서 최신 릴리스를 가져온다. 실패하면 null. [recordCheck]이면 성공했을 때 마지막 확인
	 * 시각을 기록한다(앱을 열 때의 하루 한 번 자동 확인용 — 백그라운드 확인은 기록하지 않는다). */
	suspend fun fetchLatest(
		context: Context,
		recordCheck: Boolean = true
	): Release? = withContext(Dispatchers.IO) {
		try {
			val connection = (URL(LATEST_RELEASE_API).openConnection() as HttpURLConnection).apply {
				connectTimeout = TIMEOUT_MS
				readTimeout = TIMEOUT_MS
				setRequestProperty("Accept", "application/vnd.github+json")
				setRequestProperty("User-Agent", "BNOTE-Android")
			}
			try {
				if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
				val body = connection.inputStream.bufferedReader().use { it.readText() }
				val json = JSONObject(body)
				val tag = json.optString("tag_name").takeIf { it.isNotBlank() }
					?: return@withContext null
				if (recordCheck) {
					prefs(context).edit()
						.putLong(KEY_LAST_CHECK_AT, System.currentTimeMillis())
						.apply()
				}
				Release(
					tag = tag,
					versionName = tag.removePrefix("v").removePrefix("V"),
					notes = cleanNotes(json.optString("body"))
				)
			} finally {
				connection.disconnect()
			}
		} catch (e: Exception) {
			null
		}
	}

	/** [release]가 지금 설치된 버전보다 새 버전인지. "1.9" < "1.10"처럼 숫자 단위로 비교한다. */
	fun isNewer(context: Context, release: Release): Boolean =
		compareVersions(release.versionName, currentVersionName(context)) > 0

	/** "나중에"로 미뤄둔 버전이면 미룬 기간 동안은 자동으로 다시 묻지 않는다. */
	fun isSnoozed(context: Context, release: Release): Boolean {
		val p = prefs(context)
		return p.getString(KEY_SNOOZED_TAG, null) == release.tag &&
				System.currentTimeMillis() < p.getLong(KEY_SNOOZED_UNTIL, 0L)
	}

	fun snooze(context: Context, release: Release) {
		prefs(context).edit()
			.putString(KEY_SNOOZED_TAG, release.tag)
			.putLong(KEY_SNOOZED_UNTIL, System.currentTimeMillis() + SNOOZE_MS)
			.apply()
	}

	/** 이 버전으로 이미 백그라운드 알림을 보냈는지(새 버전 하나당 알림은 한 번만). */
	fun wasNotified(context: Context, release: Release): Boolean =
		prefs(context).getString(KEY_NOTIFIED_TAG, null) == release.tag

	fun markNotified(context: Context, release: Release) {
		prefs(context).edit().putString(KEY_NOTIFIED_TAG, release.tag).apply()
	}

	/** "1.9" < "1.10"처럼 숫자 단위로 버전을 비교한다(a가 크면 양수). 알림의 버전 조건에서도 쓴다. */
	fun compareVersions(a: String, b: String): Int {
		val pa = a.split('.').map { it.trim().toIntOrNull() ?: 0 }
		val pb = b.split('.').map { it.trim().toIntOrNull() ?: 0 }
		for (i in 0 until maxOf(pa.size, pb.size)) {
			val diff = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
			if (diff != 0) return diff
		}
		return 0
	}

	/** 릴리스 노트(마크다운)에서 "## 업데이트 내역" 같은 제목 줄은 빼고, "- " 목록은 "・ "로 바꾼다. */
	private fun cleanNotes(body: String): String =
		body.lines()
			.map { it.trim() }
			.filter { it.isNotEmpty() && !it.startsWith("#") }
			.joinToString("\n") { line ->
				if (line.startsWith("- ") || line.startsWith("* ")) "・ " + line.drop(2) else line
			}

	private fun prefs(context: Context) =
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}