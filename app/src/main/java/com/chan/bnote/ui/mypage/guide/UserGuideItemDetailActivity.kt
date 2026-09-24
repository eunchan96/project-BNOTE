package com.chan.bnote.ui.mypage.guide

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

/** 가이드 항목 하나(GuideItem)만 따로 보여주는 화면. 예전에는 카테고리 안 항목을 전부 한 화면에
 * 이어 붙여서 보여줬는데, 사진이 붙으면서 한 카테고리에 20장 넘게 쌓여 너무 길고 복잡해졌다.
 * 이제 카테고리를 누르면 항목 "목록"만 보여주고(UserGuideDetailActivity), 항목 하나를 또
 * 누르면 이 화면으로 넘어와 그 항목만 본다. */
class UserGuideItemDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY_ID = "extra_category_id"
		private const val EXTRA_ITEM_TITLE = "extra_item_title"

		fun createIntent(context: Context, categoryId: String, itemTitle: String): Intent {
			return Intent(context, UserGuideItemDetailActivity::class.java).apply {
				putExtra(EXTRA_CATEGORY_ID, categoryId)
				putExtra(EXTRA_ITEM_TITLE, itemTitle)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_user_guide_item_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_guide_item_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
		val itemTitle = intent.getStringExtra(EXTRA_ITEM_TITLE)
		val category = categoryId?.let { UserGuideContent.findCategory(it) }
		val item = category?.items?.find { it.title == itemTitle }

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.text_top_bar_title).text = item?.title ?: "사용 가이드"

		if (item == null) {
			finish()
			return
		}

		val container = findViewById<LinearLayout>(R.id.container_guide_item_detail)

		val descView = TextView(this).apply {
			text = item.description
			textSize = 14f
			setTextColor(
				ContextCompat.getColor(
					this@UserGuideItemDetailActivity,
					R.color.text_primary
				)
			)
			setLineSpacing(dp(2).toFloat(), 1f)
		}
		container.addView(descView)

		// 이 항목에 붙은 사진들을 캡션과 함께 세로로 쌓는다. drawable 리소스가 아직 없는 이름이면
		// (사진을 못 붙인 항목 등) 조용히 건너뛴다 — 글만으로도 항목은 정상 표시돼야 한다.
		for (image in item.images) {
			val resId = resources.getIdentifier(image.resName, "drawable", packageName)
			if (resId == 0) continue

			val imageView = ImageView(this).apply {
				setImageResource(resId)
				adjustViewBounds = true
				scaleType = ImageView.ScaleType.FIT_CENTER
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { topMargin = dp(16) }
				background = ContextCompat.getDrawable(
					this@UserGuideItemDetailActivity, R.drawable.bg_book_button
				)
				setPadding(dp(4), dp(4), dp(4), dp(4))
			}
			container.addView(imageView)

			if (image.caption != null) {
				val captionView = TextView(this).apply {
					text = image.caption
					textSize = 12f
					gravity = android.view.Gravity.CENTER
					setTextColor(
						ContextCompat.getColor(this@UserGuideItemDetailActivity, R.color.text_hint)
					)
					layoutParams = LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT
					).apply { topMargin = dp(4) }
				}
				container.addView(captionView)
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}