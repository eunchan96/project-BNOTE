package com.chan.bnote.ui.mypage.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.chan.bnote.R

/** 가이드 항목의 사진을 전체화면으로 보여준다. PhotoViewerActivity와 같은 구조지만, 파일 경로가
 * 아니라 앱에 번들된 drawable 리소스 이름(예: "guide_bible_search")을 받는다는 점만 다르다. */
class UserGuideImageViewerActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_RES_NAMES = "extra_res_names"
		private const val EXTRA_START_INDEX = "extra_start_index"

		fun start(context: Context, resNames: List<String>, startIndex: Int) {
			val intent = Intent(context, UserGuideImageViewerActivity::class.java)
			intent.putStringArrayListExtra(EXTRA_RES_NAMES, ArrayList(resNames))
			intent.putExtra(EXTRA_START_INDEX, startIndex)
			context.startActivity(intent)
		}
	}

	private inner class ImagePagerAdapter(private val resIds: List<Int>) :
		RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

		inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val image: ImageView = view.findViewById(R.id.image_viewer_page)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.item_photo_viewer_page, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			holder.image.setImageResource(resIds[position])
		}

		override fun getItemCount(): Int = resIds.size
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_user_guide_image_viewer)

		val resNames = intent.getStringArrayListExtra(EXTRA_RES_NAMES) ?: arrayListOf()
		val resIds = resNames.mapNotNull { name ->
			val id = resources.getIdentifier(name, "drawable", packageName)
			if (id == 0) null else id
		}
		if (resIds.isEmpty()) {
			finish()
			return
		}
		val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
			.coerceIn(0, resIds.size - 1)

		val pager = findViewById<ViewPager2>(R.id.pager_guide_image_viewer)
		val counter = findViewById<TextView>(R.id.text_guide_image_viewer_counter)
		val closeButton = findViewById<ImageView>(R.id.btn_close_guide_image_viewer)

		// 사진은 상태바 뒤까지 꽉 차게 두되(전체화면 뷰어라 오히려 몰입감이 좋다), 그 위에 뜨는
		// 카운터·닫기 버튼만 상태바(와이파이·시계 아이콘 줄)와 안 겹치게 그만큼 밀어준다.
		val counterBaseMarginTop = (counter.layoutParams as ViewGroup.MarginLayoutParams).topMargin
		val closeBaseMargin = (closeButton.layoutParams as ViewGroup.MarginLayoutParams).topMargin
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.guide_image_viewer_root)) { _, insets ->
			val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
			counter.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = counterBaseMarginTop + statusBarTop
			}
			closeButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
				topMargin = closeBaseMargin + statusBarTop
			}
			insets
		}

		pager.adapter = ImagePagerAdapter(resIds)
		pager.setCurrentItem(startIndex, false)

		counter.visibility = if (resIds.size > 1) View.VISIBLE else View.GONE
		fun updateCounter(position: Int) {
			counter.text = "${position + 1} / ${resIds.size}"
		}
		updateCounter(startIndex)
		pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateCounter(position)
			}
		})

		findViewById<ImageView>(R.id.btn_close_guide_image_viewer).setOnClickListener { finish() }
		closeButton.setOnClickListener { finish() }
	}
}