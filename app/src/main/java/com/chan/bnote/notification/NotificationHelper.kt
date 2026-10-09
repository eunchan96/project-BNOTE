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
	const val NOTI_ID_UPDATE = 1003

	// 새 버전 알림은 말씀 알림과 따로 끌 수 있도록 채널을 나눈다.
	private const val UPDATE_CHANNEL_ID = "bnote_updates"

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

	/** 앱이 시작될 때 불러서, 알림을 한 번도 안 보냈어도 핸드폰 설정의 알림 목록에 "업데이트 알림"이
	 * 미리 보이게 한다(채널은 만들어져야 설정 화면에 나타난다). */
	fun ensureUpdateChannel(context: Context) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
		val manager = context.getSystemService(NotificationManager::class.java)
		if (manager.getNotificationChannel(UPDATE_CHANNEL_ID) != null) return

		val channel = NotificationChannel(
			UPDATE_CHANNEL_ID, "업데이트 알림", NotificationManager.IMPORTANCE_DEFAULT
		).apply {
			description = "BNOTE 새 버전이 나왔을 때 알려줘요"
		}
		manager.createNotificationChannel(channel)
	}

	/** 새 버전 알림. 누르면 앱이 열리면서 새 버전 안내 창이 뜬다(MainActivity.EXTRA_SHOW_UPDATE). */
	fun showUpdate(context: Context, versionName: String) {
		ensureUpdateChannel(context)

		val openIntent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			putExtra(MainActivity.EXTRA_SHOW_UPDATE, true)
		}
		val pendingIntent = PendingIntent.getActivity(
			context, NOTI_ID_UPDATE, openIntent,
			PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
		)
		val content = "BNOTE $versionName 버전이 나왔어요. 눌러서 업데이트 내역을 확인해 보세요."
		val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
			.setSmallIcon(R.drawable.ic_book_open)
			.setContentTitle("새 버전이 나왔어요")
			.setContentText(content)
			.setStyle(NotificationCompat.BigTextStyle().bigText(content))
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)
			.build()

		if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
			NotificationManagerCompat.from(context).notify(NOTI_ID_UPDATE, notification)
		}
	}

	/** [bookId]/[chapter]가 있으면 탭했을 때 해당 장으로(가능하면 [verse] 절까지), 없으면 그냥 앱을 연다. */
	fun show(
		context: Context,
		notiId: Int,
		title: String,
		content: String,
		bookId: Int? = null,
		chapter: Int? = null,
		verse: Int? = null
	) {
		ensureChannel(context)

		val openIntent = Intent(context, MainActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			if (bookId != null && chapter != null) {
				putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, bookId)
				putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, chapter)
				if (verse != null) putExtra(MainActivity.EXTRA_NAVIGATE_VERSE, verse)
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