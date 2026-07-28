package com.chan.bnote.ui.mypage.settings

import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

class OpenSourceLicensesActivity : AppCompatActivity() {

	private data class LibraryLicense(val name: String, val license: String)

	private val libraries = listOf(
		LibraryLicense("AndroidX Core KTX", "Apache License 2.0"),
		LibraryLicense("AndroidX AppCompat", "Apache License 2.0"),
		LibraryLicense("Material Components for Android", "Apache License 2.0"),
		LibraryLicense("AndroidX Activity", "Apache License 2.0"),
		LibraryLicense("AndroidX ConstraintLayout", "Apache License 2.0"),
		LibraryLicense("AndroidX Core SplashScreen", "Apache License 2.0"),
		LibraryLicense("AndroidX Room", "Apache License 2.0"),
		LibraryLicense("AndroidX RecyclerView", "Apache License 2.0"),
		LibraryLicense("AndroidX ViewPager2", "Apache License 2.0"),
		LibraryLicense("AndroidX Lifecycle", "Apache License 2.0"),
		LibraryLicense("AndroidX WorkManager", "Apache License 2.0"),
		LibraryLicense("Flexbox for Android (google/flexbox-layout)", "Apache License 2.0"),
		LibraryLicense("Coil", "Apache License 2.0")
	)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_open_source_licenses)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.open_source_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "오픈소스 라이선스"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val container = findViewById<LinearLayout>(R.id.container_licenses)
		for (lib in libraries) {
			addRow(container, lib)
		}
	}

	private fun addRow(container: LinearLayout, lib: LibraryLicense) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(14), dp(16), dp(14))
		}
		val nameView = TextView(this).apply {
			text = lib.name
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(
				ContextCompat.getColor(
					this@OpenSourceLicensesActivity,
					R.color.text_primary
				)
			)
		}
		val licenseView = TextView(this).apply {
			text = lib.license
			textSize = 13f
			setTextColor(
				ContextCompat.getColor(
					this@OpenSourceLicensesActivity,
					R.color.text_secondary
				)
			)
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(nameView)
		row.addView(licenseView)
		container.addView(row)

		val divider = android.view.View(this).apply {
			layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
			setBackgroundColor(
				ContextCompat.getColor(
					this@OpenSourceLicensesActivity,
					R.color.divider_light
				)
			)
		}
		container.addView(divider)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}