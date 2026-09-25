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
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import coil.load
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.data.sermon.sermonphoto.SermonPhoto
import com.chan.bnote.data.sermon.sermonphoto.SermonPhotoStorage
import com.chan.bnote.ui.bible.picker.BibleRangePickerBottomSheet
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Calendar

class AddSermonActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_SERMON_ID = "extra_sermon_id"
		private const val EXTRA_INITIAL_DATE_MILLIS = "extra_initial_date_millis"
		private const val MAX_PHOTOS = 5
		private const val AUTO_SAVE_DELAY_MS = 3000L

		// 화면 회전이나 백그라운드에서 시스템이 메모리를 확보하려고 이 화면을 다시 만들 때도,
		// "새로 쓰던 중 자동 저장으로 이미 DB에 생긴 설교"를 잊지 않도록 여기 같이 저장해둔다.
		private const val KEY_SAVED_SERMON_ID = "saved_sermon_id"

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
	private var isEditMode = false

	// 수정 모드에서 뒤로가기 눌렀을 때 "진짜로 뭔가 바뀌었는지" 비교하기 위한 원본 값들.
	private var originalTitle = ""
	private var originalMemo = ""
	private var originalLink = ""
	private var originalDateMillis = 0L
	private var originalCategoryId: Long? = null
	private var originalPreacherId: Long? = null
	private var originalRefsSignature = ""
	private var originalPhotoPaths: List<String> = emptyList()
	private var pendingCaptureFile: File? = null

	// 타이핑하다가(또는 앱이 갑자기 죽는 등으로) 내용이 날아가는 걸 막기 위한 자동 저장.
	// 입력이 멈추고 AUTO_SAVE_DELAY_MS만큼 지나면 조용히(화면 이동 없이) 저장한다.
	private var autoSaveJob: kotlinx.coroutines.Job? = null

	// persistSermon()은 "기존 설교가 없으면 새로 만들고, 있으면 수정한다"를 코루틴 시작 시점에 판단한다.
	// 저장이 걸리는 경로가 셋(타이핑 후 디바운스 / 사진 추가 직후 / onPause 즉시 저장)이라,
	// 겹치는 타이밍에 두 저장이 동시에 시작되면 둘 다 "기존 설교 없음"으로 보고 각자 새 설교를 하나씩 만들어버릴 수 있었다
	// (사진 추가 버튼을 눌러 갤러리/카메라가 뜨는 순간 onPause가 불리는데, 그때 마침 디바운스 타이머도 끝나면 두 저장이 동시에 시작됨 — 설교와 사진이 두 벌 생기던 원인).
	// 이 Mutex로 persistSermon() 전체를 감싸서, 실제 DB 판단·쓰기는 항상 한 번에 하나씩만 실행되게 한다.
	private val persistMutex = kotlinx.coroutines.sync.Mutex()

	private lateinit var flexboxRefs: com.google.android.flexbox.FlexboxLayout
	private lateinit var btnPickPreacher: TextView
	private lateinit var btnPickCategory: TextView
	private lateinit var btnDate: TextView
	private lateinit var btnAddPhoto: TextView
	private lateinit var scrollPhotos: View
	private lateinit var photoContainer: LinearLayout
	private lateinit var editMemo: EditText
	private lateinit var editTitle: EditText
	private lateinit var editLink: EditText

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
		scheduleAutoSave()
	}

	private val takePictureLauncher = registerForActivityResult(
		ActivityResultContracts.TakePicture()
	) { success ->
		val file = pendingCaptureFile
		pendingCaptureFile = null
		if (success && file != null) {
			photoPaths.add(file.absolutePath)
			renderPhotoThumbnails()
			scheduleAutoSave()
		}
	}

	private val cameraPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		if (granted) launchCamera() else {
			Toast.makeText(this, "카메라 권한이 필요해요", Toast.LENGTH_SHORT).show()
		}
	}

	override fun onPause() {
		super.onPause()
		// 화면이 백그라운드로 가는 순간(다른 앱으로 전환, 홈 버튼 등) 바로 한 번 저장을 시도한다 —
		// 그 사이에 시스템이 프로세스를 종료해버려도 최소한 이 시점까지 쓴 내용은 남아있게 하기
		// 위함이다. 디바운스(3초 대기)까지 기다리지 않고 즉시 실행한다.
		val title = editTitle.text.toString().trim()
		val preacherId = selectedPreacherId
		if (title.isNotEmpty() && preacherId != null && hasUnsavedContent()) {
			autoSaveJob?.cancel()
			lifecycleScope.launch {
				persistSermon(title, editMemo.text, editLink.text.toString().trim(), preacherId)
			}
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

		val sermonId = savedInstanceState?.getLong(KEY_SAVED_SERMON_ID, -1L)
			?.takeIf { it != -1L }
			?: intent.getLongExtra(EXTRA_SERMON_ID, -1L)
		isEditMode = sermonId != -1L

		if (intent.hasExtra(EXTRA_INITIAL_DATE_MILLIS)) {
			selectedDateMillis = intent.getLongExtra(EXTRA_INITIAL_DATE_MILLIS, selectedDateMillis)
		}

		findViewById<TextView>(R.id.text_top_bar_title).text =
			if (isEditMode) "설교 노트 수정" else "설교 노트 작성"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { handleBackPress() }
		onBackPressedDispatcher.addCallback(
			this,
			object : androidx.activity.OnBackPressedCallback(true) {
				override fun handleOnBackPressed() {
					handleBackPress()
				}
			})

		editTitle = findViewById(R.id.edit_title)
		editMemo = findViewById(R.id.edit_memo)
		com.chan.bnote.ui.common.TextAutoReplace.attachArrowReplacement(editMemo)
		// 블루투스/물리 키보드를 연결했을 때, 드래그로 선택한 뒤 Ctrl+B/Ctrl+U로도 굵게/밑줄을 바로
		// 적용할 수 있게 한다(툴바 버튼 누르는 것과 동일하게 동작).
		editMemo.setOnKeyListener { _, keyCode, event ->
			if (event.action == android.view.KeyEvent.ACTION_DOWN && event.isCtrlPressed) {
				when (keyCode) {
					android.view.KeyEvent.KEYCODE_B -> {
						applyFormatting(bold = true)
						true
					}

					android.view.KeyEvent.KEYCODE_U -> {
						applyFormatting(bold = false)
						true
					}

					else -> false
				}
			} else {
				false
			}
		}
		editLink = findViewById(R.id.edit_sermon_link)

		val autoSaveWatcher = object : android.text.TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: android.text.Editable?) {
				scheduleAutoSave()
			}
		}
		editTitle.addTextChangedListener(autoSaveWatcher)
		editMemo.addTextChangedListener(autoSaveWatcher)
		editLink.addTextChangedListener(autoSaveWatcher)
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
					scheduleAutoSave()
				},
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
			).show()
		}

		btnPickPreacher.setOnClickListener {
			val picker = PreacherPickerBottomSheet()
			picker.onPreacherSelected = { preacher ->
				selectedPreacherId = preacher.id
				btnPickPreacher.text = preacher.name
				scheduleAutoSave()
			}
			picker.show(supportFragmentManager, "preacher_picker")
		}

		btnPickCategory.setOnClickListener {
			val picker = CategoryPickerBottomSheet()
			picker.onCategorySelected = { category ->
				selectedCategoryId = category?.id
				btnPickCategory.text = category?.name ?: "카테고리 선택"
				scheduleAutoSave()
			}
			picker.show(supportFragmentManager, "category_picker")
		}

		btnAddPhoto.setOnClickListener { showPhotoSourceMenu(it) }

		findViewById<TextView>(R.id.btn_save_sermon).setOnClickListener {
			save(editTitle.text.toString().trim(), editMemo.text, editLink.text.toString().trim())
		}

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			if (isEditMode) {
				val sermon = db.sermonDao().getById(sermonId)
				existingSermon = sermon
				if (sermon != null) {
					editTitle.setText(sermon.title)
					editMemo.setText(RichTextUtils.toEditable(sermon.memo))
					editLink.setText(sermon.link ?: "")
					selectedDateMillis = sermon.sermonDate
					selectedCategoryId = sermon.categoryId
					selectedPreacherId = sermon.preacherId
					updateDateText()

					bibleRefs.addAll(db.sermonBibleRefDao().getBySermon(sermon.id))
					renderBibleRefBoxes()

					photoPaths.addAll(
						db.sermonPhotoDao().getBySermon(sermon.id).map { it.filePath })
					renderPhotoThumbnails()

					// 뒤로가기 시 "진짜로 뭔가 바뀌었는지" 비교하기 위한 원본 스냅샷.
					originalTitle = sermon.title
					originalMemo = sermon.memo
					originalLink = sermon.link ?: ""
					originalDateMillis = sermon.sermonDate
					originalCategoryId = sermon.categoryId
					originalPreacherId = sermon.preacherId
					originalRefsSignature = refsSignature(bibleRefs)
					originalPhotoPaths = photoPaths.toList()
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

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		existingSermon?.let { outState.putLong(KEY_SAVED_SERMON_ID, it.id) }
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
		btnAddPhoto.text = "사진 추가 (${photoPaths.size}/$MAX_PHOTOS)"
		photoContainer.removeAllViews()
		scrollPhotos.visibility = if (photoPaths.isEmpty()) View.GONE else View.VISIBLE

		for (path in photoPaths) {
			val thumb = LayoutInflater.from(this)
				.inflate(R.layout.item_sermon_photo_thumbnail, photoContainer, false)
			thumb.findViewById<ImageView>(R.id.image_photo_thumbnail).load(File(path))
			thumb.findViewById<ImageView>(R.id.btn_remove_photo).setOnClickListener {
				photoPaths.remove(path)
				renderPhotoThumbnails()
				scheduleAutoSave()
			}
			photoContainer.addView(thumb)
		}
	}

	private fun renderBibleRefBoxes() {
		flexboxRefs.removeAllViews()

		if (bibleRefs.isEmpty()) {
			flexboxRefs.addView(buildRefBox("본문 선택", fullWidth = true) {
				openBibleRangePicker(
					existing = null
				)
			})
			return
		}

		for (i in 0 until bibleRefs.size - 1) {
			val ref = bibleRefs[i]
			flexboxRefs.addView(
				buildRefBox(ref.toDisplayLabel(), fullWidth = false) {
					openBibleRangePicker(existing = ref)
				}
			)
		}

		// 마지막 본문 박스는 "+" 버튼과 하나의 묶음(LinearLayout)으로 만들어서 flexbox에 통째로
		// 하나의 항목으로 넣는다 — flexbox는 항목 하나하나를 따로 줄바꿈 여부를 판단하므로, 그냥
		// 나열만 하면 줄이 애매하게 남았을 때 "+" 버튼만 혼자 다음 줄로 떨어질 수 있다. 묶어두면
		// 그 둘이 항상 같이 다니고(줄이 부족하면 묶음 전체가 다음 줄로 넘어감), 앞줄에 남는
		// 박스들도 "+" 버튼 몫까지 정확히 계산해서 그 줄을 끝까지 채울 수 있다.
		val lastRef = bibleRefs.last()
		val lastBox = buildRefBox(lastRef.toDisplayLabel(), fullWidth = false) {
			openBibleRangePicker(existing = lastRef)
		}
		val addButton = buildAddSquareButton { openBibleRangePicker(existing = null) }

		// buildRefBox/buildAddSquareButton이 만들어준 FlexboxLayout.LayoutParams는 이 묶음
		// 안(일반 LinearLayout)에서는 그대로 안 통하므로, 여기서 실제로 쓰일
		// LinearLayout.LayoutParams로 새로 지정한다. lastBox는 폭을 0dp로 두지 않고
		// wrap_content + weight=1로 둬서 "일단 제 글자 크기만큼은 꼭 차지하고, 묶음이 늘어나면
		// 그 늘어난 만큼만 추가로 흡수"하게 한다(0dp+weight로 두면 또 처음부터 거의 0폭으로
		// 측정돼서 "박스 안에서 줄바꿈"되는 예전 버그가 재발한다). addButton은 고정 정사각형.
		lastBox.layoutParams = LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
		).apply { marginEnd = dp(8) }
		addButton.layoutParams =
			LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			)

		val lastGroup = LinearLayout(this).apply {
			orientation = LinearLayout.HORIZONTAL
			addView(lastBox)
			addView(addButton)
			layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				flexGrow = 1f
				flexShrink = 0f
				bottomMargin = dp(8)
			}
		}
		flexboxRefs.addView(lastGroup)

		// 본문 박스들이 실제로 배치된 뒤, 그 높이에 맞춰 "+" 버튼을 정확히 정사각형으로 맞춘다.
		// flexboxRefs.getChildAt(0)을 쓰면 본문이 1개뿐일 때 그게 lastGroup 자신이 돼서(그 안에
		// 아직 정사각형으로 안 맞춰진 addButton이 포함된 채로 측정되는) 순환 참조가 생겨 버튼이
		// 점점 커지는 문제가 있었다 — 항상 실제 박스인 lastBox에서 직접 높이를 가져온다.
		//
		// post{}를 중첩해서 쓰면 "그 다음 프레임"이 실제로 레이아웃이 다 끝난 뒤라는 보장이 없다
		// (Choreographer의 레이아웃 트래버설과 Handler post()의 실행 순서가 항상 정해진 게 아님).
		// doOnPreDraw는 정확히 "이번 레이아웃이 다 끝나고 화면에 그려지기 직전"에 불려서 훨씬
		// 확실하다.
		flexboxRefs.doOnPreDraw {
			val height = lastBox.height
			if (height > 0) {
				val lp = addButton.layoutParams as LinearLayout.LayoutParams
				if (lp.height != height || lp.width != height) {
					lp.height = height
					lp.width = height
					addButton.layoutParams = lp
				}
			}
			// "+" 버튼 크기가 바뀌면 레이아웃이 다시 잡히므로, 그 레이아웃이 끝난 뒤(doOnPreDraw를
			// 한 번 더 걸어서) 각 줄이 실제로 얼마나 채워졌는지 보고 남는 공간을 직접 나눠준다.
			flexboxRefs.doOnPreDraw { fillRefBoxLinesCompletely() }
		}
	}

	/** flexboxRefs 안의 항목들을 실제 배치된 y좌표(top) 기준으로 줄별로 묶은 뒤, 각 줄에 남는
	 * 폭이 있으면 그 줄 안의 flexGrow 항목들(본문 박스들·마지막 묶음)에게 균등하게 나눠 더해서
	 * 줄 전체가 끝까지 채워지도록 만든다. */
	private fun fillRefBoxLinesCompletely() {
		val containerWidth = flexboxRefs.width
		if (containerWidth <= 0 || flexboxRefs.childCount == 0) return

		val children = (0 until flexboxRefs.childCount).map { flexboxRefs.getChildAt(it) }
		// top이 완전히 똑같은 값끼리만 묶으면, 같은 줄인데도 높이가 미세하게(1~2px) 다른 항목이
		// 있을 때(예: "+" 버튼이 포함된 마지막 묶음은 alignItems=center로 인해 top이 살짝 다를 수
		// 있음) 서로 다른 줄로 잘못 나뉠 수 있다. 8px 오차까지는 같은 줄로 본다.
		val sorted = children.sortedBy { it.top }
		val lines = mutableListOf<MutableList<View>>()
		val lineTolerancePx = dp(4)
		for (child in sorted) {
			val currentLine = lines.lastOrNull()
			if (currentLine != null && kotlin.math.abs(child.top - currentLine.first().top) <= lineTolerancePx) {
				currentLine.add(child)
			} else {
				lines.add(mutableListOf(child))
			}
		}

		for (line in lines) {
			// 이 줄에서 실제로 가장 오른쪽에 있는(=마지막) 박스를 찾는다. 설교자/카테고리 행에서
			// 마지막인 "카테고리" 박스는 애초에 marginEnd가 없어서 행 끝까지 닿는데, 본문 박스는
			// 줄바꿈이 동적이라 어떤 박스가 마지막이 될지 만들 때는 알 수 없어서 전부 똑같이
			// marginEnd(8dp)를 갖고 있다 — 그래서 그 박스를 늘려도 뒤에 marginEnd만큼의 여백은
			// (배경색 밖의 공간이라) 그대로 남아있었다. 여기서 실제 마지막 박스를 찾아 그
			// marginEnd를 0으로 만들어 없애준다.
			val lastInLine = line.maxByOrNull { it.right }

			val growable = line.filter {
				val lp = it.layoutParams as? com.google.android.flexbox.FlexboxLayout.LayoutParams
				(lp?.flexGrow ?: 0f) > 0f
			}
			if (growable.isEmpty()) continue

			val usedWidth = line.sumOf { child ->
				val lp = child.layoutParams as com.google.android.flexbox.FlexboxLayout.LayoutParams
				val effectiveMarginEnd = if (child === lastInLine) 0 else lp.marginEnd
				child.width + lp.marginStart + effectiveMarginEnd
			}
			val extra = containerWidth - usedWidth
			if (extra <= 0) {
				// 늘릴 공간은 없어도, 마지막 박스의 marginEnd는 그대로 없애줘야 한다(원래도
				// 정확히 꽉 차 있었을 뿐 여백이 필요 없는 경우).
				if (lastInLine != null) removeMarginEnd(lastInLine)
				continue
			}

			val perItem = extra / growable.size
			growable.forEachIndexed { index, child ->
				val lp = child.layoutParams as com.google.android.flexbox.FlexboxLayout.LayoutParams
				// 나머지(나눗셈 오차)는 마지막 항목이 떠안아서, 합계가 정확히 남는 폭과 같아지게 한다.
				val addAmount =
					if (index == growable.lastIndex) extra - perItem * (growable.size - 1) else perItem
				lp.width = child.width + addAmount
				if (child === lastInLine) lp.marginEnd = 0
				child.layoutParams = lp
			}
		}
	}

	private fun removeMarginEnd(view: View) {
		val lp =
			view.layoutParams as? com.google.android.flexbox.FlexboxLayout.LayoutParams ?: return
		if (lp.marginEnd != 0) {
			lp.marginEnd = 0
			view.layoutParams = lp
		}
	}

	private fun openBibleRangePicker(existing: SermonBibleRef?) {
		val rangePicker = BibleRangePickerBottomSheet()
		rangePicker.existingRef = existing
		rangePicker.onRangeSelected = { ref ->
			if (existing != null) {
				val index = bibleRefs.indexOf(existing)
				if (index != -1) bibleRefs[index] = ref else bibleRefs.add(ref)
			} else {
				bibleRefs.add(ref)
			}
			renderBibleRefBoxes()
			scheduleAutoSave()
		}
		rangePicker.onDeleteRequested = {
			bibleRefs.remove(existing)
			renderBibleRefBoxes()
			scheduleAutoSave()
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
			setPadding(dp(12), dp(8), dp(12), dp(8))
			setTextColor(ContextCompat.getColor(this@AddSermonActivity, R.color.text_primary))
			background =
				ContextCompat.getDrawable(this@AddSermonActivity, R.drawable.bg_book_button)
			isClickable = true
			isFocusable = true
			layoutParams = com.google.android.flexbox.FlexboxLayout.LayoutParams(
				if (fullWidth) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT
			).apply {
				// width가 wrap_content라서(= flexBasis가 "이 박스의 원래 텍스트 크기") flexGrow를
				// 다시 켜도 안전하다 — 전에 "박스 안에서 텍스트가 줄바꿈"되던 버그는 width를
				// 0dp로 뒀을 때(flexbox가 박스를 먼저 거의 0폭으로 측정한 뒤 늘리는 방식)만
				// 생기던 문제였다. flexGrow=1로 두면, 한 줄에 들어간 박스들이 그 줄의 남는
				// 공간을("+" 버튼은 flexGrow가 없어 고정 크기라 자연히 제외되고) 서로 나눠
				// 가지면서 줄 전체를 꽉 채운다. flexShrink=0은 그대로 둬서, 그래도 안 맞으면
				// 줄어드는 대신 다음 줄로 넘어간다.
				flexGrow = 1f
				flexShrink = 0f
				// 설교자/카테고리 박스는 각각 marginEnd 4dp + marginStart 4dp를 따로 갖고 있어서
				// (안드로이드는 마진을 자동으로 안 합쳐주므로) 실제 간격이 8dp다. 본문 박스는
				// marginEnd만 있었어서 그 절반(4dp)밖에 안 됐던 것 — 8dp로 맞춘다. 마지막 본문
				// 박스의 marginEnd가 곧 "+" 버튼과의 간격이므로 그쪽도 같이 맞춰진다. 단,
				// fullWidth(본문을 아직 하나도 안 골랐을 때의 "본문 선택" 박스 하나뿐인 경우)는
				// 뒤에 다른 박스가 없으니 marginEnd를 주면 오른쪽에 불필요한 공백만 생긴다.
				if (!fullWidth) marginEnd = dp(8)
				bottomMargin = dp(8)
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
		// setSpan()은 TextWatcher를 안 부르므로(글자 내용 자체는 안 바뀌니까), 여기서 직접 예약한다.
		scheduleAutoSave()
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
					scheduleAutoSave()
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

	private fun refsSignature(refs: List<SermonBibleRef>): String =
		refs.joinToString("|") {
			"${it.startBookId}-${it.startChapter}-${it.startVerse}-${it.endBookId}-${it.endChapter}-${it.endVerse}"
		}

	private fun hasUnsavedContent(): Boolean {
		// 자동 저장 덕분에 "새로 작성 중"이던 설교도 중간에 한 번 저장돼서 existingSermon이 생길
		// 수 있다 — 그 뒤로는(원래부터 수정 모드였든, 자동 저장으로 방금 생겼든) 항상 "마지막으로
		// 저장된 상태"와 비교해야 한다. 아직 한 번도 저장된 적 없을 때만 단순히 "뭐라도 썼는지"를 본다.
		if (existingSermon == null) {
			return editTitle.text.toString().isNotBlank() ||
					editMemo.text.toString().isNotBlank() ||
					editLink.text.toString().isNotBlank() ||
					bibleRefs.isNotEmpty() ||
					photoPaths.isNotEmpty() ||
					selectedCategoryId != null ||
					selectedPreacherId != null
		}
		// 원본(또는 마지막 자동 저장 시점)과 실제로 달라진 게 있을 때만 "저장 안 된 변경사항"으로 본다.
		return editTitle.text.toString().trim() != originalTitle ||
				RichTextUtils.toStorageString(editMemo.text) != originalMemo ||
				editLink.text.toString().trim() != originalLink ||
				selectedDateMillis != originalDateMillis ||
				selectedCategoryId != originalCategoryId ||
				selectedPreacherId != originalPreacherId ||
				refsSignature(bibleRefs) != originalRefsSignature ||
				photoPaths != originalPhotoPaths
	}

	private fun handleBackPress() {
		if (!hasUnsavedContent()) {
			// 자동 저장 덕분에, "지금은 더 바뀐 게 없어도" 이 화면에 머무는 동안 이미 DB에 실제로
			// 저장이 됐을 수 있다 — 그런 경우 호출한 화면(설교 상세 등)이 최신 내용으로 새로고침
			// 되도록 RESULT_OK를 같이 넘겨준다.
			if (existingSermon != null) setResult(Activity.RESULT_OK)
			finish()
			return
		}
		com.chan.bnote.ui.common.UnsavedChangesDialog.show(
			context = this,
			onDiscard = {
				if (existingSermon != null) setResult(Activity.RESULT_OK)
				finish()
			}
		)
	}

	private fun save(
		title: String,
		memo: CharSequence,
		link: String,
		exitAfterSave: Boolean = true
	) {
		if (title.isEmpty()) {
			Toast.makeText(this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
			return
		}
		val preacherId = selectedPreacherId
		if (preacherId == null) {
			Toast.makeText(this, "설교자를 선택해주세요", Toast.LENGTH_SHORT).show()
			return
		}
		// 수동으로(저장 버튼이든 Ctrl+S든) 바로 저장을 실행하는 경우, 뒤이어 자동으로 또 한 번
		// 저장이 실행되지 않도록 예약해둔 자동 저장은 취소한다.
		autoSaveJob?.cancel()

		lifecycleScope.launch {
			val sermonId = persistSermon(title, memo, link, preacherId)

			if (exitAfterSave) {
				setResult(Activity.RESULT_OK)
				if (isNewlyCreated) {
					// 새로 작성한 설교는 어디서 시작했든(캘린더 등) 그 설교의 상세 화면으로 바로
					// 이동해줘야 하므로 새로 연다.
					SermonDetailActivity.start(this@AddSermonActivity, sermonId)
				}
				// 수정인 경우엔 항상 설교 상세 화면(editSermonLauncher)에서 열렸으므로, 여기서 새
				// 상세 화면을 또 띄우면 뒤로가기 시 상세 화면이 중복으로 쌓인다. RESULT_OK로 finish만
				// 하면 원래 상세 화면이 자기 자신을 다시 불러온다(editSermonLauncher 콜백 참고).
				finish()
			} else {
				Toast.makeText(this@AddSermonActivity, "저장했어요", Toast.LENGTH_SHORT).show()
			}
		}
	}

	/** 실제로 DB에 쓰는 부분만 따로 뗀 것. 자동 저장·Ctrl+S·저장 버튼이 전부 이 함수를 공유한다.
	 * 저장이 끝나면 existingSermon과 "원본" 비교값들을 지금 상태로 갱신해서, 이 화면을 계속
	 * 붙잡고 있는 동안 (1) 다음 저장은 새로 만들지 않고 이어서 수정하고 (2) hasUnsavedContent()가
	 * "방금 자동 저장된 상태"를 기준으로 다시 비교하도록 한다.
	 *
	 * persistMutex로 전체를 감싸는 이유는 위 persistMutex 필드 선언부 주석 참고 — 겹치는 저장
	 * 요청이 동시에 "기존 설교 없음"으로 판단해 각자 insert해버리는 걸 막는다. */
	private var isNewlyCreated = false

	private suspend fun persistSermon(
		title: String,
		memo: CharSequence,
		link: String,
		preacherId: Long
	): Long = persistMutex.withLock {
		val memoText = RichTextUtils.toStorageString(memo)
		// 검색용 순수 텍스트. memo(CharSequence)는 서식(스팬)이 있어도 .toString()하면 글자만 남는다 — 아직 HTML로 저장되기 전이라 문자 참조로 안 바뀐, 진짜 글자 그대로다.
		val memoSearchText = memo.toString()
		val linkValue = link.trim().ifEmpty { null }
		val db = BibleDatabase.getInstance(applicationContext)

		val sermonId: Long
		val current = existingSermon
		if (current == null) {
			sermonId = db.sermonDao().insert(
				Sermon(
					title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
					categoryId = selectedCategoryId, memo = memoText,
					memoSearchText = memoSearchText, link = linkValue
				)
			)
			isNewlyCreated = true
		} else {
			sermonId = current.id
			db.sermonDao().update(
				current.copy(
					title = title, preacherId = preacherId, sermonDate = selectedDateMillis,
					categoryId = selectedCategoryId, memo = memoText,
					memoSearchText = memoSearchText, link = linkValue
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

		// 방금 저장한 내용을 새 "기준값"으로 삼는다 — 그래야 이 화면에 계속 머무는 동안(자동 저장
		// 등으로) 다음 비교/저장이 지금 막 저장한 상태를 기준으로 정확히 이어진다.
		existingSermon = db.sermonDao().getById(sermonId)
		originalTitle = title
		originalMemo = memoText
		originalLink = linkValue ?: ""
		originalDateMillis = selectedDateMillis
		originalCategoryId = selectedCategoryId
		originalPreacherId = preacherId
		originalRefsSignature = refsSignature(bibleRefs)
		originalPhotoPaths = photoPaths.toList()

		return sermonId
	}

	/** 입력이 멈추고 AUTO_SAVE_DELAY_MS만큼 지나면 조용히(화면 이동·안내 없이) 저장한다.
	 * 제목·설교자처럼 저장에 꼭 필요한 값이 아직 없으면(작성 극초반) 건너뛰고, 저장할 내용이
	 * 실제로 없으면(원본과 차이 없음) 굳이 다시 안 쓴다. */
	private fun scheduleAutoSave() {
		autoSaveJob?.cancel()
		autoSaveJob = lifecycleScope.launch {
			kotlinx.coroutines.delay(AUTO_SAVE_DELAY_MS)
			val title = editTitle.text.toString().trim()
			val preacherId = selectedPreacherId
			if (title.isEmpty() || preacherId == null) return@launch
			if (!hasUnsavedContent()) return@launch
			persistSermon(title, editMemo.text, editLink.text.toString().trim(), preacherId)
		}
	}

	override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
		// 물리/블루투스 키보드에서 Ctrl+S를 누르면, 화면을 나가지 않고 그 자리에서 바로 저장한다
		// (일반 저장 버튼은 저장 후 상세 화면으로 이동하지만, 이건 계속 이어서 쓸 수 있게 그대로 둔다).
		if (event.action == android.view.KeyEvent.ACTION_DOWN &&
			event.isCtrlPressed &&
			event.keyCode == android.view.KeyEvent.KEYCODE_S
		) {
			save(
				editTitle.text.toString().trim(),
				editMemo.text,
				editLink.text.toString().trim(),
				exitAfterSave = false
			)
			return true
		}
		return super.dispatchKeyEvent(event)
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