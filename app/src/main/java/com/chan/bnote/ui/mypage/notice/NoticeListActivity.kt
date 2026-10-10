package com.chan.bnote.ui.mypage.notice

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chan.bnote.R
import com.chan.bnote.data.notice.Notice
import com.chan.bnote.data.notice.NoticeFormatter
import com.chan.bnote.data.notice.NoticeRepository
import com.chan.bnote.data.notice.NoticeType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 알림(공지사항) 목록. 마이페이지 상단바의 알림 아이콘으로 들어온다.
 *
 * - 위쪽 칩으로 전체 / 안 읽음 / 종류별로 걸러 볼 수 있다. 종류 칩은 업데이트만 항상 보이고, 나머지 종류는 그 종류의 알림이 하나라도 있을 때만 보인다.
 * - 항목을 누르면 상세 화면(읽음 처리됨), 길게 누르거나 "편집"을 누르면 선택 모드.
 * - 선택 모드: 전체 선택, 고른 것 읽음 처리, 고른 것 삭제(이 기기에서만).
 * - "모두 읽음"으로 한 번에 읽음 처리, 아래로 당기면 새로고침.
 * - 열 때마다 저장된 목록을 먼저 보여주고, 바로 GitHub에서 새로 가져와 갱신한다.
 */
class NoticeListActivity : AppCompatActivity() {

	/** 필터 칩: null = 전체, UNREAD = 안 읽음, 그 외 = 해당 타입만. */
	private sealed class Filter(val label: String) {
		object All : Filter("전체")
		object Unread : Filter("안 읽음")
		class Type(val type: NoticeType) : Filter(type.displayName)
	}

	/** 알림이 없어도 항상 보여줄 종류 칩. 나머지 종류는 알림이 있을 때만 보인다. */
	private val alwaysShownTypes = setOf(NoticeType.UPDATE)

	private var currentFilter: Filter = Filter.All

	private var allNotices: List<Notice> = emptyList()
	private var isSelectionMode = false
	private val selectedIds = mutableSetOf<Long>()

	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyText: TextView
	private lateinit var swipeRefresh: SwipeRefreshLayout
	private lateinit var titleText: TextView
	private lateinit var backButton: ImageView
	private lateinit var btnReadAll: TextView
	private lateinit var btnEdit: TextView
	private lateinit var btnSelectAll: TextView
	private lateinit var selectionBar: View
	private lateinit var filterContainer: LinearLayout
	private val adapter = NoticeAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_notice_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.notice_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		recyclerView = findViewById(R.id.recycler_notices)
		emptyText = findViewById(R.id.text_notice_empty)
		swipeRefresh = findViewById(R.id.swipe_refresh_notices)
		titleText = findViewById(R.id.text_top_bar_title)
		backButton = findViewById(R.id.btn_top_bar_back)
		btnReadAll = findViewById(R.id.btn_notice_read_all)
		btnEdit = findViewById(R.id.btn_notice_edit)
		btnSelectAll = findViewById(R.id.btn_notice_select_all)
		selectionBar = findViewById(R.id.bar_notice_selection_actions)
		filterContainer = findViewById(R.id.container_notice_filters)

		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = adapter

		// 목록이 FrameLayout 안에 있어서, 목록을 내려 둔 상태에서 위로 스크롤하려는 걸 새로고침으로
		// 착각하지 않게 "목록이 더 위로 올라갈 수 있으면 새로고침 아님"을 직접 알려준다.
		swipeRefresh.setOnChildScrollUpCallback { _, _ -> recyclerView.canScrollVertically(-1) }
		swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.brown_text))
		swipeRefresh.setOnRefreshListener { refreshFromServer(showFailure = true) }

		backButton.setOnClickListener { handleBack() }
		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() = handleBack()
		})
		btnReadAll.setOnClickListener {
			NoticeRepository.markAllRead(this)
			render()
		}
		btnEdit.setOnClickListener { setSelectionMode(true) }
		btnSelectAll.setOnClickListener {
			val visibleIds = filteredNotices().map { it.id }
			if (visibleIds.isNotEmpty() && selectedIds.containsAll(visibleIds)) {
				selectedIds.removeAll(visibleIds.toSet())
			} else {
				selectedIds.addAll(visibleIds)
			}
			render()
		}
		findViewById<TextView>(R.id.btn_notice_mark_selected_read).setOnClickListener {
			val targets = allNotices.filter { it.id in selectedIds }
			if (targets.isEmpty()) return@setOnClickListener
			NoticeRepository.markRead(this, targets)
			setSelectionMode(false)
		}
		findViewById<TextView>(R.id.btn_notice_delete_selected).setOnClickListener {
			confirmDeleteSelected()
		}

		reloadFromCache()
		refreshFromServer(showFailure = false)
	}

	override fun onResume() {
		super.onResume()
		// 상세 화면에서 읽거나 삭제하고 돌아왔을 수 있으니 저장된 상태를 다시 반영한다.
		reloadFromCache()
	}

	private fun handleBack() {
		if (isSelectionMode) setSelectionMode(false) else finish()
	}

	private fun reloadFromCache() {
		allNotices = NoticeRepository.visibleNotices(this)
		selectedIds.retainAll(allNotices.map { it.id }.toSet())
		// 보고 있던 종류의 알림이 다 없어져서 그 칩이 사라지면 "전체"로 돌아간다.
		val current = currentFilter
		if (current is Filter.Type && current.type !in visibleTypes()) currentFilter = Filter.All
		renderFilters()
		render()
	}

	/** 칩으로 보여줄 종류: 항상 보이는 종류 + 알림이 하나라도 있는 종류(정의된 순서대로). */
	private fun visibleTypes(): List<NoticeType> {
		val present = allNotices.map { it.type }.toSet()
		return NoticeType.entries.filter { it in alwaysShownTypes || it in present }
	}

	private fun visibleFilters(): List<Filter> =
		listOf(Filter.All, Filter.Unread) + visibleTypes().map { Filter.Type(it) }

	private fun refreshFromServer(showFailure: Boolean) {
		swipeRefresh.isRefreshing = true
		lifecycleScope.launch {
			val ok = NoticeRepository.refresh(this@NoticeListActivity)
			swipeRefresh.isRefreshing = false
			if (ok) {
				reloadFromCache()
			} else if (showFailure) {
				Toast.makeText(
					this@NoticeListActivity,
					"알림을 불러오지 못했어요. 인터넷 연결을 확인해 주세요",
					Toast.LENGTH_SHORT
				).show()
			}
		}
	}

	private fun filteredNotices(): List<Notice> = when (val f = currentFilter) {
		Filter.All -> allNotices
		Filter.Unread -> allNotices.filter { !NoticeRepository.isRead(this, it) }
		is Filter.Type -> allNotices.filter { it.type == f.type }
	}

	private fun setSelectionMode(enabled: Boolean) {
		isSelectionMode = enabled
		selectedIds.clear()
		// 선택 중엔 당겨서 새로고침을 막는다(목록이 바뀌면 고른 게 어긋날 수 있어서).
		swipeRefresh.isEnabled = !enabled
		render()
	}

	private fun render() {
		val list = filteredNotices()
		adapter.submit(list)
		emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
		emptyText.text = when (currentFilter) {
			Filter.Unread -> "안 읽은 알림이 없어요"
			is Filter.Type -> "이 종류의 알림이 없어요"
			Filter.All -> "알림이 없어요"
		}

		if (isSelectionMode) {
			titleText.text = "${selectedIds.size}개 선택"
			backButton.setImageResource(R.drawable.ic_close)
			backButton.contentDescription = "선택 취소"
			btnReadAll.visibility = View.GONE
			btnEdit.visibility = View.GONE
			btnSelectAll.visibility = View.VISIBLE
			val allSelected = list.isNotEmpty() && selectedIds.containsAll(list.map { it.id })
			btnSelectAll.text = if (allSelected) "선택 해제" else "전체 선택"
			selectionBar.visibility = View.VISIBLE
		} else {
			titleText.text = "알림"
			backButton.setImageResource(R.drawable.ic_chevron_left)
			backButton.contentDescription = "뒤로가기"
			val hasUnread = allNotices.any { !NoticeRepository.isRead(this, it) }
			btnReadAll.visibility = if (hasUnread) View.VISIBLE else View.GONE
			btnEdit.visibility = if (allNotices.isNotEmpty()) View.VISIBLE else View.GONE
			btnSelectAll.visibility = View.GONE
			selectionBar.visibility = View.GONE
		}
	}

	private fun renderFilters() {
		filterContainer.removeAllViews()
		val density = resources.displayMetrics.density
		for (filter in visibleFilters()) {
			val chip = TextView(this).apply {
				text = filter.label
				textSize = 13f
				setPadding(
					(14 * density).toInt(),
					(6 * density).toInt(),
					(14 * density).toInt(),
					(6 * density).toInt()
				)
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { marginEnd = (8 * density).toInt() }
				isClickable = true
				isFocusable = true
				setOnClickListener {
					currentFilter = filter
					renderFilters()
					render()
				}
			}
			val selected = filter::class == currentFilter::class &&
					(filter !is Filter.Type || filter.type == (currentFilter as Filter.Type).type)
			chip.background = ContextCompat.getDrawable(
				this, if (selected) R.drawable.bg_chip_filled else R.drawable.bg_chip_outline
			)
			chip.setTextColor(
				if (selected) ContextCompat.getColor(this, R.color.surface_background)
				else ContextCompat.getColor(this, R.color.brown_text)
			)
			filterContainer.addView(chip)
		}
	}

	private fun confirmDeleteSelected() {
		val count = selectedIds.size
		if (count == 0) return
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("알림 삭제")
			.setMessage("선택한 알림 ${count}개를 삭제할까요?\n이 기기에서만 지워지고, 다시 볼 수 없어요.")
			.setPositiveButton("삭제") { _, _ ->
				NoticeRepository.hide(this, selectedIds.toList())
				setSelectionMode(false)
				reloadFromCache()
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun onItemClicked(notice: Notice) {
		if (isSelectionMode) {
			if (!selectedIds.add(notice.id)) selectedIds.remove(notice.id)
			render()
		} else {
			startActivity(NoticeDetailActivity.createIntent(this, notice.id))
		}
	}

	private fun onItemLongClicked(notice: Notice) {
		if (!isSelectionMode) setSelectionMode(true)
		selectedIds.add(notice.id)
		render()
	}

	private inner class NoticeAdapter : RecyclerView.Adapter<NoticeAdapter.ViewHolder>() {

		private var items: List<Notice> = emptyList()

		inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val content: View = view.findViewById(R.id.row_notice_content)
			val check: CheckBox = view.findViewById(R.id.check_notice)
			val badges: LinearLayout = view.findViewById(R.id.container_notice_badges)
			val date: TextView = view.findViewById(R.id.text_notice_date)
			val title: TextView = view.findViewById(R.id.text_notice_title)
			val preview: TextView = view.findViewById(R.id.text_notice_preview)
			val unreadDot: View = view.findViewById(R.id.dot_notice_unread)
		}

		@SuppressLint("NotifyDataSetChanged")
		fun submit(list: List<Notice>) {
			items = list
			notifyDataSetChanged()
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
			ViewHolder(
				LayoutInflater.from(parent.context).inflate(R.layout.item_notice_row, parent, false)
			)

		override fun getItemCount() = items.size

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val notice = items[position]
			val context = holder.itemView.context
			val unread = !NoticeRepository.isRead(context, notice)

			NoticeBadges.fill(holder.badges, notice)
			holder.date.text = NoticeFormatter.listDate(notice.createdAt)
			holder.title.text = notice.title
			holder.title.setTypeface(null, if (unread) Typeface.BOLD else Typeface.NORMAL)
			val preview = NoticeFormatter.preview(notice.body)
			holder.preview.text = preview
			holder.preview.visibility = if (preview.isBlank()) View.GONE else View.VISIBLE
			// 읽은 알림은 점 자리를 아예 비워서(GONE) 날짜가 오른쪽 끝에 붙게 한다.
			holder.unreadDot.visibility = if (unread) View.VISIBLE else View.GONE
			holder.content.setBackgroundColor(
				if (unread) ContextCompat.getColor(context, R.color.notice_unread_background)
				else android.graphics.Color.TRANSPARENT
			)

			holder.check.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
			holder.check.isChecked = notice.id in selectedIds

			holder.content.setOnClickListener { onItemClicked(notice) }
			holder.content.setOnLongClickListener {
				onItemLongClicked(notice)
				true
			}
		}
	}
}