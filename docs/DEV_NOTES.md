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

## 공식 API 후보 비교 (2026-11-13 조사, 결정 대기)

CLAUDE.md 조건: 신청 기간, 지원 대상, 공식 공고 링크를 주는 전국 공통 공식 API 한 곳부터.

| | 온통청년 청년정책 API | 정부24 공공서비스(혜택) 정보 API (보조금24) |
|---|---|---|
| 제공 | 한국고용정보원 (온통청년, 공공데이터포털 15143273) | 행정안전부 (공공데이터포털 15113968) |
| 대상 | 청년 정책만 (일자리·주거·금융·복지·교육문화) | 전 국민 대상 정부·지자체 혜택 전체 (약 1만 1천 건) |
| 신청 기간 | `aplyYmd` "YYYYMMDD ~ YYYYMMDD", 접수 구분 코드로 상시·특정기간·마감 구분 (커뮤니티 보고) | `신청기한` 자유 텍스트. 대부분 "상시신청", 날짜가 있는 건 약 5% (커뮤니티 보고) |
| 지원 대상 | 최소·최대 나이, 시군구 코드(`zipCd`), 취업 상태·소득 조건 코드 (커뮤니티 보고) | `supportConditions`로 코드 제공: 대상연령 시작·종료(JA0110/JA0111), 근로자·직장인(JA0326), 중소기업(JA2101), 중위소득 구간 등 (공식 페이지 확인) |
| 링크 | 신청 URL, 참고 URL (커뮤니티 보고) | 상세조회URL, 온라인신청사이트URL (공식 페이지 확인) |
| 변경 감지 | 정책번호, 최종수정일 (커뮤니티 보고) | 서비스ID, 수정일시 + 수정일시 조건 검색 (공식 페이지 확인) |
| 키 발급 | 온통청년 회원가입 → 마이페이지 → OPEN API 신청 → 담당자 승인 (기간 안내 없음) | 공공데이터포털 활용신청 (승인 방식·트래픽은 페이지에서 확인 못 함) |
| 이용 조건 | 무료, 이용허락범위 제한 없음 | 무료, 이용허락범위 제한 없음 |

**추천: 온통청년 청년정책 API를 먼저 연동.** 서비스 대상(청년)과 같고, 마감 알림에 필요한 신청 기간이 날짜로 오며, 나이와 지역 코드로 매칭과 상위 지역 처리가 가능하다. 보조금24는 마감일이 대부분 "상시"라 마감 알림에 약하므로, 대상과 기관을 넓힐 때 두 번째 출처로 붙인다.

**확인 못 한 것 (키 받은 뒤 실제 호출로 확인):**
- 온통청년 공식 문서 페이지(oaiDoc)의 요청·출력 표가 브라우저에서도 비어 있어 필드명을 공식 문서로 확인하지 못했다. 위 필드명은 GitHub 프로젝트들의 기록이다.
- 2025년 개편으로 엔드포인트와 파라미터가 바뀐 것으로 보고된다: `https://www.youthcenter.go.kr/go/ythip/getPlcy`, `apiKeyNm`, `pageNum`, `pageSize`, `rtnType=json`, 응답 `result.pagging`, `result.youthPolicyList`. 문서 페이지 예시는 아직 예전 주소(`/opi/youthPlcyList.do`, `openApiVlak`)다.
- 지역 코드: 전북특별자치도 출범(2024) 이후 시군구 코드 체계(45→52)가 어느 쪽으로 오는지, 전국 공통 정책은 어떻게 표시되는지. 코드정의서(`/downloadform/API코드정보.xlsx`)로 확인한다.
- 호출 한도, 공고 취소가 응답에서 어떻게 보이는지(목록에서 사라지는지, 상태값이 있는지).

**사용자가 할 일:** 온통청년 회원가입 후 마이페이지 → 오픈(OPEN) API에서 인증키 신청. 받은 키는 저장소에 넣지 않고 환경 변수(예: `YOUTHCENTER_API_KEY`)로 넣는다.

## 다음 작업

1. ~~공고 데이터 모델과 서비스 분리~~ (2026-11-12 완료)
2. 공식 API 결정
3. 1시간 주기 수집(출처·식별자로 갱신, 마감 변경·모집 취소 반영, 첫 수집은 알림 제외)
4. 카카오 로그인, 조건 저장, 관심 공고
5. 웹 푸시(새 공고, 마감 3일 전·당일, 변경·취소, 중복 발송 방지)
