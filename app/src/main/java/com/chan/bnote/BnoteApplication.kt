package com.chan.bnote

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.chan.bnote.data.CrashLogger
import com.chan.bnote.widget.WidgetUpdater

class BnoteApplication : Application() {

	// 화면에 보이는(started) 액티비티 수. 0이 되면 앱 전체가 화면에서 사라진 것(홈으로 나감 등)이다.
	private var startedActivityCount = 0

	override fun onCreate() {
		super.onCreate()
		CrashLogger.install(this)
		refreshWidgetsWhenAppGoesBackground()
	}

	/**
	 * 앱 안에서 바뀐 것(암송 구절 추가/삭제, 데이터 불러오기, 첫 실행 뒤 성경 데이터 준비 등)이
	 * 홈 화면 위젯에 반영되도록, 앱이 화면에서 사라지는 순간 위젯을 한 번 다시 그린다. 암송 구절을 고치는
	 * 곳이 여러 화면이라 각각에서 위젯을 부르는 대신 여기서 한 번에 처리한다.
	 */
	private fun refreshWidgetsWhenAppGoesBackground() {
		registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
			override fun onActivityStarted(activity: Activity) {
				startedActivityCount++
			}

			override fun onActivityStopped(activity: Activity) {
				startedActivityCount--
				// 화면 회전·다크모드 전환처럼 액티비티가 다시 만들어지는 중일 땐 건너뛴다.
				if (startedActivityCount == 0 && !activity.isChangingConfigurations) {
					WidgetUpdater.requestUpdateAll(applicationContext)
				}
			}

			override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
			override fun onActivityResumed(activity: Activity) {}
			override fun onActivityPaused(activity: Activity) {}
			override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
			override fun onActivityDestroyed(activity: Activity) {}
		})
	}
}