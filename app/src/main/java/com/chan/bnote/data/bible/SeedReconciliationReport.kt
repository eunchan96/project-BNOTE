package com.chan.bnote.data.bible

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 성경 본문 재시딩 중, 단어 메모 · 부분 하이라이트가 가리키던 글자를 새 본문에서 못 찾은 경우를
 * 기록해둔다. 개발자 로그(Logcat)뿐 아니라, 앱 정보 화면에서 사용자가 직접 확인할 수 있게 하기 위함.
 */
object SeedReconciliationReport {

	private const val PREFS_NAME = "bible_seed_prefs"
	private const val KEY_ISSUES = "reconciliation_issues"

	data class Issue(
		val type: String, // "word_memo" 또는 "highlight"
		val bookId: Int,
		val chapter: Int,
		val verse: Int,
		val oldSnippet: String,
		val memoText: String?, // 단어 메모일 때만
		val recordedAt: Long
	)

	/** 이번 재시딩에서 새로 발견된 문제 목록으로 통째로 교체한다(이전 것들은 이미 해결됐을 수도 있어서). */
	fun replaceAll(context: Context, issues: List<Issue>) {
		val array = JSONArray()
		for (issue in issues) {
			array.put(
				JSONObject().apply {
					put("type", issue.type)
					put("bookId", issue.bookId)
					put("chapter", issue.chapter)
					put("verse", issue.verse)
					put("oldSnippet", issue.oldSnippet)
					issue.memoText?.let { put("memoText", it) }
					put("recordedAt", issue.recordedAt)
				}
			)
		}
		prefs(context).edit().putString(KEY_ISSUES, array.toString()).apply()
	}

	fun getAll(context: Context): List<Issue> {
		val raw = prefs(context).getString(KEY_ISSUES, null) ?: return emptyList()
		return try {
			val array = JSONArray(raw)
			(0 until array.length()).map { i ->
				val obj = array.getJSONObject(i)
				Issue(
					type = obj.getString("type"),
					bookId = obj.getInt("bookId"),
					chapter = obj.getInt("chapter"),
					verse = obj.getInt("verse"),
					oldSnippet = obj.getString("oldSnippet"),
					memoText = if (obj.has("memoText")) obj.getString("memoText") else null,
					recordedAt = obj.optLong("recordedAt", 0L)
				)
			}
		} catch (e: Exception) {
			emptyList()
		}
	}

	/** 사용자가 확인했으면 목록에서 하나 지운다. */
	fun dismiss(context: Context, issue: Issue) {
		val remaining = getAll(context).filterNot {
			it.type == issue.type && it.bookId == issue.bookId && it.chapter == issue.chapter &&
					it.verse == issue.verse && it.oldSnippet == issue.oldSnippet && it.recordedAt == issue.recordedAt
		}
		replaceAll(context, remaining)
	}

	fun count(context: Context): Int = getAll(context).size

	private fun prefs(context: Context) =
		context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}