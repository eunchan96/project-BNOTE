package com.chan.bnote.data.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chan.bnote.notification.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * 앱을 열지 않아도 하루에 한 번(인터넷이 연결돼 있을 때) 새 버전을 확인해서, 새 버전이 있으면 알림을 보낸다.
 * 같은 버전으로는 알림을 한 번만 보낸다 — 알림을 놓친 사용자는 앱을 열 때 뜨는 안내 창이 받쳐준다.
 *
 * 정확한 시각이 필요 없는 작업이라 WorkManager 주기 작업으로 충분하다(기기가 절전 중이면 몇 시간 늦어질 수 있다).
 * 알림 권한이 없는 사용자에게는 알림이 가지 않는다(NotificationHelper가 조용히 건너뜀).
 */
class UpdateCheckWorker(
	context: Context,
	params: WorkerParameters
) : CoroutineWorker(context, params) {

	companion object {
		private const val WORK_NAME = "bnote_update_check"

		/** 앱이 시작될 때마다 불러도 된다 — 이미 예약돼 있으면 그대로 둔다(KEEP). */
		fun schedule(context: Context) {
			val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
				.setConstraints(
					Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
				)
				.build()
			WorkManager.getInstance(context).enqueueUniquePeriodicWork(
				WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
			)
		}
	}

	override suspend fun doWork(): Result {
		val context = applicationContext
		// 앱을 열 때의 자동 확인(하루 한 번)과는 따로 센다 — 여기서 기록을 남기면 앱을 열었을 때 안내 창이 안 뜰 수 있다.
		val release = UpdateChecker.fetchLatest(context, recordCheck = false)
			?: return Result.success()
		if (!UpdateChecker.isNewer(context, release)) return Result.success()
		if (UpdateChecker.wasNotified(context, release)) return Result.success()

		NotificationHelper.showUpdate(context, release.versionName)
		UpdateChecker.markNotified(context, release)
		return Result.success()
	}
}