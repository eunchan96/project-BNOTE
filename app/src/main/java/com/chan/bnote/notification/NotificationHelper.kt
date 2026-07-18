package com.chan.bnote.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chan.bnote.MainActivity
import com.chan.bnote.R

object NotificationHelper {

	private const val CHANNEL_ID = "bnote_reminders"

	const val NOTI_ID_DAILY_VERSE = 1001
	const val NOTI_ID_READING_REMINDER = 1002

	fun ensureChannel(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val manager = context.getSystemService(NotificationManager::class.java)
		if (manager.getNotificationChannel(CHANNEL_ID) != null) return

		val channel = NotificationChannel(
			CHANNEL_ID, "말씀 알림", NotificationManager.IMPORTANCE_DEFAULT
		).apply {
			description = "매일 말씀 알림, 통독 리마인더"
		}
		manager.createNotificationChannel(channel)
	}

	/** [bookId]/[chapter]가 있으면 탭했을 때 해당 장으로, 없으면 그냥 앱을 연다. */
	fun show(
		context: Context,
		notiId: Int,
		title: String,
		content: String,
		bookId: Int? = null,
		chapter: Int? = null
	) {
		ensureChannel(context)

		val openIntent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			if (bookId != null && chapter != null) {
				putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
				putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
			}
		}
		val pendingIntent = PendingIntent.getActivity(
			context, notiId, openIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)

		val notification = NotificationCompat.Builder(context, CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_book_open)
			.setContentTitle(title)
			.setContentText(content)
			.setStyle(NotificationCompat.BigTextStyle().bigText(content))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.build()

		// 알림 권한이 없는 상태(사용자가 나중에 거부)에서 호출돼도 앱이 죽지 않도록 방어.
		if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
			NotificationManagerCompat.from(context).notify(notiId, notification)
		}
	}
}