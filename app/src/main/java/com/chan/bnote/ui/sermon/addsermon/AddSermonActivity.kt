package com.chan.bnote.ui.sermon.addsermon

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.data.sermon.sermonphoto.SermonPhoto
import com.chan.bnote.data.sermon.sermonphoto.SermonPhotoStorage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddSermonActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_SERMON_ID = "extra_sermon_id"
		private const val EXTRA_INITIAL_DATE_MILLIS = "extra_initial_date_millis"
		private const val MAX_PHOTOS = 5

		/** 신규 등록용 Intent. */
		fun createIntent(
			context: Context,
			initialDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
		): Intent {
			return Intent(context, AddSermonActivity::class.java).apply {
				putExtra(EXTRA_INITIAL_DATE_MILLIS, initialDateMillis)
			}
		}

		/** 기존 설교 수정용 Intent. */
		fun editIntent(context: Context, sermonId: Long): Intent {
			return Intent(context, AddSermonActivity::class.java).apply {
				putExtra(EXTRA_SERMON_ID, sermonId)
			}
		}
	}

	private var existingSermon: Sermon? = null
	private var selectedDateMillis: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())
	private var selectedCategoryId: Long? = null
	private var selectedPreacherId: Long? = null
	private val bibleRefs = mutableListOf<SermonBibleRef>()
	private val photoPaths = mutableListOf<String>()
	private var pendingCaptureFile: File? = null

	private lateinit var flexboxRefs: com.google.android.flexbox.FlexboxLayout
	private lateinit var btnPickPreacher: TextView
	private lateinit var btnPickCategory: TextView
	private lateinit var btnDate: TextView
	private lateinit var btnAddPhoto: TextView
	private lateinit var scrollPhotos: View
	private lateinit var photoContainer: LinearLayout
	private lateinit var editMemo: EditText

	private val pickPhotosLauncher = registerForActivityResult(
		ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)
	) { uris ->
		if (uris.isEmpty()) return@registerForActivityResult
		val remaining = MAX_PHOTOS - photoPaths.size
		for (uri in uris.take(remaining)) {
			SermonPhotoStorage.copyToInternalStorage(this, uri)?.let { path ->
				photoPaths.add(path)
			}
		}
		renderPhotoThumbnails()
	}

	private val takePictureLauncher = registerForActivityResult(
		ActivityResultContracts.TakePicture()
	) { success ->
		val file = pendingCaptureFile
		pendingCaptureFile = null
		if (success && file != null) {
			photoPaths.add(file.absolutePath)
			renderPhotoThumbnails()
		}
	}

	private val cameraPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		if (granted) launchCamera() else {
			Toast.makeText(this, "카메라 권한이 필요해요", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_add_sermon)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_sermon_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
			// enableEdgeToEdge()로 인해 decorFitsSystemWindows가 꺼져 있어서, 키보드가 떠도
			// 기존 windowSoftInputMode="adjustResize"만으로는 화면이 자동으로 줄어들지 않는다.
			// 키보드 인셋을 직접 소비해서(하단 패딩으로) 삼성노트처럼 키보드 위 공간을 확보해야 한다.
			v.setPadding(
				systemBars.left,
				systemBars.top,
				systemBars.right,
				maxOf(systemBars.bottom, ime.bottom)
			)
			insets
		}

		val sermonId = intent.getLongExtra(EXTRA_SERMON_ID, -1L)
		val isEditMode = sermonId != -1L

		if (intent.hasExtra(EXTRA_INITIAL_DATE_MILLIS)) {
			selectedDateMillis = intent.getLongExtra(EXTRA_INITIAL_DATE_MILLIS, selectedDateMillis)
		}

		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "설교 기록 수정" else "설교 기록 추가"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val editTitle = findViewById<EditText>(R.id.edit_title)
		editMemo = findViewById(R.id.edit_memo)
		btnDate = findViewById(R.id.btn_pick_date)
		btnPickPreacher = findViewById(R.id.btn_pick_preacher)
		btnPickCategory = findViewById(R.id.btn_pick_category)
		flexboxRefs = findViewById(R.id.flexbox_bible_refs)
		btnAddPhoto = findViewById(R.id.btn_add_photo)
		scrollPhotos = findViewById(R.id.scroll_photo_thumbnails)
		photoContainer = findViewById(R.id.container_photo_thumbnails)

		updateDateText()
		renderPhotoThumbnails()
		renderBibleRefBoxes()

		findViewById<TextView>(R.id.btn_format_bold).setOnClickListener {
			applyFormatting(bold = true)
		}
		findViewById<TextView>(R.id.btn_format_underline).setOnClickListener {
			applyFormatting(bold = false)
		}
		findViewById<TextView>(R.id.btn_format_color).setOnClickListener { showColorPicker() }

		btnDate.setOnClickListener {
			val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
			DatePickerDialog(
				this,
				{ _, year, month, day ->
					val picked = Calendar.getInstance()
					picked.set(year, month, day, 0, 0, 0)
					selectedDateMillis = DateUtils.normalizeToDayStart(picked.timeInMillis)
					updateDateText()
				},
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
			).show()
		}

		btnPickPreacher.setOnClickListener {
			val picker = PreacherPickerBottomSheet()
			picker.onPreacherSelected = { preacher ->
				selectedPreacherId = preacher.id
				btnPickPreacher.text = preacher.name
			}
			picker.show(supportFragmentManager, "preacher_picker")
		}

		btnPickCategory.setOnClickListener {
			val picker = CategoryPickerBottomSheet()
			picker.onCategorySelected = { category ->
				selectedCategoryId = category?.id
				btnPickCategory.text = category?.name ?: "카테고리 선택"
			}
			picker.show(supportFragmentManager, "category_picker")
		}

		btnAddPhoto.setOnClickListener { showPhotoSourceMenu(it) }

		findViewById<TextView>(R.id.btn_save_sermon).setOnClickListener {
			save(editTitle.text.toString().trim(), editMemo.text)
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			if (isEditMode) {
				val sermon = db.sermonDao().getById(sermonId)
				existingSermon = sermon
				if (sermon != null) {
					editTitle.setText(sermon.title)
					editMemo.setText(RichTextUtils.toEditable(sermon.memo))
					selectedDateMillis = sermon.sermonDate
					selectedCategoryId = sermon.categoryId
					selectedPreacherId = sermon.preacherId
					updateDateText()

					bibleRefs.addAll(db.sermonBibleRefDao().getBySermon(sermon.id))
					renderBibleRefBoxes()

					photoPaths.addAll(
						db.sermonPhotoDao().getBySermon(sermon.id).map { it.filePath })
					renderPhotoThumbnails()
				}
			}

			selectedPreacherId?.let { id ->
				db.preacherDao().getById(id)?.let { btnPickPreacher.text = it.name }
			}
			selectedCategoryId?.let { id ->
				db.sermonCategoryDao().getById(id)?.let { btnPickCategory.text = it.name }
			}
		}
	}

	private fun showPhotoSourceMenu(anchor: View) {
		if (photoPaths.size >= MAX_PHOTOS) {
			Toast.makeText(this, "사진은 최대 ${MAX_PHOTOS}장까지 추가할 수 있어요", Toast.LENGTH_SHORT).show()
			return
		}
		val popup = PopupMenu(this, anchor)
		popup.menu.add(0, 0, 0, "갤러리에서 선택")
		popup.menu.add(0, 1, 1, "카메라로 촬영")
		popup.setOnMenuItemClickListener { item ->
			when (item.itemId) {
				0 -> pickPhotosLauncher.launch(
					androidx.activity.result.PickVisualMediaRequest(
						ActivityResultContracts.PickVisualMedia.ImageOnly
					)
				)

				1 -> requestCameraAndLaunch()
			}
			true
		}
		popup.show()
	}

	private fun requestCameraAndLaunch() {
		val granted = ContextCompat.checkSelfPermission(
			this, Manifest.permission.CAMERA
		) == PackageManager.PERMISSION_GRANTED
		if (granted) {
			launchCamera()
		} else {
			cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
		}
	}

	private fun launchCamera() {
		val (file, uri) = SermonPhotoStorage.createCaptureTarget(this)
		pendingCaptureFile = file
		takePictureLauncher.launch(uri)
	}

	private fun renderPhotoThumbnails() {
		btnAddPhoto.text = "+ 사진 추가 (${photoPaths.size}/$MAX_PHOTOS)"
		photoContainer.removeAllViews()
		scrollPhotos.visibility = if (photoPaths.isEmpty()) View.GONE else View.VISIBLE

		for (path in photoPaths) {
			val thumb = LayoutInflater.from(this)
				.inflate(R.layout.item_sermon_photo_thumbnail, photoContainer, false)
			thumb.findViewById<ImageView>(R.id.image_photo_thumbnail).load(File(path))
			thumb.findViewById<ImageView>(R.id.btn_remove_photo).setOnClickListener {
				photoPaths.remove(path)
				renderPhotoThumbnails()
			}
			photoContainer.addView(thumb)
		}
	}

	private fun renderBibleRefBoxes() {
		flexboxRefs.removeAllViews()

		if (bibleRefs.isEmpty()) {
			flexboxRefs.addView(buildRefBox("본문 선택", fullWidth = true) { openBibleRangePicker() })
			return
		}

		for (ref in bibleRefs) {
			flexboxRefs.addView(
				buildRefBox(ref.toDisplayLabel(), fullWidth = false) {
					bibleRefs.remove(ref)
					renderBibleRefBoxes()
				}
			)
		}
		val addButton = buildAddSquareButton { openBibleRangePicker() }
		flexboxRefs.addView(addButton)

		// 본문 박스들이 실제로 배치된 뒤, 그 높이에 맞춰 "+" 버튼을 정확히 정사각형으로 맞춘다.
		flexboxRefs.post {
			val refBox = flexboxRefs.getChildAt(0)
			val height = refBox?.height ?: 0
			if (height > 0) {
				val lp =
					addButton.layoutParams as com.google.android.flexbox.FlexboxLayout.LayoutParams
				lp.height = height
				lp.width = height
				addButton.layoutParams = lp
			}
		}
	}

	private fun openBibleRangePicker() {
		val rangePicker = BibleRangePickerBottomSheet()
		rangePicker.onRangeSelected = { ref ->
			bibleRefs.add(ref)
			renderBibleRefBoxes()
		}
		rangePicker.show(supportFragmentManager, "bible_range_picker")
	}

	/** 본문 구절 하나를 나타내는 박스. [fullWidth]면 (아직 구절이 없을 때) 혼자 줄 전체를 채우고,
	 * 아니면 다른 박스들과 flexGrow로 너비를 나눠 갖는다. 탭하면 그 구절을 지운다(첫 박스 예외: 추가).
	 * 설교자/카테고리 박스와 똑같은 패딩·정렬을 써서 높이와 텍스트 위치를 맞춘다. */
	private fun buildRefBox(text: String, fullWidth: Boolean, onClick: () -> Unit): View {
		return TextView(this).apply {
			this.text = text
			textSize = 15f
			gravity = Gravity.START or Gravity.CENTER_VERTICAL
			maxLines = 1
			ellipsize = android.text.TextUtils.TruncateAt.END
			setPadding(dp(12), dp(8), dp(12), dp(8))
			setTextColor(ContextCompat.getColor(this@AddSermonActivity, R.color.text_primary))
			background =
				ContextCompat.getDrawable(this@AddSermonActivity, R.drawable.bg_book_button)
			isClickable = true
			isFocusable = true
			layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
				if (fullWidth) ViewGroup.LayoutParams.MATCH_PARENT else 0,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				flexGrow = 1f
				marginEnd = dp(4)
				bottomMargin = dp(4)
			}
			setOnClickListener { onClick() }
		}
	}

	/** 본문이 하나 이상 있을 때, 맨 끝에 붙는 정사각형 "+" 추가 버튼. 실제 크기는 renderBibleRefBoxes()에서
	 * 본문 박스 높이에 맞춰 다시 정해준다. */
	private fun buildAddSquareButton(onClick: () -> Unit): View {
		return TextView(this).apply {
			text = "+"
			textSize = 18f
			gravity = Gravity.CENTER
			setTextColor(ContextCompat.getColor(this@AddSermonActivity, R.color.brown_primary))
			background =
				ContextCompat.getDrawable(this@AddSermonActivity, R.drawable.bg_book_button)
			isClickable = true
			isFocusable = true
			layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				bottomMargin = dp(4)
			}
			setOnClickListener { onClick() }
		}
	}

	/** 선택한 텍스트에 굵게/밑줄을 씌우거나 벗긴다. 선택 영역이 없으면 안내만 한다. */
	private fun applyFormatting(bold: Boolean) {
		val range = requireSelection() ?: return
		RichTextUtils.toggleStyle(editMemo.text, range.first, range.second, bold)
	}

	private fun showColorPicker() {
		val range = requireSelection() ?: return

		val colors = listOf(
			"#000000" to "검정", "#795548" to "브라운", "#E53935" to "빨강",
			"#1E88E5" to "파랑", "#43A047" to "초록", "#FB8C00" to "주황"
		)

		val row = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			setPadding(dp(16), dp(8), dp(16), dp(8))
		}
		lateinit var dialog: androidx.appcompat.app.AlertDialog
		for ((hex, name) in colors) {
			val swatch = View(this).apply {
				contentDescription = name
				background = android.graphics.drawable.GradientDrawable().apply {
					shape = android.graphics.drawable.GradientDrawable.OVAL
					setColor(android.graphics.Color.parseColor(hex))
				}
				layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
					marginEnd = dp(12)
				}
				isClickable = true
				isFocusable = true
				setOnClickListener {
					RichTextUtils.applyColor(
						editMemo.text,
						range.first,
						range.second,
						Color.parseColor(hex)
					)
					dialog.dismiss()
				}
			}
			row.addView(swatch)
		}

		dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("글자 색")
			.setView(row)
			.setNegativeButton("취소", null)
			.show()
	}

	/** 서식을 적용할 선택 영역을 확인한다. 선택이 없으면 안내 토스트를 띄우고 null을 반환한다. */
	private fun requireSelection(): Pair<Int, Int>? {
		val start = editMemo.selectionStart
		val end = editMemo.selectionEnd
		if (start == end || start < 0 || end < 0) {
			Toast.makeText(this, "서식을 적용할 텍스트를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
			return null
		}
		return minOf(start, end) to maxOf(start, end)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private fun save(title: String, memo: CharSequence) {
		if (title.isEmpty()) {
			Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
			return
		}
		val preacherId = selectedPreacherId
		if (preacherId == null) {
			Toast.makeText(this, "설교자를 선택해주세요", Toast.LENGTH_SHORT).show()
			return
		}
		val memoText = RichTextUtils.toStorageString(memo)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			val sermonId: Long
			val current = existingSermon
			if (current == null) {
				sermonId = db.sermonDao().insert(
					Sermon(
						title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memoText
					)
				)
			} else {
				sermonId = current.id
				db.sermonDao().update(
					current.copy(
						title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
						categoryId = selectedCategoryId, memo = memoText
					)
				)
				db.sermonBibleRefDao().deleteBySermon(sermonId)
			}

			if (bibleRefs.isNotEmpty()) {
				db.sermonBibleRefDao().insertAll(bibleRefs.map { it.copy(sermonId = sermonId) })
			}

			db.sermonPhotoDao().deleteBySermon(sermonId)
			if (photoPaths.isNotEmpty()) {
				db.sermonPhotoDao().insertAll(
					photoPaths.mapIndexed { index, path ->
						SermonPhoto(sermonId = sermonId, filePath = path, sortOrder = index)
					}
				)
			}

			setResult(Activity.RESULT_OK)
			finish()
		}
	}

	private fun updateDateText() {
		val label = DateUtils.formatDate(selectedDateMillis)
		val spannable = SpannableString(label)
		spannable.setSpan(
			UnderlineSpan(),
			0, label.length,
			Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
		)
		btnDate.text = spannable
	}
}