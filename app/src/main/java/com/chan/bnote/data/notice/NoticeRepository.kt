package com.chan.bnote.data.notice

import android.content.Context
import com.chan.bnote.data.update.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 앱 안 알림(공지사항)을 GitHub 이슈에서 가져오고, 기기에 저장해 둔다.
 *
 * - 가져오기: "공지" 라벨이 붙은 열린 이슈 중 저장소 주인(또는 협업자)이 연 것만. PR은 뺀다.
 * - 저장: 마지막으로 가져온 목록(오프라인에서도 보이게), 읽은 알림, 이 기기에서 삭제한 알림.
 *   기기마다의 기록이라 데이터 내보내기(백업)와 상관없는 별도 SharedPreferences에 둔다(update_check와 같은 이유).
 * - 자동 확인은 앱을 열 때 30분에 한 번까지(GitHub API는 로그인 없이 IP당 시간당 60회까지라 넉넉하다),
 *   그리고 하루 한 번 백그라운드(UpdateCheckWorker)에서. 알림 목록 화면을 열거나 당겨서 새로고침하면 바로 가져온다.
 * - 인터넷이 안 되면 조용히 넘어가고, 저장해 둔 목록을 그대로 보여준다.
 */
object NoticeRepository {

	private const val ISSUES_API = "https://api.github.com/repos/eunchan96/project-BNOTE/issues"
	const val NOTICE_LABEL = "공지"
	private const val PINNED_LABEL = "고정"
	private const val RESOLVED_LABEL = "해결됨"

	private const val PREF_NAME = "notices"
	private const val KEY_CACHE = "cache"
	private const val KEY_READ = "read" // JSON: { "이슈 번호": readSignature }
	private const val KEY_HIDDEN = "hidden_ids"
	private const val KEY_LAST_FETCH_AT = "last_fetch_at"

	private const val AUTO_FETCH_INTERVAL_MS = 30 * 60 * 1000L
	private const val TIMEOUT_MS = 5000

	/** 저장소 주인·멤버·협업자가 쓴 것만 공지로 인정한다(누구나 이슈·댓글을 달 수 있어서). */
	private val TRUSTED_AUTHORS = setOf("OWNER", "MEMBER", "COLLABORATOR")

	private val TARGET_LINE = Regex("^\\s*대상\\s*[:：]\\s*(.+)$")

	private fun prefs(context: Context) =
		context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

	// --- 가져오기 ---

	fun shouldAutoFetch(context: Context): Boolean =
		System.currentTimeMillis() - prefs(context).getLong(
			KEY_LAST_FETCH_AT,
			0L
		) >= AUTO_FETCH_INTERVAL_MS

	/** GitHub에서 공지를 다시 가져와 저장한다. 성공하면 true, 인터넷 문제 등으로 실패하면 false(저장된 목록은 그대로). */
	suspend fun refresh(context: Context): Boolean = withContext(Dispatchers.IO) {
		val label = URLEncoder.encode(NOTICE_LABEL, "UTF-8")
		val url = "$ISSUES_API?labels=$label&state=open&per_page=50&sort=created&direction=desc"
		val body = httpGet(url) ?: return@withContext false
		try {
			val array = JSONArray(body)
			val notices = mutableListOf<Notice>()
			for (i in 0 until array.length()) {
				parseIssue(array.getJSONObject(i))?.let { notices.add(it) }
			}
			prefs(context).edit()
				.putString(KEY_CACHE, JSONArray(notices.map { it.toJson() }).toString())
				.putLong(KEY_LAST_FETCH_AT, System.currentTimeMillis())
				.apply()
			true
		} catch (e: Exception) {
			false
		}
	}

	/** 상세 화면의 "추가 안내" — 그 이슈에 저장소 주인이 단 댓글들(오래된 순). 실패하면 null. */
	suspend fun fetchComments(noticeId: Long): List<NoticeComment>? = withContext(Dispatchers.IO) {
		val body = httpGet("$ISSUES_API/$noticeId/comments?per_page=50") ?: return@withContext null
		try {
			val array = JSONArray(body)
			(0 until array.length()).mapNotNull { i ->
				val obj = array.getJSONObject(i)
				if (obj.optString("author_association") !in TRUSTED_AUTHORS) return@mapNotNull null
				NoticeComment(
					body = NoticeFormatter.stripHtmlComments(obj.optString("body")),
					createdAt = parseIsoTime(obj.optString("created_at"))
				)
			}
		} catch (e: Exception) {
			null
		}
	}

	private fun httpGet(url: String): String? =
		try {
			val connection = (URL(url).openConnection() as HttpURLConnection).apply {
				connectTimeout = TIMEOUT_MS
				readTimeout = TIMEOUT_MS
				setRequestProperty("Accept", "application/vnd.github+json")
				setRequestProperty("User-Agent", "BNOTE-Android")
			}
			try {
				if (connection.responseCode != HttpURLConnection.HTTP_OK) null
				else connection.inputStream.bufferedReader().use { it.readText() }
			} finally {
				connection.disconnect()
			}
		} catch (e: Exception) {
			null
		}

	private fun parseIssue(obj: JSONObject): Notice? {
		if (obj.has("pull_request")) return null
		if (obj.optString("author_association") !in TRUSTED_AUTHORS) return null

		val labelsArray = obj.optJSONArray("labels") ?: JSONArray()
		val labels = (0 until labelsArray.length()).map {
			labelsArray.optJSONObject(it)?.optString("name") ?: labelsArray.optString(it)
		}.toSet()

		// 본문 맨 위쪽의 "대상: 1.13 이하" 줄은 버전 조건으로 읽고, 화면에 보일 본문에서는 뺀다.
		var minVersion: String? = null
		var maxVersion: String? = null
		val bodyLines = mutableListOf<String>()
		// 이슈 템플릿의 작성 안내(<!-- -->)는 GitHub에서도 안 보이므로 앱에서도 뺀다.
		val rawBody = NoticeFormatter.stripHtmlComments(obj.optString("body"))
		for (line in rawBody.lines()) {
			val match = TARGET_LINE.find(line)
			if (match != null && minVersion == null && maxVersion == null) {
				val (min, max) = parseTarget(match.groupValues[1])
				minVersion = min
				maxVersion = max
			} else {
				bodyLines.add(line)
			}
		}

		return Notice(
			id = obj.optLong("number"),
			title = obj.optString("title"),
			body = bodyLines.joinToString("\n").trim(),
			type = NoticeType.fromLabels(labels),
			isPinned = PINNED_LABEL in labels,
			isResolved = RESOLVED_LABEL in labels,
			createdAt = parseIsoTime(obj.optString("created_at")),
			updatedAt = parseIsoTime(obj.optString("updated_at")),
			commentCount = obj.optInt("comments"),
			minVersion = minVersion,
			maxVersion = maxVersion
		)
	}

	/** "1.13 이하" / "1.14 이상" / "1.12 ~ 1.13" / "1.13" → (최소, 최대). 못 읽으면 조건 없음. */
	private fun parseTarget(value: String): Pair<String?, String?> {
		val v = value.trim()
		Regex("^([\\d.]+)\\s*~\\s*([\\d.]+)").find(v)
			?.let { return it.groupValues[1] to it.groupValues[2] }
		Regex("^([\\d.]+)\\s*이하").find(v)?.let { return null to it.groupValues[1] }
		Regex("^([\\d.]+)\\s*이상").find(v)?.let { return it.groupValues[1] to null }
		Regex("^([\\d.]+)$").find(v)?.let { return it.groupValues[1] to it.groupValues[1] }
		return null to null
	}

	private fun parseIsoTime(value: String): Long =
		try {
			SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
				.apply { timeZone = TimeZone.getTimeZone("UTC") }
				.parse(value)?.time ?: 0L
		} catch (e: Exception) {
			0L
		}

	// --- 저장된 목록 ---

	/** 이 기기에서 보여줄 알림 — 삭제한 것과 버전 조건이 안 맞는 것을 빼고, 고정 먼저 그다음 최신순. */
	fun visibleNotices(context: Context): List<Notice> {
		val hidden = hiddenIds(context)
		val current = UpdateChecker.currentVersionName(context)
		return cachedNotices(context)
			.filter { it.id !in hidden && matchesVersion(it, current) }
			.sortedWith(compareByDescending<Notice> { it.isPinned }.thenByDescending { it.createdAt })
	}

	fun find(context: Context, id: Long): Notice? =
		cachedNotices(context).firstOrNull { it.id == id }

	private fun matchesVersion(notice: Notice, current: String): Boolean {
		if (current.isBlank()) return true
		notice.minVersion?.let { if (UpdateChecker.compareVersions(current, it) < 0) return false }
		notice.maxVersion?.let { if (UpdateChecker.compareVersions(current, it) > 0) return false }
		return true
	}

	private fun cachedNotices(context: Context): List<Notice> =
		try {
			val array = JSONArray(prefs(context).getString(KEY_CACHE, "[]"))
			(0 until array.length()).map { noticeFromJson(array.getJSONObject(it)) }
		} catch (e: Exception) {
			emptyList()
		}

	// --- 읽음 / 삭제 ---

	private fun readMap(context: Context): JSONObject =
		try {
			JSONObject(prefs(context).getString(KEY_READ, "{}") ?: "{}")
		} catch (e: Exception) {
			JSONObject()
		}

	fun isRead(context: Context, notice: Notice): Boolean =
		readMap(context).optString(notice.id.toString(), "") == notice.readSignature

	fun hasUnread(context: Context): Boolean = unreadCount(context) > 0

	fun unreadCount(context: Context): Int {
		val read = readMap(context)
		return visibleNotices(context).count {
			read.optString(
				it.id.toString(),
				""
			) != it.readSignature
		}
	}

	fun markRead(context: Context, notices: Collection<Notice>) {
		val read = readMap(context)
		notices.forEach { read.put(it.id.toString(), it.readSignature) }
		prefs(context).edit().putString(KEY_READ, read.toString()).apply()
	}

	fun markAllRead(context: Context) = markRead(context, visibleNotices(context))

	private fun hiddenIds(context: Context): Set<Long> =
		prefs(context).getStringSet(KEY_HIDDEN, emptySet())!!.mapNotNull { it.toLongOrNull() }
			.toSet()

	/** 이 기기에서만 지운다(다시 가져와도 안 보임). 공지 자체를 내리려면 GitHub에서 이슈를 닫는다. */
	fun hide(context: Context, ids: Collection<Long>) {
		val updated = hiddenIds(context).map { it.toString() }.toMutableSet()
		ids.forEach { updated.add(it.toString()) }
		prefs(context).edit().putStringSet(KEY_HIDDEN, updated).apply()
	}

	// --- 저장 형식 ---

	private fun Notice.toJson() = JSONObject().apply {
		put("id", id); put("title", title); put("body", body); put("type", type.name)
		put("pinned", isPinned); put("resolved", isResolved)
		put("createdAt", createdAt); put("updatedAt", updatedAt); put("comments", commentCount)
		minVersion?.let { put("minVersion", it) }
		maxVersion?.let { put("maxVersion", it) }
	}

	private fun noticeFromJson(o: JSONObject) = Notice(
		id = o.getLong("id"),
		title = o.optString("title"),
		body = o.optString("body"),
		type = runCatching { NoticeType.valueOf(o.optString("type")) }.getOrDefault(NoticeType.GENERAL),
		isPinned = o.optBoolean("pinned"),
		isResolved = o.optBoolean("resolved"),
		createdAt = o.optLong("createdAt"),
		updatedAt = o.optLong("updatedAt"),
		commentCount = o.optInt("comments"),
		minVersion = o.optString("minVersion").takeIf { it.isNotBlank() },
		maxVersion = o.optString("maxVersion").takeIf { it.isNotBlank() }
	)
}