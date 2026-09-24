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

		if (item == null || categoryId == null) {
			finish()
			return
		}

		val container = findViewById<LinearLayout>(R.id.container_guide_item_detail)

		// 설명을 옅은 카드 안에 넣고, 글자 크기·줄간격을 넉넉히 줘서 더 편하게 읽히게 한다
		// (예전엔 배경 위에 글자만 덩그러니 있어서 눈에 잘 안 들어왔다).
		val descView = TextView(this).apply {
			text = item.description
			textSize = 15.5f
			setTextColor(
				ContextCompat.getColor(
					this@UserGuideItemDetailActivity,
					R.color.text_primary
				)
			)
			setLineSpacing(dp(6).toFloat(), 1.05f)
			setPadding(dp(16), dp(16), dp(16), dp(16))
			background = ContextCompat.getDrawable(
				this@UserGuideItemDetailActivity, R.drawable.bg_book_button
			)
		}
		container.addView(descView)

		renderImageGrid(container, item)
	}

	/** 사진들을 2열 그리드로 보여준다(리소스가 없는 항목은 조용히 건너뛴다). 화면이 아주 좁은
	 * 폰(가장 짧은 변이 360dp 미만)에서는 그리드가 너무 빽빽해지니 한 줄에 한 장씩만 보여준다.
	 * 어느 사진이든 누르면 이 항목의 사진 전체를 스와이프로 넘겨보는 전체화면 뷰어가 열린다. */
	private fun renderImageGrid(container: LinearLayout, item: UserGuideContent.GuideItem) {
		val validImages = item.images.filter {
			resources.getIdentifier(it.resName, "drawable", packageName) != 0
		}
		if (validImages.isEmpty()) return

		val columns = if (resources.configuration.smallestScreenWidthDp < 360) 1 else 2
		val resNames = validImages.map { it.resName }

		var index = 0
		while (index < validImages.size) {
			val rowImages = validImages.subList(index, minOf(index + columns, validImages.size))
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { topMargin = dp(16) }
			}

			// 그리드 칸이 다 안 채워진 마지막 줄(예: 2열인데 사진이 1장 남음)은 절반만 채우지 않고
			// 꽉 채운 크기로 보여준다 — 어중간하게 비어 보이지 않게.
			val fillRow = rowImages.size == columns
			for ((offsetInRow, image) in rowImages.withIndex()) {
				val globalIndex = index + offsetInRow
				val cell = buildImageCell(image, resNames, globalIndex)
				val cellParams = if (fillRow) {
					LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
				} else {
					LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT
					)
				}
				if (offsetInRow > 0) cellParams.marginStart = dp(10)
				cell.layoutParams = cellParams
				row.addView(cell)
			}
			container.addView(row)
			index += columns
		}
	}

	private fun buildImageCell(
		image: UserGuideContent.GuideImage,
		allResNames: List<String>,
		indexInList: Int
	): LinearLayout {
		val resId = resources.getIdentifier(image.resName, "drawable", packageName)
		return LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL

			val imageView = ImageView(this@UserGuideItemDetailActivity).apply {
				setImageResource(resId)
				adjustViewBounds = true
				scaleType = ImageView.ScaleType.FIT_CENTER
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				)
				background = ContextCompat.getDrawable(
					this@UserGuideItemDetailActivity, R.drawable.bg_book_button
				)
				setPadding(dp(4), dp(4), dp(4), dp(4))
				isClickable = true
				isFocusable = true
				setOnClickListener {
					UserGuideImageViewerActivity.start(
						this@UserGuideItemDetailActivity, allResNames, indexInList
					)
				}
			}
			addView(imageView)

			if (image.caption != null) {
				val captionView = TextView(this@UserGuideItemDetailActivity).apply {
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
				addView(captionView)
			}
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}