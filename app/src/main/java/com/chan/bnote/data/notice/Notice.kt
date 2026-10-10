package com.chan.bnote.data.notice

import androidx.annotation.ColorRes
import com.chan.bnote.R

/**
 * 앱 안 알림(공지사항) 하나. GitHub 저장소의 이슈 하나가 공지 하나다(이슈 번호 = [id]).
 *
 * - "공지" 라벨이 붙은 열린 이슈만 공지로 가져온다. 이슈를 닫으면 앱에서도 사라진다.
 * - 타입은 라벨로 정한다(NoticeType 참고). "고정" 라벨이면 목록 맨 위, "해결됨" 라벨이면 해결 표시.
 * - 본문 맨 위에 "대상: 1.13 이하"처럼 쓰면 그 버전 사용자에게만 보인다(그 줄은 화면에 안 나옴).
 */
data class Notice(
	val id: Long,
	val title: String,
	/** 이슈 본문(마크다운, "대상:" 줄은 뺀 것). 화면에 보일 땐 NoticeFormatter로 다듬는다. */
	val body: String,
	val type: NoticeType,
	val isPinned: Boolean,
	val isResolved: Boolean,
	val createdAt: Long,
	val updatedAt: Long,
	/** 내가(저장소 주인) 단 댓글 수 — 상세 화면의 "추가 안내". GitHub가 주는 전체 댓글 수라 다른 사람 댓글이 섞일 수 있다. */
	val commentCount: Int,
	val minVersion: String?,
	val maxVersion: String?
) {
	/**
	 * 읽음 여부를 판단하는 "내용 버전". 읽은 뒤에 추가 안내(댓글)가 달리거나 "해결됨"으로 바뀌면
	 * 값이 달라져서 다시 안 읽은 알림이 된다(본문 오타 수정 같은 작은 고침으로는 다시 뜨지 않음).
	 */
	val readSignature: String get() = "$commentCount:$isResolved"
}

/**
 * 알림 타입. [label]은 GitHub 이슈에 붙이는 라벨 이름(없으면 기본 "안내").
 * 종류 표시는 파스텔 배경 + 같은 계열의 진한 글자(다크모드는 어두운 배경 + 밝은 글자, values-night에서).
 */
enum class NoticeType(
	val label: String?,
	val displayName: String,
	@ColorRes val backgroundColorRes: Int,
	@ColorRes val textColorRes: Int
) {
	UPDATE("업데이트", "업데이트", R.color.notice_type_update_bg, R.color.notice_type_update_text),
	BIBLE_TEXT_ERROR(
		"본문 오류",
		"성경 본문 오류",
		R.color.notice_type_bible_bg,
		R.color.notice_type_bible_text
	),
	BUG("기능 오류", "기능 오류", R.color.notice_type_bug_bg, R.color.notice_type_bug_text),
	TIP("사용 팁", "사용 팁", R.color.notice_type_tip_bg, R.color.notice_type_tip_text),
	GENERAL(null, "안내", R.color.notice_type_general_bg, R.color.notice_type_general_text);

	companion object {
		fun fromLabels(labels: Collection<String>): NoticeType =
			entries.firstOrNull { it.label != null && it.label in labels } ?: GENERAL
	}
}

/** 상세 화면의 "추가 안내" 한 개(이슈에 저장소 주인이 단 댓글). */
data class NoticeComment(val body: String, val createdAt: Long)