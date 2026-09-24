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

		// PhotoViewerActivity와 달리 여기서는 사진도 상태바 아래로 내려서, 상태바 자리는 항상
		// 배경(검정)만 보이게 한다 — 사진 위에 아이콘이 겹쳐서 잘 안 보이던 문제까지 같이 없앤다.
		// 화면 전체(루트)에 위쪽 패딩을 주면 그 안의 사진·카운터·닫기 버튼이 전부 같이 내려온다.
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.guide_image_viewer_root)) { v, insets ->
			val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
			v.setPadding(v.paddingLeft, statusBarTop, v.paddingRight, v.paddingBottom)
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