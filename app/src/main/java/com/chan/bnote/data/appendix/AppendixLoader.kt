package com.chan.bnote.data.appendix

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 부록(주기도문/사도신경/십계명/교독문) 콘텐츠 로더
 */
object AppendixLoader {

	private const val DIR = "appendix"

	private var lordsPrayerCache: VersionedTextContent? = null
	private var apostlesCreedCache: VersionedTextContent? = null
	private var tenCommandmentsCache: TenCommandmentsContent? = null
	private var responsiveReadingsCache: List<ResponsiveReading>? = null

	fun loadLordsPrayer(context: Context): VersionedTextContent {
		lordsPrayerCache?.let { return it }
		val json = readAsset(context, "lords_prayer.json")
		val content = parseVersionedTextContent(JSONObject(json))
		lordsPrayerCache = content
		return content
	}

	fun loadApostlesCreed(context: Context): VersionedTextContent {
		apostlesCreedCache?.let { return it }
		val json = readAsset(context, "apostles_creed.json")
		val content = parseVersionedTextContent(JSONObject(json))
		apostlesCreedCache = content
		return content
	}

	fun loadTenCommandments(context: Context): TenCommandmentsContent {
		tenCommandmentsCache?.let { return it }
		val json = readAsset(context, "ten_commandments.json")
		val obj = JSONObject(json)

		val intro = obj.getJSONArray("intro").let { arr ->
			(0 until arr.length()).map { arr.getString(it) }
		}

		val commandments = obj.getJSONArray("commandments").let { arr ->
			(0 until arr.length()).map { i ->
				val item = arr.getJSONObject(i)
				CommandmentItem(
					number = item.getInt("number"),
					text = item.getString("text")
				)
			}
		}

		val summaryObj = obj.getJSONObject("summary")
		val summary = CommandmentSummary(
			text = summaryObj.getString("text"),
			reference = summaryObj.getString("reference")
		)

		val content = TenCommandmentsContent(
			title = obj.getString("title"),
			intro = intro,
			commandments = commandments,
			reference = obj.getString("reference"),
			summary = summary
		)
		tenCommandmentsCache = content
		return content
	}

	fun loadResponsiveReadings(context: Context): List<ResponsiveReading> {
		responsiveReadingsCache?.let { return it }
		val json = readAsset(context, "responsive_readings.json")
		val array = JSONArray(json)

		val readings = (0 until array.length()).map { i ->
			val obj = array.getJSONObject(i)
			val linesArray = obj.getJSONArray("lines")
			val lines = (0 until linesArray.length()).map { j ->
				val lineObj = linesArray.getJSONObject(j)
				ResponsiveReadingLine(
					speaker = ReadingSpeaker.fromJson(lineObj.getString("speaker")),
					text = lineObj.getString("text")
				)
			}
			ResponsiveReading(
				number = obj.getInt("number"),
				title = obj.getString("title"),
				lines = lines
			)
		}
		responsiveReadingsCache = readings
		return readings
	}

	private fun parseVersionedTextContent(obj: JSONObject): VersionedTextContent {
		val versionsArray = obj.getJSONArray("versions")
		val versions = (0 until versionsArray.length()).map { i ->
			val versionObj = versionsArray.getJSONObject(i)
			val linesArray = versionObj.getJSONArray("lines")
			val lines = (0 until linesArray.length()).map { j -> linesArray.getString(j) }
			TextVersion(
				id = versionObj.getString("id"),
				label = versionObj.getString("label"),
				lines = lines
			)
		}
		return VersionedTextContent(
			title = obj.getString("title"),
			versions = versions
		)
	}

	private fun readAsset(context: Context, fileName: String): String {
		return context.assets.open("$DIR/$fileName")
			.bufferedReader(Charsets.UTF_8)
			.use { it.readText() }
	}
}