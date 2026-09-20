# 📖 BNOTE

**성경 읽기부터 설교노트, 암송, 기도제목까지 — 교회 공동체를 위한 개인용 성경 앱**

BNOTE는 서버 없이 완전히 기기 안에서만 동작하는 안드로이드 성경 앱이에요. 성경을 읽으면서 형광펜을 칠하고, 메모를 남기고, 설교노트를 정리하고, 말씀을 암송하는 것까지 한
곳에서 할 수 있도록 만들었습니다. Play 스토어 없이 APK 파일로 직접 배포하는 사이드로딩 방식이에요.

---

## ✨ 주요 기능

### 성경 읽기

- 성경 → 장 → 절 순서로 빠르게 이동
- 다중 번역본 지원 (개역개정 / 개역한글 / 새번역 / 쉬운성경) + 대역본("함께보기") 나란히 보기
- 성경 전체 검색 (최근 검색어 기록 지원)
- 형광펜(구절 전체 또는 일부 단어만), 북마크, 스크랩(그룹 관리)
- 구절 메모 / 단어 메모 — 한 곳에 메모 여러 개, 단어 메모는 같은 단어가 나오는 다른 구절에도 한 번에 추가 가능
- 메모 안에 `(창 1:1)`, `1절`, `(2:1)` 같은 인용 표기를 길게 누르면 실제 본문 미리보기
- 성경읽기표 자동 체크, 자동 스크롤
- 찬송가 (분류별 탐색, 악보 이미지, 유튜브 반주 연동)
- 부록 (주기도문 · 사도신경 · 십계명 · 교독문)
- 성경 배경지식 (인물사전 · 지명사전 · 족보 · 연대표 · 상황별 말씀 · 당시 문화 · 비유와 이적)

### 설교노트

- 제목 · 본문(성경 구절, 여러 개 가능) · 설교자 · 카테고리 기록
- 메모에 굵게 / 밑줄 / 글자색 서식 적용
- 사진 최대 5장 첨부 (촬영 또는 갤러리)
- 날짜별 · 설교자별로 모아보기, 검색

### 마이페이지

- 내 정보 및 전체 활동 기록 요약
- 성경읽기표 진행률
- 올해 약속의 말씀
- 기도제목 노트 (응답 체크)
- 암송 구절 (그룹 관리, 그룹/개별 암송 연습, 힌트 기능)
- 최근 활동(최근 본 장 · 메모 · 설교노트) 바로가기

### 설정 및 관리

- 글자 크기, 다크모드, 자동스크롤 속도
- 매일 말씀 알림 / 통독 리마인더
- **데이터 내보내기 / 불러오기** — 사이드로딩 앱 특성상 업데이트 시 데이터가 초기화될 수 있어, 백업 zip 파일로 모든 사용자 데이터(사진 포함)를 내보내고 복원할 수
  있어요
- 사용 가이드 (검색 가능, 모든 기능에 대한 상세 설명)
- 크래시 로그 자동 수집 → 문의 시 자동 첨부

---

## 🛠 기술 스택

- **언어**: Kotlin
- **아키텍처**: Room 싱글톤 DB + ViewModel 없는 경량 구조 (서버가 없는 로컬 전용 앱 특성상 ViewModel이 Controller 역할 겸함)
- **데이터베이스**: [Room](https://developer.android.com/training/data-storage/room) (실제 마이그레이션 정책 적용, 스키마
  변경 시 데이터 보존)
- **UI**: XML + View
  기반, [Material Components](https://github.com/material-components/material-components-android)
- **비동기 처리**: Kotlin Coroutines
- **이미지 로딩**: [Coil](https://github.com/coil-kt/coil)
- **레이아웃**: [Flexbox for Android](https://github.com/google/flexbox-layout)
- **백그라운드 작업**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) (알림 스케줄링)
- **시작화면**: [Core SplashScreen](https://developer.android.com/develop/ui/views/launch/splash-screen)

| 항목                   | 값                |
|----------------------|------------------|
| Application ID       | `com.chan.bnote` |
| Min SDK              | 24               |
| Target / Compile SDK | 35               |
| Kotlin               | 2.0.21           |
| AGP                  | 8.10.1           |

---

## 📁 프로젝트 구조

```
com.chan.bnote
├── data/                      # Room 엔티티 · DAO · 저장소
│   ├── bible/                 #   성경 읽기 관련 (본문, 북마크, 하이라이트, 스크랩, 메모, 찬송가)
│   ├── sermon/                #   설교노트 (설교자 · 카테고리 · 사진 포함)
│   ├── mypage/                #   마이페이지 (암송구절 · 성경읽기표 · 올해의 말씀 · 기도제목 · 프로필)
│   ├── knowledge/              #   성경 배경지식
│   ├── appendix/               #   부록
│   └── backup/                 #   데이터 내보내기/불러오기
├── ui/                         # 화면 (Activity / Fragment / Adapter)
│   ├── bible/, sermon/, mypage/, knowledge/, appendix/, common/
├── notification/                # 알림 스케줄링 (WorkManager)
└── BnoteApplication.kt          # 크래시 로거 초기화
```

---

## 🚀 빌드 및 실행

```bash
git clone https://github.com/eunchan96/project-BNOTE.git
cd project-BNOTE
```

Android Studio에서 프로젝트를 열고 Gradle Sync 후 실행하면 돼요.

### 배포용 APK 만들기

Play 스토어 없이 APK를 직접 배포하는 방식이라, 서명이 필요해요.

1. `Build > Generate Signed Bundle / APK` → APK 선택
2. keystore 생성 또는 기존 keystore 선택
3. release 빌드 → `app/release/app-release.apk` 생성

> ⚠️ **업데이트 시 반드시 처음 만든 keystore로 서명해야 기존 사용자 데이터가 유지돼요.** keystore 파일과 비밀번호는 안전한 곳에 반드시 백업해두세요.

---

## 🔒 데이터 및 개인정보

- 이 앱은 **서버가 없는 완전한 로컬 전용 앱**이에요. 모든 데이터는 기기 안 Room 데이터베이스에만 저장되고, 어디로도 전송되지 않아요.
- 사용자가 직접 "데이터 내보내기"를 실행하기 전까지는 데이터가 기기 밖으로 나가지 않아요.
- 크래시 로그는 기기 안에만 저장되며, 사용자가 "문의하기"를 통해 직접 보낼 때만 전달돼요.

---

## ⚠️ 저작권 안내

이 프로젝트는 개인/교회 공동체 내 비상업적 사용을 목적으로 만들어졌어요. 앱에 포함된 성경 번역본, 찬송가 등은 각 저작권자(대한성서공회 등)의 권리가 있는 저작물이에요. 이
프로젝트를 배포하거나 재사용하실 때는 반드시 해당 콘텐츠의 저작권 정책을 별도로 확인하시고, 필요한 경우 저작권자의 허가를 받아주세요.

---

## 📬 문의

앱 내 **마이페이지 > 설정 > 앱 정보 > 문의하기**에서 이메일 또는 카카오톡 오픈채팅으로 문의하실 수 있어요.
