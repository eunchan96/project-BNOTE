package com.chan.bnote.ui.sermon.addsermon

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
import coil.load
import com.chan.bnote.R

/** 사진을 전체화면으로 보여준다. 여러 장이면(설교 상세에서 진입) 좌우로 스와이프해서 다음/이전
 * 사진으로 넘길 수 있고, 위쪽에 "2 / 5"처럼 몇 번째인지 보여준다. 사진 한 장만 볼 때(기존 호출부
 * 호환용)도 그대로 동작한다. 사진을 한 번 탭하면 뒤로가기 버튼·카운터가 숨고, 다시 탭하면 나타난다. */
class PhotoViewerActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_PATHS = "extra_photo_paths"
		private const val EXTRA_START_INDEX = "extra_start_index"

		/** 사진 한 장만 볼 때(기존 호출부 호환용). */
		fun start(context: Context, filePath: String) {
			start(context, listOf(filePath), 0)
		}

		/** 여러 장 중 startIndex번째부터 스와이프로 넘겨볼 때. */
		fun start(context: Context, filePaths: List<String>, startIndex: Int) {
			val intent = Intent(context, PhotoViewerActivity::class.java)
			intent.putStringArrayListExtra(EXTRA_PATHS, ArrayList(filePaths))
			intent.putExtra(EXTRA_START_INDEX, startIndex)
			context.startActivity(intent)
		}
	}

	private class PhotoPagerAdapter(
		private val paths: List<String>,
		private val onTap: () -> Unit
	) :
		RecyclerView.Adapter<PhotoPagerAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val image: ImageView = view.findViewById(R.id.image_viewer_page)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.item_photo_viewer_page, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			holder.image.load(paths[position])
			// 사진 위든 바깥 검은 여백이든, 한 번 탭하면 위쪽 버튼들을 숨기거나 보인다(확대/축소 제스처와는 별개).
			(holder.image as? com.github.chrisbanes.photoview.PhotoView)
				?.setOnViewTapListener { _, _, _ -> onTap() }
		}

		override fun getItemCount(): Int = paths.size
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_photo_viewer)

		val paths = intent.getStringArrayListExtra(EXTRA_PATHS) ?: arrayListOf()
		val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
			.coerceIn(0, (paths.size - 1).coerceAtLeast(0))

		val pager = findViewById<ViewPager2>(R.id.pager_photo_viewer)
		val counter = findViewById<TextView>(R.id.text_photo_viewer_counter)
		val closeButton = findViewById<ImageView>(R.id.btn_close_photo_viewer)

		// 가이드 사진 뷰어(UserGuideImageViewerActivity)와 같이 사진도 상태바 아래로 내려서, 상태바
		// 자리는 항상 배경(검정)만 보이게 한다. 예전엔 사진이 상태바 뒤까지 깔려서 공지 사진처럼 위쪽에
		// 글자가 있는 사진은 시계 · 와이파이 아이콘과 겹쳐 보였다. 화면 전체(루트)에 위쪽 패딩을 주면
		// 그 안의 사진 · 카운터 · 닫기 버튼이 전부 같이 내려온다.
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.photo_viewer_root)) { v, insets ->
			val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
			v.setPadding(v.paddingLeft, statusBarTop, v.paddingRight, v.paddingBottom)
			insets
		}

		counter.visibility = if (paths.size > 1) View.VISIBLE else View.GONE
		// 사진이 한 장이면 카운터는 원래 안 보이므로, 탭으로 숨기고 보일 대상에서도 뺀다.
		val controlsToggle = com.chan.bnote.ui.common.ViewerControlsToggle(
			if (paths.size > 1) listOf(closeButton, counter) else listOf(closeButton)
		)

		pager.adapter = PhotoPagerAdapter(paths) { controlsToggle.toggle() }
		pager.setCurrentItem(startIndex, false)

		fun updateCounter(position: Int) {
			counter.text = "${position + 1} / ${paths.size}"
		}
		updateCounter(startIndex)
		pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateCounter(position)
			}
		})

		closeButton.setOnClickListener { finish() }
	}
}