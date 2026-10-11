package com.chan.bnote.data.mypage

import android.content.Context
import com.chan.bnote.data.BibleDatabase

/**
 * 기본으로 제공하는 복사 형식 3개("묵상용" / "짧게 (창 1:1)" / "길게 (창세기 1장 1절)").
 * 처음 한 번만 심고, 그 뒤로는 자유롭게 수정 · 삭제할 수 있는 평범한 프리셋이다(다시 자동으로 채워지지 않는다).
 *
 * 복사 형식 선택 창(CopyFormatPickerBottomSheet)과 데이터 불러오기(BackupManager)가 함께 쓴다.
 * 불러오기는 백업의 복사 형식을 그대로 가져오고(기본 형식을 지웠던 사람은 지운 그대로), "이미 심었음"으로
 * 표시해서 나중에 선택 창을 열 때 기본 형식이 또 심겨 두 번씩 생기지 않게 한다.
 */
object CopyFormatDefaults {

	private const val PREF_NAME = "copy_format_prefs"
	private const val KEY_SEEDED = "default_presets_seeded"
	private const val KEY_RENAMED_V2 = "default_presets_renamed_v2"

	/**
	 * - 묵상용: 참조를 괄호 없이 구절 위에 얹고, 절 번호를 붙여 여러 절을 죽 읽기 좋게 만든다.
	 * - 짧게: "(창 1:1) 본문"처럼 참조를 짧게 줄여 구절 앞에 붙인다.
	 * - 길게: 짧게와 같은 모양이되, 참조만 "창세기 1장 1절"처럼 풀어 쓴다.
	 */
	fun presets(): List<Pair<String, CopyFormatConfig>> = listOf(
		"묵상용" to CopyFormatConfig.meditationDefault(),
		"짧게 (창 1:1)" to CopyFormatConfig(
			refVerseSeparator = CopyFormatConfig.Separator.SPACE,
			multiVerseSeparator = CopyFormatConfig.Separator.SPACE,
			refPosition = CopyFormatConfig.RefPosition.BEFORE,
			refLength = CopyFormatConfig.RefLength.SHORT,
			refSpacing = true,
			refBracket = CopyFormatConfig.RefBracket.PAREN,
			showVerseNumberWhenMulti = false
		),
		"길게 (창세기 1장 1절)" to CopyFormatConfig(
			refVerseSeparator = CopyFormatConfig.Separator.SPACE,
			multiVerseSeparator = CopyFormatConfig.Separator.SPACE,
			refPosition = CopyFormatConfig.RefPosition.BEFORE,
			refLength = CopyFormatConfig.RefLength.LONG,
			refSpacing = true,
			refBracket = CopyFormatConfig.RefBracket.PAREN,
			showVerseNumberWhenMulti = false
		)
	)

	/** 아직 한 번도 심지 않았으면 기본 형식을 심는다. 예전에 "기본1/2/3"으로 심긴 기기는 이름 · 설정을 한 번만 바꿔준다. */
	suspend fun seedIfNeeded(context: Context, db: BibleDatabase) {
		val prefs = prefs(context)
		if (!prefs.getBoolean(KEY_SEEDED, false)) {
			for ((name, cfg) in presets()) {
				db.copyFormatPresetDao()
					.insert(CopyFormatPreset(name = name, configJson = cfg.toJson()))
			}
			markSeeded(context)
			return
		}

		// 이미 예전에 "기본1/기본2/기본3"으로 한 번 심어졌던 기기라면, 이름과 설정을 딱 한 번만 새로 바꿔준다.
		// 그 사이 사용자가 이름을 직접 바꿨거나 지운 프리셋은(이름이 더 이상 "기본1" 등이 아니므로) 건드리지 않는다.
		if (prefs.getBoolean(KEY_RENAMED_V2, false)) return
		val defaults = presets()
		val renameMap = mapOf("기본1" to defaults[0], "기본2" to defaults[1], "기본3" to defaults[2])
		for (preset in db.copyFormatPresetDao().getAll()) {
			val replacement = renameMap[preset.name] ?: continue
			db.copyFormatPresetDao().update(
				preset.copy(name = replacement.first, configJson = replacement.second.toJson())
			)
		}
		prefs.edit().putBoolean(KEY_RENAMED_V2, true).apply()
	}

	/**
	 * 데이터 불러오기 뒤 호출한다. 백업에 복사 형식 목록이 들어 있었다면(비어 있어도) 그 내용이 곧 사용자의
	 * 복사 형식이므로, 기본 형식을 다시 심지 않도록 표시만 해 둔다. 복사 형식 기능이 생기기 전의 옛날 백업처럼
	 * 목록 자체가 없으면 표시하지 않아서, 선택 창을 처음 열 때 평소처럼 기본 형식이 심긴다.
	 */
	fun onRestored(context: Context, backupHadPresets: Boolean) {
		if (backupHadPresets) markSeeded(context)
	}

	private fun markSeeded(context: Context) {
		prefs(context).edit()
			.putBoolean(KEY_SEEDED, true)
			.putBoolean(KEY_RENAMED_V2, true)
			.apply()
	}

	private fun prefs(context: Context) =
		context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}