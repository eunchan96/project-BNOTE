package com.chan.bnote.ui.appendix

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
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

class TenCommandmentsActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_ten_commandments)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ten_commandments_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val content = AppendixLoader.loadTenCommandments(this)

		findViewById<TextView>(R.id.text_top_bar_title).text = content.title
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val container = findViewById<LinearLayout>(R.id.container_commandments)

		content.intro.forEach { line ->
			container.addView(textView(line, textSize = 15f, bottomMargin = 4))
		}

		container.addView(spacer(12))

		content.commandments.forEach { item ->
			val line = TextView(this).apply {
				text = buildOrdinalPrefixSpan(item.text)
				textSize = 16f
				setTextColor(
					ContextCompat.getColor(
						this@TenCommandmentsActivity,
						R.color.text_primary
					)
				)
				setLineSpacing(dp(3).toFloat(), 1f)
				setPadding(0, 0, 0, dp(10))
			}
			container.addView(line)
		}

		container.addView(
			textView(
				"(${content.reference})",
				textSize = 13f,
				isHint = true,
				bottomMargin = 16
			)
		)

		container.addView(divider())
		container.addView(spacer(12))

		container.addView(textView(content.summary.text, textSize = 15f, bottomMargin = 4))
		container.addView(textView("(${content.summary.reference})", textSize = 13f, isHint = true))
	}

	private fun textView(
		text: String,
		textSize: Float,
		isHint: Boolean = false,
		bottomMargin: Int = 0
	): TextView {
		return TextView(this).apply {
			this.text = text
			this.textSize = textSize
			setTextColor(
				ContextCompat.getColor(
					this@TenCommandmentsActivity,
					if (isHint) R.color.text_hint else R.color.text_primary
				)
			)
			setLineSpacing(dp(3).toFloat(), 1f)
			setPadding(0, 0, 0, dp(bottomMargin))
		}
	}

	private fun divider(): View {
		return View(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
			)
			setBackgroundColor(
				ContextCompat.getColor(
					this@TenCommandmentsActivity,
					R.color.divider_light
				)
			)
		}
	}

	private fun spacer(heightDp: Int): View {
		return View(this).apply {
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)
			)
		}
	}

	/**
	 * "제일은," "제이는," 처럼 앞 4글자(서수 표현 + 콤마)만 브라운 색으로 강조한다.
	 */
	private fun buildOrdinalPrefixSpan(text: String): SpannableString {
		val prefixLength = 4.coerceAtMost(text.length)
		return SpannableString(text).apply {
			setSpan(
				ForegroundColorSpan(
					ContextCompat.getColor(
						this@TenCommandmentsActivity,
						R.color.brown_primary
					)
				),
				0,
				prefixLength,
				Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
			)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}