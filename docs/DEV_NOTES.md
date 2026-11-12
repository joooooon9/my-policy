# 개발 노트

작업하면서 정한 것, 알려진 문제, 확인해야 할 것을 모아 둔다. 작업이 끝날 때마다 갱신하고, 해결한 항목은 지우지 말고 "해결" 표시와 날짜를 남긴다.

## 로컬 실행

- PostgreSQL만 Docker Compose로 띄운다: `docker compose up -d` (데이터는 `postgres-data` 볼륨).
- 앱은 IntelliJ에서 `MyPolicyApplication`을 실행한다(기본 포트 8080).
- DevTools가 들어 있다. IntelliJ에서 아래 두 설정을 켜면 템플릿·CSS는 새로고침, Java는 자동 재시작으로 반영된다.
  - Settings → Build, Execution, Deployment → Compiler → **Build project automatically**
  - Settings → Advanced Settings → **Allow auto-make to start even if developed application is currently running**
- 테스트: `./gradlew test` (단위 테스트만 있음, DB 불필요).

## 정한 것

| 날짜 | 내용 |
|---|---|
| 2026-11-12 | 서비스 소개 `/about`은 `design-taste-frontend` 스킬로 만들고 gate를 통과시킨다. 검색·상세는 `static/css/policy.css`에서 같은 색과 글꼴을 쓴다. |
| 2026-11-12 | 디자인 기준: 토스처럼 밝고 친근하게. 흰 바탕, 파랑 `#1b64da`, Pretendard, 둥근 모서리. 다크 모드는 시스템 설정을 따르고 헤더 버튼으로 바꾼다(`static/js/theme.js`). |
| 2026-11-12 | DB 스키마는 Flyway로 관리한다(`src/main/resources/db/migration`). `ddl-auto=validate`라 엔티티와 테이블이 다르면 앱이 뜨지 않는다. 스키마를 바꿀 때는 새 `V{n}__*.sql`을 추가하고, 이미 적용된 파일은 고치지 않는다. |
| 2026-11-12 | 공고는 `(source, source_id)`로 식별한다. 지역은 `Region` enum(전국 ← 전북 ← 익산)으로 두고 상위 지역을 함께 조회한다. |
| 2026-11-12 | 매칭 규칙은 `PolicyMatcher`에 있다. 지역·나이·재직 상태·중소기업 "아니요"가 명확히 어긋날 때만 제외하고, 소득·근속기간·중소기업 "잘 모르겠어요"는 확인 필요 칩으로 표시한다. |
| 2026-11-12 | 날짜는 `Clock`(Asia/Seoul, `my-policy.zone`)으로 계산한다. 마감 시각(`apply_end_time`)은 공고에 있을 때만 저장·표시한다. |
| 2026-11-12 | 공식 API를 붙이기 전까지 샘플 공고 3개를 `source = 'sample'`로 넣는다(`my-policy.sample-data=true`, DB에 샘플이 없을 때만 한 번). |
| 2026-11-12 | 스킬 사본(`.claude/skills/design-taste-frontend`, 38MB)은 저장소에 그대로 둔다. 설치 원본은 `.agents/`(무시), 출처는 `skills-lock.json`. |

## 알려진 문제

- **샘플 공고 마감일이 고정돼 있다.** 2026-11-15, 19, 24 마감이라 11월 25일부터 목록이 비어 보인다. 공식 API를 붙이면 샘플을 끄거나(`my-policy.sample-data=false`) 지운다(`delete from policy where source = 'sample'`).
- **`/about` 태블릿 레이아웃(820, 768).** gate는 1440·390·320만 본다. 이전 검사에서 매칭 섹션 가운데 빈 공간, 푸터 절반 이상 빈 영역, 768에서 검색 예시의 '예' 글자 잘림이 나왔다. 디자인을 바꾼 뒤 다시 확인하지 않았다.
- **로딩 성능.** 키트 CSS 합계가 약 330KiB(압축 전), 글꼴 파일이 많다. Spring 응답 압축(`server.compression.enabled=true`)이 꺼져 있다.
- **외부 CDN 의존.** Pretendard를 jsDelivr에서 불러온다. CDN이 막히면 시스템 글꼴로 보인다. 필요하면 `static/`에 직접 넣는다.
- **`/about` 문구가 아직 없는 기능을 설명한다.** 카카오 로그인, 관심 공고, 웹 푸시, 공식 API 수집은 미구현이다. 출시 전에 구현하거나 문구를 맞춘다.
- **지역 선택지.** CLAUDE.md는 "수집 중인 지역만 선택지에 노출"인데 지금은 세 지역이 모두 나온다. 수집을 붙일 때 `Region`에 수집 여부를 두고 걸러야 한다.
- **Gradle 경고.** 빌드에 "Gradle 10과 호환되지 않는 기능 사용" 경고가 나온다. `--warning-mode all`로 원인 확인 필요.

## 확인해야 할 것

- [ ] 연동할 전국 공통 공식 API 결정(후보: 온통청년 청년정책 Open API, 공공데이터포털 보조금24). 신청 기간·공고 식별자·원문 링크 제공 여부와 인증키 발급 조건 확인.
- [ ] 운영 DB 비밀번호는 환경 변수(`DB_PASSWORD`)로 바꾼다. `application.properties`와 `compose.yaml`의 `my_policy_local`은 로컬 기본값이다.
- [ ] 공개 도메인이 정해지면 `/about`의 `canonical` 주석을 실제 주소로 바꾼다.
- [ ] `/about` gate 예외 처리 4건이 여전히 맞는지 디자인을 바꿀 때마다 다시 본다(`.taste-skill/waivers.json`, 저장소에는 없음).
  - hero-lines, hero-maxchars: 한글 전각 글자로 채우는 길이 검사
  - section-gaps(#search / #match, 390): 정상 간격을 기준값과 비교해 걸림
  - one-section(#search 앱 화면): 제품 화면 예시를 글자 띠로 오인
- [ ] 스킬 설치 때 보안 평가가 Gen "Critical Risk", Snyk "Med Risk"였다. 스킬 스크립트(크롬 실행, 아이콘·사진 내려받기)를 쓰기 전에 한 번 검토한다.

## 다음 작업

1. ~~공고 데이터 모델과 서비스 분리~~ (2026-11-12 완료)
2. 공식 API 결정
3. 1시간 주기 수집(출처·식별자로 갱신, 마감 변경·모집 취소 반영, 첫 수집은 알림 제외)
4. 카카오 로그인, 조건 저장, 관심 공고
5. 웹 푸시(새 공고, 마감 3일 전·당일, 변경·취소, 중복 발송 방지)
