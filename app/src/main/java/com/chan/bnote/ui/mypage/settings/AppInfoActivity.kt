package com.chan.bnote.ui.mypage.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R
import com.chan.bnote.data.CrashLogger
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AppInfoActivity : AppCompatActivity() {

	companion object {
		private const val CONTACT_EMAIL = "taegwon02@gmail.com"
		private const val KAKAO_OPEN_CHAT_URL = "https://open.kakao.com/o/sfqUXEEi"
		private const val GITHUB_URL = "https://github.com/eunchan96/project-BNOTE"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_app_info)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_info_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "앱 정보"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		findViewById<TextView>(R.id.text_version).text = buildVersionLabel()

		findViewById<TextView>(R.id.menu_version_history).setOnClickListener {
			startActivity(Intent(this, VersionHistoryActivity::class.java))
		}
		findViewById<TextView>(R.id.menu_seed_reconciliation).setOnClickListener {
			startActivity(Intent(this, SeedReconciliationActivity::class.java))
		}
		findViewById<TextView>(R.id.menu_open_source).setOnClickListener {
			startActivity(Intent(this, OpenSourceLicensesActivity::class.java))
		}
		findViewById<TextView>(R.id.menu_github).setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(GITHUB_URL)))
		}
		findViewById<TextView>(R.id.menu_contact).setOnClickListener { showContactDialog() }
	}

	override fun onResume() {
		super.onResume()
		val count = com.chan.bnote.data.bible.SeedReconciliationReport.count(this)
		val visibility = if (count > 0) android.view.View.VISIBLE else android.view.View.GONE
		findViewById<TextView>(R.id.menu_seed_reconciliation).apply {
			this.visibility = visibility
			text = "본문 수정 확인 필요 (${count}개)"
		}
		findViewById<android.view.View>(R.id.divider_seed_reconciliation).visibility = visibility
	}

	private fun buildVersionLabel(): String {
		return try {
			val packageInfo = packageManager.getPackageInfo(packageName, 0)
			val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				packageInfo.longVersionCode
			} else {
				@Suppress("DEPRECATION")
				packageInfo.versionCode.toLong()
			}
			"버전 ${packageInfo.versionName} ($versionCode)"
		} catch (e: Exception) {
			""
		}
	}

	private fun showContactDialog() {
		val options = arrayOf("이메일로 문의하기", "카카오톡 오픈채팅으로 문의하기")
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("문의하기")
			.setItems(options) { _, which ->
				if (which == 0) openContactEmail() else openKakaoOpenChat()
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun openContactEmail() {
		val logFile = CrashLogger.getLatestLogFile(this)
		val intent = if (logFile != null) {
			val logUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", logFile)
			Intent(Intent.ACTION_SEND).apply {
				type = "text/plain"
				putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
				putExtra(Intent.EXTRA_SUBJECT, "[BNOTE] 문의/피드백")
				putExtra(Intent.EXTRA_TEXT, "최근 오류 기록을 함께 첨부했어요. 어떤 상황이었는지도 같이 적어주시면 도움이 많이 돼요!")
				putExtra(Intent.EXTRA_STREAM, logUri)
				addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
			}
		} else {
			Intent(Intent.ACTION_SENDTO).apply {
				data = Uri.parse("mailto:")
				putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
				putExtra(Intent.EXTRA_SUBJECT, "[BNOTE] 문의/피드백")
			}
		}
		try {
			startActivity(Intent.createChooser(intent, "메일 앱 선택"))
		} catch (e: Exception) {
			Toast.makeText(this, "메일 앱을 찾을 수 없어요. $CONTACT_EMAIL 로 연락해주세요", Toast.LENGTH_LONG)
				.show()
		}
	}

	private fun openKakaoOpenChat() {
		val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KAKAO_OPEN_CHAT_URL))
		try {
			startActivity(intent)
		} catch (e: Exception) {
			Toast.makeText(this, "링크를 열 수 없어요", Toast.LENGTH_SHORT).show()
		}
	}
}