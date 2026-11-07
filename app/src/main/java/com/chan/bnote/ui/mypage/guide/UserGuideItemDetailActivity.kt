package com.chan.bnote.ui.mypage.guide

import android.content.Context
import android.content.Intent
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
import com.chan.bnote.data.AppSettings
import com.chan.bnote.ui.common.ListText

/** 가이드 항목 하나(GuideItem)만 따로 보여주는 화면. 예전에는 카테고리 안 항목을 전부 한 화면에
 * 이어 붙여서 보여줬는데, 사진이 붙으면서 한 카테고리에 20장 넘게 쌓여 너무 길고 복잡해졌다.
 * 이제 카테고리를 누르면 항목 "목록"만 보여주고(UserGuideDetailActivity), 항목 하나를 또
 * 누르면 이 화면으로 넘어와 그 항목만 본다.
 *
 * 설명([UserGuideContent.GuideItem.description])이 여러 줄이면 "단계형"으로 그린다:
 * - 첫 줄: 한 줄 요약(굵게)
 * - "1. " · "・ "로 시작하는 줄: 단계 · 항목(알림 상세와 같은 ListText로 들여쓰기 · 간격)
 * - "💡 "로 시작하는 줄: 단계 아래의 옅은 참고 박스로 모은다
 * - 사진은 [UserGuideContent.GuideImage.afterStep]번째 단계 · 항목 바로 아래에, 지정이 없으면 맨 아래에
 * 설명이 한 줄(예전 문단 형식)이면 예전처럼 옅은 카드 안 문단 + 맨 아래 사진으로 그린다 —
 * 아직 단계형으로 옮기지 않은 항목도 그대로 보인다. */
class UserGuideItemDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY_ID = "extra_category_id"
		private const val EXTRA_ITEM_TITLE = "extra_item_title"

		private const val TIP_PREFIX = "💡"

		fun createIntent(context: Context, categoryId: String, itemTitle: String): Intent {
			return Intent(context, UserGuideItemDetailActivity::class.java).apply {
				putExtra(EXTRA_CATEGORY_ID, categoryId)
				putExtra(EXTRA_ITEM_TITLE, itemTitle)
			}
		}
	}

	/** 본문 글자 크기: 스크랩 · 북마크 목록과 같은 규칙(설정의 글자 크기 − 2)에서 0.5 작게(기본 15.5sp). */
	private val bodyTextSp: Float by lazy {
		AppSettings.getListPreviewFontSize(this) - 0.5f
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
		// 리소스가 없는 사진은 조용히 건너뛴다. 전체화면 뷰어는 이 목록 순서로 넘겨 본다.
		val validImages = item.images.filter {
			resources.getIdentifier(it.resName, "drawable", packageName) != 0
		}
		if (item.description.contains('\n')) {
			renderSteps(container, item.description, validImages)
		} else {
			renderParagraph(container, item.description)
			renderImageGrid(container, validImages, validImages)
		}
	}

	/** 예전 형식(문단 하나): 옅은 카드 안에 글자 크기 · 줄간격을 넉넉히 줘서 보여준다. */
	private fun renderParagraph(container: LinearLayout, description: String) {
		container.addView(TextView(this).apply {
			text = description
			textSize = bodyTextSp
			setTextColor(color(R.color.text_primary))
			setLineSpacing(dp(6).toFloat(), 1.05f)
			setPadding(dp(16), dp(16), dp(16), dp(16))
			background = ContextCompat.getDrawable(
				this@UserGuideItemDetailActivity, R.drawable.bg_book_button
			)
		})
	}

	/** 단계형: 요약 → 단계 · 항목(중간중간 사진) → 참고 박스 → 위치를 안 정한 사진. */
	private fun renderSteps(
		container: LinearLayout,
		description: String,
		validImages: List<UserGuideContent.GuideImage>
	) {
		val lines = description.lines().map { it.trim() }.filter { it.isNotEmpty() }
		if (lines.isEmpty()) return

		// 한 줄 요약
		container.addView(TextView(this).apply {
			text = lines.first()
			textSize = bodyTextSp + 1f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(color(R.color.text_primary))
			setLineSpacing(0f, 1.3f)
		})

		val tips = mutableListOf<String>()
		val buffer = mutableListOf<String>()
		var stepCount = 0
		fun flush() {
			if (buffer.isEmpty()) return
			addStepText(container, buffer.joinToString("\n"))
			buffer.clear()
		}
		for (line in lines.drop(1)) {
			if (line.startsWith(TIP_PREFIX)) {
				tips += line
				continue
			}
			buffer += line
			stepCount++
			val imagesHere = validImages.filter { it.afterStep == stepCount }
			if (imagesHere.isNotEmpty()) {
				flush()
				renderImageGrid(container, imagesHere, validImages)
			}
		}
		flush()

		if (tips.isNotEmpty()) addTipBox(container, tips)

		// 위치를 안 정했거나, 단계 수보다 큰 번호를 적은 사진은 맨 아래에 모은다.
		val rest = validImages.filter { it.afterStep == null || it.afterStep > stepCount }
		renderImageGrid(container, rest, validImages)
	}

	private fun addStepText(container: LinearLayout, text: String) {
		val view = TextView(this).apply {
			textSize = bodyTextSp
			setTextColor(color(R.color.text_primary))
			// 한 항목 안에서 넘어간 줄은 촘촘하게, 항목 사이는 ListText의 간격으로 띄운다(알림 상세와 같은 방식).
			setLineSpacing(0f, 1.3f)
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { topMargin = dp(16) }
		}
		// textSize를 먼저 정해야 "1. " · "・ " 폭을 정확히 잰다.
		view.text = ListText.style(text, view, paragraphGapEm = 0.6f)
		container.addView(view)
	}

	/** "💡 참고" 줄들을 알림의 "업데이트 방법" 박스와 같은 옅은 박스에 모은다. */
	private fun addTipBox(container: LinearLayout, tips: List<String>) {
		val box = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			background = ContextCompat.getDrawable(
				this@UserGuideItemDetailActivity, R.drawable.bg_notice_callout
			)
			setPadding(dp(16), dp(14), dp(16), dp(14))
			layoutParams = LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
			).apply { topMargin = dp(20) }
		}
		val view = TextView(this).apply {
			textSize = bodyTextSp - 0.5f
			setTextColor(color(R.color.text_primary))
			setLineSpacing(0f, 1.3f)
		}
		view.text = ListText.style(tips.joinToString("\n"), view, paragraphGapEm = 0.5f)
		box.addView(view)
		container.addView(box)
	}

	/** 사진들을 2열 그리드로 보여준다. 화면이 아주 좁은 폰(가장 짧은 변이 360dp 미만)에서는
	 * 그리드가 너무 빽빽해지니 한 줄에 한 장씩만 보여준다.
	 * 어느 사진이든 누르면 이 항목의 사진 전체([allImages])를 스와이프로 넘겨보는 전체화면 뷰어가 열린다. */
	private fun renderImageGrid(
		container: LinearLayout,
		images: List<UserGuideContent.GuideImage>,
		allImages: List<UserGuideContent.GuideImage>
	) {
		if (images.isEmpty()) return

		val columns = if (resources.configuration.smallestScreenWidthDp < 360) 1 else 2
		val allResNames = allImages.map { it.resName }

		// 칸 너비를 고정 픽셀로 미리 계산해서 모든 칸에 그대로 쓴다.
		// weight로 나누면 한 줄에 사진이 하나뿐일 때 그 하나가 weight 몫(=줄 전체)을 다 차지해버려서 전체 너비로 커지는 문제가 있었다
		// 고정 너비를 쓰면 사진이 하나여도 칸 하나 크기만 차지하고 나머지는 그냥 빈 공간으로 남는다.
		val screenWidthPx = resources.displayMetrics.widthPixels
		val containerPaddingPx = dp(20) * 2
		val gapPx = dp(10) * (columns - 1)
		val columnWidthPx = (screenWidthPx - containerPaddingPx - gapPx) / columns

		var index = 0
		while (index < images.size) {
			val rowImages = images.subList(index, minOf(index + columns, images.size))
			val row = LinearLayout(this).apply {
				orientation = LinearLayout.HORIZONTAL
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { topMargin = dp(16) }
			}
			// 사진이 한 장뿐이어도 그리드 칸(2열이면 절반 너비) 크기 그대로 보여준다
			// 항목마다 사진 개수가 달라도 사진 크기가 들쭉날쭉해 보이지 않게 항상 같은 칸 크기를 쓴다.
			for ((offsetInRow, image) in rowImages.withIndex()) {
				val cell = buildImageCell(image, allResNames, allImages.indexOf(image))
				val cellParams =
					LinearLayout.LayoutParams(columnWidthPx, LinearLayout.LayoutParams.WRAP_CONTENT)
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
						this@UserGuideItemDetailActivity, allResNames, indexInList.coerceAtLeast(0)
					)
				}
			}
			addView(imageView)

			if (image.caption != null) {
				val captionView = TextView(this@UserGuideItemDetailActivity).apply {
					text = image.caption
					textSize = 12f
					gravity = android.view.Gravity.CENTER
					setTextColor(color(R.color.text_hint))
					layoutParams = LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.WRAP_CONTENT
					).apply { topMargin = dp(4) }
				}
				addView(captionView)
			}
		}
	}

	private fun color(resId: Int): Int = ContextCompat.getColor(this, resId)

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}