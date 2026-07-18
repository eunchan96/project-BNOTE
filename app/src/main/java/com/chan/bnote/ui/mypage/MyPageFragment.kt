package com.chan.bnote.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.profile.ProfileDisplay
import com.chan.bnote.ui.TopBarActionHandler
import com.chan.bnote.ui.TopBarConfig
import kotlinx.coroutines.launch

class MyPageFragment : Fragment(), TopBarActionHandler {

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_mypage, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		view.findViewById<View>(R.id.container_profile_thumbnail).setOnClickListener {
			startActivity(Intent(requireContext(), ProfileActivity::class.java))
		}
		view.findViewById<ImageView>(R.id.img_profile_photo).loadProfilePhoto(null)
		view.findViewById<TextView>(R.id.menu_settings).setOnClickListener {
			startActivity(Intent(requireContext(), SettingsActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_reading_plan).setOnClickListener {
			startActivity(Intent(requireContext(), ReadingPlanActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_verse_of_year).setOnClickListener {
			startActivity(Intent(requireContext(), VerseOfYearActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_prayer_request).setOnClickListener {
			startActivity(Intent(requireContext(), PrayerRequestActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_memorization).setOnClickListener {
			startActivity(Intent(requireContext(), MemorizationVerseListActivity::class.java))
		}
		view.findViewById<TextView>(R.id.menu_bible_knowledge).setOnClickListener {
			startActivity(
				Intent(
					requireContext(),
					com.chan.bnote.ui.knowledge.BibleKnowledgeHubActivity::class.java
				)
			)
		}
	}

	override fun onResume() {
		super.onResume()
		// 정보 수정 화면에서 돌아왔을 때 최신 값을 반영하기 위해 매번 다시 불러온다.
		loadProfileThumbnail()
	}

	private fun loadProfileThumbnail() {
		val view = view ?: return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val profile = db.userProfileDao().get()

			view.findViewById<TextView>(R.id.text_profile_name).setNameWithPosition(
				ProfileDisplay.nameText(profile), ProfileDisplay.positionText(profile)
			)
			view.findViewById<TextView>(R.id.text_profile_meta).text =
				ProfileDisplay.thumbnailMetaText(profile)
			view.findViewById<ImageView>(R.id.img_profile_photo)
				.loadProfilePhoto(profile?.photoPath)
		}
	}

	override fun getTopBarConfig() = TopBarConfig(title = "마이페이지", showMenu = false)
}