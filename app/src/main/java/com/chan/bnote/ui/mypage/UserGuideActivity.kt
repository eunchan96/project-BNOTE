package com.chan.bnote.ui.mypage

import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

class UserGuideActivity : AppCompatActivity() {

	private data class GuideItem(val title: String, val description: String)
	private data class GuideSection(val header: String, val items: List<GuideItem>)

	private val sections = listOf(
		GuideSection(
			"성경 읽기",
			listOf(
				GuideItem(
					"장/절 이동",
					"상단 위치 표시를 누르면 성경 · 장 · 절 순서로 탭을 오가며 원하는 곳으로 바로 이동할 수 있어요."
				),
				GuideItem(
					"대역본(함께보기)",
					"상단의 번역본 이름을 누르면 주성경과 함께 볼 대역본을 고를 수 있어요."
				),
				GuideItem(
					"형광펜 · 북마크 · 스크랩",
					"절을 길게 누르거나 드래그해서 선택하면 형광펜, 북마크, 스크랩 중 원하는 걸 남길 수 있어요. 스크랩은 그룹으로 나눠서 정리할 수 있어요."
				),
				GuideItem(
					"메모",
					"절 전체에 메모를 남기거나, 특정 단어만 드래그해서 그 단어에만 메모를 남길 수 있어요. 단어 메모는 같은 단어가 나오는 다른 구절에도 한 번에 추가할 수 있고, 한 곳에 메모를 여러 개 쌓을 수도 있어요."
				)
			)
		),
		GuideSection(
			"설교 노트",
			listOf(
				GuideItem(
					"작성",
					"제목, 본문(성경 구절), 설교자, 카테고리를 정하고 메모를 남길 수 있어요. 메모는 굵게 · 밑줄 · 색 지정도 가능하고, 사진도 최대 5장까지 첨부할 수 있어요."
				),
				GuideItem(
					"찾아보기",
					"날짜별, 설교자별로 모아보거나 제목/본문 검색으로 찾을 수 있어요. 성경을 읽다가 그 장에 관련된 설교가 있으면 표시가 떠요."
				)
			)
		),
		GuideSection(
			"찬송가",
			listOf(
				GuideItem(
					"찬송 보기",
					"성경 탭 메뉴에서 찬송을 열면 분류별로 찾아볼 수 있고, 악보 이미지와 유튜브 반주 영상을 같이 볼 수 있어요."
				)
			)
		),
		GuideSection(
			"마이페이지",
			listOf(
				GuideItem(
					"내 정보",
					"이름, 교회, 부서, 직분과 사진을 등록하고, 지금까지 쌓은 활동 기록(하이라이트/메모/설교노트 개수 등)을 한눈에 볼 수 있어요."
				),
				GuideItem(
					"성경읽기표",
					"책별로 읽은 장 수를 체크하며 통독 진도를 관리할 수 있어요. 상단바의 초기화 버튼으로 기록을 리셋할 수 있어요."
				),
				GuideItem("올해 약속의 말씀", "연도별로 올해의 말씀을 등록하고, 원하면 바로 암송 구절로 보낼 수 있어요."),
				GuideItem("기도제목 노트", "기도제목을 적어두고 응답되면 체크 표시를 남길 수 있어요."),
				GuideItem(
					"암송 구절",
					"구절을 그룹으로 나눠 등록하고, 그룹 전체 또는 구절 하나만 골라 암송 연습을 할 수 있어요. 연습 중엔 힌트 보기 기능도 있어요."
				),
				GuideItem(
					"성경 배경지식",
					"성경 탭의 메뉴(≡) 안에서 열 수 있어요. 인물사전, 지명사전, 족보, 연대표, 상황별 말씀, 당시 문화, 비유와 이적을 찾아볼 수 있어요."
				)
			)
		),
		GuideSection(
			"알림",
			listOf(
				GuideItem(
					"매일 말씀 알림 / 통독 리마인더",
					"설정에서 켜면 정해진 시간에 랜덤 말씀 알림을 받거나, 그날 아직 성경을 안 읽었을 때 리마인더를 받을 수 있어요."
				)
			)
		),
		GuideSection(
			"데이터 내보내기 · 불러오기",
			listOf(
				GuideItem(
					"왜 필요한가요?",
					"이 앱은 사이드로딩(APK 직접 설치)으로 배포돼서, 업데이트하다가 데이터가 초기화될 수 있어요. 업데이트 전에 설정에서 내보내기로 백업해두고, 업데이트 후 불러오기로 복원하면 안전해요."
				),
				GuideItem(
					"주의할 점",
					"불러오기는 기존 데이터를 전부 백업 파일 내용으로 교체해요. 성경 본문/찬송가 같은 내장 데이터는 영향받지 않아요."
				)
			)
		)
	)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_user_guide)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_guide_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "사용 가이드"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val container = findViewById<LinearLayout>(R.id.container_guide)
		for ((index, section) in sections.withIndex()) {
			addSectionHeader(container, section.header, addTopSpacing = index != 0)
			for (item in section.items) {
				addItem(container, item.title, item.description)
			}
		}
	}

	private fun addSectionHeader(container: LinearLayout, text: String, addTopSpacing: Boolean) {
		val header = TextView(this).apply {
			this.text = text
			textSize = 17f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@UserGuideActivity, R.color.brown_primary))
			setPadding(0, if (addTopSpacing) dp(28) else 0, 0, dp(12))
		}
		container.addView(header)
	}

	private fun addItem(container: LinearLayout, title: String, description: String) {
		val itemContainer = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(0, 0, 0, dp(16))
		}
		val titleView = TextView(this).apply {
			text = title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@UserGuideActivity, R.color.text_primary))
		}
		val descView = TextView(this).apply {
			text = description
			textSize = 14f
			setTextColor(ContextCompat.getColor(this@UserGuideActivity, R.color.text_secondary))
			setPadding(0, dp(4), 0, 0)
			setLineSpacing(dp(2).toFloat(), 1f)
		}
		itemContainer.addView(titleView)
		itemContainer.addView(descView)
		container.addView(itemContainer)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}