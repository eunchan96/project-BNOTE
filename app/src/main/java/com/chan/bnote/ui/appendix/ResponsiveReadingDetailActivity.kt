package com.chan.bnote.ui.appendix

import android.content.Context
import android.content.Intent
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
import com.chan.bnote.data.appendix.AppendixLoader
import com.chan.bnote.data.appendix.ReadingSpeaker

/**
 * 교독문 한 항목의 상세 화면.
 * speaker가 CONGREGATION 또는 UNISON이면 굵게, LEADER는 일반체로 표시한다.
 */
class ResponsiveReadingDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_NUMBER = "extra_reading_number"

		fun start(context: Context, readingNumber: Int) {
			val intent = Intent(context, ResponsiveReadingDetailActivity::class.java)
			intent.putExtra(EXTRA_NUMBER, readingNumber)
			context.startActivity(intent)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_responsive_reading_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.responsive_reading_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val readingNumber = intent.getIntExtra(EXTRA_NUMBER, 1)
		val reading = AppendixLoader.loadResponsiveReadings(this)
			.first { it.number == readingNumber }

		findViewById<TextView>(R.id.text_top_bar_title).text = "${reading.number}. ${reading.title}"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val container = findViewById<LinearLayout>(R.id.container_reading_lines)
		reading.lines.forEach { line ->
			val isBold = line.speaker == ReadingSpeaker.CONGREGATION ||
					line.speaker == ReadingSpeaker.UNISON

			val lineView = TextView(this).apply {
				text = line.text
				textSize = 16f
				setTextColor(
					ContextCompat.getColor(
						this@ResponsiveReadingDetailActivity,
						R.color.text_primary
					)
				)
				setTypeface(
					null,
					if (isBold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
				)
				setLineSpacing(dp(4).toFloat(), 1f)
				setPadding(0, 0, 0, dp(10))
			}
			container.addView(lineView)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}