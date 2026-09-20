package com.chan.bnote

import android.app.Application
import com.chan.bnote.data.CrashLogger

class BnoteApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		CrashLogger.install(this)
	}
}