package com.chan.bnote.data.notice

/**
 * 공지 본문의 소제목("## 제목") 줄 표시. 모양은 바꾸지 않는 표시용 span으로, NoticeFormatter가 소제목 줄에
 * 붙이고 화면 쪽(ListText)이 이 표시를 보고 소제목 위는 넓게 · 아래 내용과는 붙게 간격을 정한다.
 * (data 쪽 NoticeFormatter가 화면 코드에 기대지 않도록 여기에 둔다.)
 */
class NoticeHeadingSpan