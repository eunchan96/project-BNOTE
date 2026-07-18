package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.profile.ProfilePhotoStorage
import com.chan.bnote.data.profile.UserProfile
import kotlinx.coroutines.launch

class ProfileEditActivity : AppCompatActivity() {

	private lateinit var imgPhoto: ImageView
	private lateinit var inputName: EditText
	private lateinit var inputChurch: EditText
	private lateinit var inputDepartment: EditText
	private lateinit var inputPosition: EditText

	// 갤러리에서 새로 고른 사진 경로. 저장을 눌러야 실제로 반영된다.
	private var pendingPhotoPath: String? = null
	private var existingPhotoPath: String? = null

	private val pickPhotoLauncher = registerForActivityResult(
		ActivityResultContracts.PickVisualMedia()
	) { uri ->
		if (uri == null) return@registerForActivityResult
		ProfilePhotoStorage.copyToInternalStorage(this, uri)?.let { path ->
			pendingPhotoPath = path
			imgPhoto.load(path) { placeholder(R.drawable.ic_person) }
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_profile_edit)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_edit_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "내 정보 수정"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		imgPhoto = findViewById(R.id.img_edit_photo)
		inputName = findViewById(R.id.input_name)
		inputChurch = findViewById(R.id.input_church)
		inputDepartment = findViewById(R.id.input_department)
		inputPosition = findViewById(R.id.input_position)

		findViewById<android.view.View>(R.id.btn_edit_photo).setOnClickListener {
			pickPhotoLauncher.launch(
				PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
			)
		}

		findViewById<TextView>(R.id.btn_save_profile).setOnClickListener {
			saveProfile()
		}

		loadExistingProfile()
	}

	private fun loadExistingProfile() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val profile = db.userProfileDao().get() ?: return@launch

			inputName.setText(profile.name)
			inputChurch.setText(profile.church)
			inputDepartment.setText(profile.department)
			inputPosition.setText(profile.position)

			existingPhotoPath = profile.photoPath
			if (!profile.photoPath.isNullOrBlank()) {
				imgPhoto.load(profile.photoPath) { placeholder(R.drawable.ic_person) }
			}
		}
	}

	private fun saveProfile() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			// 새 사진을 골랐으면 이전 사진 파일은 지워서 내부 저장소에 계속 쌓이지 않게 한다.
			if (pendingPhotoPath != null && !existingPhotoPath.isNullOrBlank()) {
				ProfilePhotoStorage.deleteFile(existingPhotoPath!!)
			}

			db.userProfileDao().upsert(
				UserProfile(
					id = 1,
					photoPath = pendingPhotoPath ?: existingPhotoPath,
					name = inputName.text.toString().trim(),
					church = inputChurch.text.toString().trim(),
					department = inputDepartment.text.toString().trim(),
					position = inputPosition.text.toString().trim()
				)
			)
			finish()
		}
	}
}