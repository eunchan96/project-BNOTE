package com.chan.bnote.data

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 크래시가 나면 스택트레이스를 앱 내부 저장소 파일로 남겨둔다.
 * 폰이 컴퓨터에 연결 안 된 상태에서 터진 크래시도, 나중에 "문의하기"로 로그를 첨부해 보낼 수 있게 하는 게 목적.
 */
object CrashLogger {

	private const val DIR_NAME = "crash_logs"
	private const val MAX_LOG_FILES = 10

	fun install(context: Context) {
		val appContext = context.applicationContext
		val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

		Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
			try {
				writeLog(appContext, throwable)
			} catch (e: Exception) {
				// 로그 저장 자체가 실패해도 원래 크래시 처리는 막지 않는다.
			}
			// 로그만 남기고, 원래 시스템 크래시 처리(앱 종료 등)는 그대로 진행되게 한다.
			defaultHandler?.uncaughtException(thread, throwable)
		}
	}

	private fun writeLog(context: Context, throwable: Throwable) {
		val dir = File(context.filesDir, DIR_NAME)
		if (!dir.exists()) dir.mkdirs()

		val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.KOREA).format(Date())
		val file = File(dir, "crash_$timestamp.txt")

		val stringWriter = StringWriter()
		throwable.printStackTrace(PrintWriter(stringWriter))
		file.writeText(
			"발생 시각: ${
				SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss",
					Locale.KOREA
				).format(Date())
			}\n\n$stringWriter"
		)

		// 오래된 로그는 정리 (최근 MAX_LOG_FILES개만 유지)
		val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
		if (files.size > MAX_LOG_FILES) {
			files.drop(MAX_LOG_FILES).forEach { it.delete() }
		}
	}

	/** 가장 최근 크래시 로그 파일. 없으면 null. */
	fun getLatestLogFile(context: Context): File? {
		val dir = File(context.applicationContext.filesDir, DIR_NAME)
		return dir.listFiles()?.maxByOrNull { it.lastModified() }
	}

	fun hasLogs(context: Context): Boolean = getLatestLogFile(context) != null
}