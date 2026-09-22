package com.chan.bnote.ui.sermon.addsermon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.chan.bnote.R

/** 사진을 전체화면으로 보여준다. 여러 장이면(설교 상세에서 진입) 좌우로 스와이프해서 다음/이전
 * 사진으로 넘길 수 있고, 위쪽에 "2 / 5"처럼 몇 번째인지 보여준다. 사진 한 장만 볼 때(기존 호출부
 * 호환용)도 그대로 동작한다. */
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

	private class PhotoPagerAdapter(private val paths: List<String>) :
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
		}

		override fun getItemCount(): Int = paths.size
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_photo_viewer)

		val paths = intent.getStringArrayListExtra(EXTRA_PATHS) ?: arrayListOf()
		val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)
			.coerceIn(0, (paths.size - 1).coerceAtLeast(0))

		val pager = findViewById<ViewPager2>(R.id.pager_photo_viewer)
		val counter = findViewById<TextView>(R.id.text_photo_viewer_counter)

		pager.adapter = PhotoPagerAdapter(paths)
		pager.setCurrentItem(startIndex, false)

		counter.visibility = if (paths.size > 1) View.VISIBLE else View.GONE
		fun updateCounter(position: Int) {
			counter.text = "${position + 1} / ${paths.size}"
		}
		updateCounter(startIndex)
		pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateCounter(position)
			}
		})

		findViewById<ImageView>(R.id.btn_close_photo_viewer).setOnClickListener { finish() }
	}
}