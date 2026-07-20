package com.chan.bnote.ui.sermon.detail

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable

class TriangleBubbleDrawable(
	private val tailCenterXRatio: Float, // 꼬리가 가로 폭의 몇 % 지점에 있는지 (0~1)
	density: Float
) : Drawable() {

	private val cornerRadius = 12f * density
	private val tailWidth = 14f * density
	private val tailHeight = 8f * density

	private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		color = Color.parseColor("#EE333333")
		style = Paint.Style.FILL
	}

	override fun draw(canvas: Canvas) {
		val bounds = bounds
		val bodyBottom = bounds.bottom - tailHeight
		val tailCenterX = (bounds.left + (bounds.width() * tailCenterXRatio))
			.coerceIn(bounds.left + tailWidth, bounds.right - tailWidth)

		val path = Path()
		// 말풍선 몸체 (둥근 사각형)
		path.addRoundRect(
			bounds.left.toFloat(), bounds.top.toFloat(),
			bounds.right.toFloat(), bodyBottom,
			cornerRadius, cornerRadius, Path.Direction.CW
		)
		// 아래쪽 꼬리 (삼각형)
		val tailPath = Path()
		tailPath.moveTo(tailCenterX - tailWidth / 2, bodyBottom)
		tailPath.lineTo(tailCenterX, bounds.bottom.toFloat())
		tailPath.lineTo(tailCenterX + tailWidth / 2, bodyBottom)
		tailPath.close()

		path.addPath(tailPath)
		canvas.drawPath(path, paint)
	}

	override fun setAlpha(alpha: Int) {
		paint.alpha = alpha
	}

	override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
		paint.colorFilter = colorFilter
	}

	override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}