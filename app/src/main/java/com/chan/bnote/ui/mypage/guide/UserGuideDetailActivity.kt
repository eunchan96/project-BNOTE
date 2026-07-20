package com.chan.bnote.ui.mypage.guide

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

class UserGuideDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY_ID = "extra_category_id"
		private const val EXTRA_HIGHLIGHT_TITLE = "extra_highlight_title"

		fun createIntent(context: Context, categoryId: String, highlightTitle: String?): Intent {
			return Intent(context, UserGuideDetailActivity::class.java).apply {
				putExtra(EXTRA_CATEGORY_ID, categoryId)
				if (highlightTitle != null) putExtra(EXTRA_HIGHLIGHT_TITLE, highlightTitle)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_user_guide_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_guide_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
		val highlightTitle = intent.getStringExtra(EXTRA_HIGHLIGHT_TITLE)
		val category = categoryId?.let { UserGuideContent.findCategory(it) }

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.text_top_bar_title).text = category?.name ?: "사용 가이드"

		if (category == null) {
			finish()
			return
		}

		val container = findViewById<LinearLayout>(R.id.container_guide_detail)
		val itemViews = mutableMapOf<String, LinearLayout>()

		for (item in category.items) {
			val itemContainer = LinearLayout(this).apply {
				orientation = LinearLayout.VERTICAL
				setPadding(dp(4), dp(8), dp(4), dp(20))
			}
			val titleView = TextView(this).apply {
				text = item.title
				textSize = 15f
				setTypeface(typeface, Typeface.BOLD)
				setTextColor(
					ContextCompat.getColor(
						this@UserGuideDetailActivity,
						R.color.brown_primary
					)
				)
			}
			val descView = TextView(this).apply {
				text = item.description
				textSize = 14f
				setTextColor(
					ContextCompat.getColor(
						this@UserGuideDetailActivity,
						R.color.text_primary
					)
				)
				setPadding(0, dp(6), 0, 0)
				setLineSpacing(dp(2).toFloat(), 1f)
			}
			itemContainer.addView(titleView)
			itemContainer.addView(descView)
			container.addView(itemContainer)
			itemViews[item.title] = itemContainer
		}

		if (highlightTitle != null) {
			val target = itemViews[highlightTitle]
			if (target != null) {
				val scrollView = findViewById<ScrollView>(R.id.scroll_guide_detail)
				scrollView.post {
					scrollView.smoothScrollTo(0, target.top)
					flashHighlight(target)
				}
			}
		}
	}

	/** 검색으로 찾아온 항목이 어떤 건지 한눈에 보이도록 배경을 잠깐 노랗게 깜빡인다. */
	private fun flashHighlight(view: LinearLayout) {
		val highlightColor = Color.parseColor("#FFF3C4")
		val animator = ValueAnimator.ofObject(
			android.animation.ArgbEvaluator(),
			highlightColor,
			Color.TRANSPARENT
		)
		animator.duration = 1200
		animator.interpolator = LinearInterpolator()
		animator.addUpdateListener { animation ->
			view.setBackgroundColor(animation.animatedValue as Int)
		}
		animator.start()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}