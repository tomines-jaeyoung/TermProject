# 온에어마켓 (OnAir Market) — Term Project

상품 소개 사진·동영상을 올리고, 정해진 기간 동안 입찰받는 경매 웹 서비스입니다.
온에어마켓 설계 보고서의 **사진·동영상 기간 경매**와 **회원가입·로그인**을 Spring Boot + JPA로 구현하고,
중고 거래처럼 정해진 가격으로 바로 사는 **즉시 구매**, **관리자 화면**,
머신러닝(TF-IDF + 코사인 유사도) 기반 **비슷한 경매 추천**을 함께 구현했습니다.

## 사용한 과목 (배운 과목 2가지 이상 활용)

| 과목 | 이 프로젝트에서 사용한 내용 |
|---|---|
| 프레임워크 실습 | Spring Boot · Spring MVC · Spring Security · Spring Data JPA로 서버, 회원가입·로그인, 권한 처리 |
| 데이터베이스 | MySQL, E-R 설계(테이블 7개), JPA 엔티티 매핑, 조건부 UPDATE로 동시성 처리 |
| 웹 프로그래밍 | Thymeleaf · HTML · CSS · JavaScript 화면 (목록·상세·등록 폼, 남은 시간, 업로드 진행률) |
| 머신러닝심화 | 내용 기반 추천: 상품명·설명·카테고리 TF-IDF 벡터화 + 코사인 유사도 → 경매 상세의 '비슷한 경매' |

## ▶ 바로 실행해 보기

**[http://192.168.26.135:8082](http://192.168.26.135:8082/)** — 학교 서버에서 실행 중 (학교 네트워크에서 접속)

| 화면 | 주소 |
|---|---|
| 첫 화면(소개) | http://192.168.26.135:8082/ |
| 홈 (영상 경매 · 곧 낙찰 · 사진 경매 · 즉시 구매) | http://192.168.26.135:8082/home |
| 경매 상세 + 비슷한 경매 추천 | 홈에서 아무 경매나 누르면 아래쪽에 'ML 추천' 표시 |
| 회원가입 · 로그인 | http://192.168.26.135:8082/signup · http://192.168.26.135:8082/login |
| 관리자 화면 (admin / admin1234) | http://192.168.26.135:8082/admin |

## 실행 방법 (IntelliJ)

1. MySQL 8.0 실행 (localhost:3306, 계정 `root` / `1234` — `application.properties`에서 변경 가능)
2. IntelliJ에서 이 폴더를 열고 Gradle 프로젝트로 불러오기 (JDK 21)
3. `OnAirMarketApplication` 실행 → http://localhost:8080
   - DB 스키마 `onairmarket`은 없으면 자동 생성됩니다 (`createDatabaseIfNotExist=true`).
   - 테이블은 JPA 엔티티로 자동 생성됩니다 (`spring.jpa.hibernate.ddl-auto=update`). 별도 SQL 파일이 필요 없습니다.
   - 처음 실행할 때 체험용 회원 2명, 샘플 경매 12개(영상 경매·시작 예정·종료 예시 포함), 즉시 구매 상품 5개가 자동으로 들어갑니다.

| 계정 | 아이디 | 비밀번호 |
|---|---|---|
| **관리자** (`/admin`) | `admin` | `admin1234` |
| 판매자 (온에어셀렉트) | `onair_seller` | `onair1234` |
| 구매자 (수집가민지) | `onair_buyer` | `onair1234` |

- 관리자 계정은 실행할 때 없으면 자동으로 만들어집니다 (이미 쓰던 DB에도 한 번만 추가). 일반 회원가입으로는 관리자가 될 수 없습니다.

### DB 직접 확인하기 (MySQL)

| 항목 | 값 |
|---|---|
| Host / Port | `localhost` / `3306` |
| User / Password | `root` / `1234` |
| Database | `onairmarket` |

IntelliJ 오른쪽 **Database** 창 → **＋ → Data Source → MySQL** → 위 값 입력 → (처음이면 드라이버 다운로드) → Test Connection → OK.
`onairmarket → tables`에서 테이블 7개를 볼 수 있고, 회원가입·관리자 작업 후 새로고침하면 `members`·`admin_audit` 행이 바뀌는 것을 확인할 수 있습니다.

업로드한 파일은 프로젝트 폴더의 `uploads/` 아래에 저장됩니다 (git에는 올리지 않음).

## 기술 구성

| 영역 | 사용 기술 |
|---|---|
| 서버 | Spring Boot 4.0.3, Java 21, Spring MVC |
| 화면 | Thymeleaf, CSS(기존 온에어마켓 브랜드 색), 약간의 JavaScript(남은 시간, 업로드 진행률) |
| 인증 | Spring Security 폼 로그인 + members 테이블, BCrypt 비밀번호 암호화 |
| DB | MySQL 8.0 + Spring Data JPA (Hibernate) |
| 파일 | 로컬 폴더 저장 + `auction_media` 메타데이터, `/api/media/{id}`로 읽기 (영상 구간 요청 지원) |

## 기능

| 설계 보고서 ID | 기능 | 구현 위치 |
|---|---|---|
| F-01 계정 인증 | 회원가입·로그인·로그아웃 (DB 연동) | `AuthController`, `MemberService`, `SecurityConfig` |
| F-02 목록 탐색 | 홈 섹션(영상으로 만나는 경매·곧 낙찰·사진 경매·즉시 구매·곧 시작하는 경매), 둘러보기: 카테고리·검색·정렬(최신/마감 임박/입찰 많은 순/현재가)·진행 중·동영상만 필터 | `HomeController`, `TimedAuctionRepository.search` |
| F-03 판매 방식 선택·등록 | 판매하기 → 방식 선택(사진·동영상 경매 / 즉시 판매) → 등록 (경매: 즉시 시작/시작 예약, 7·30·90일/직접 지정) | `SellController`, `AuctionService.create`, `ProductService.create` |
| F-04 파일 업로드 | 사진 8MB(JPG·PNG·WebP), 영상 50MB(MP4·WebM), 파일 앞부분으로 형식 확인, pending → ready, 실패 시 정리, 업로드 진행률 | `MediaService`, `onair.js` |
| F-05 입찰 | 현재가 + 1,000원만 허용, revision 조건부 갱신, 동시 입찰 시 한 건만 성공, 본인 경매·기간 외 입찰 차단 | `AuctionService.bid`, `TimedAuctionRepository.applyBid` |
| F-07 관심 저장 | 저장/해제, 복합 기본 키로 중복 방지 | `AuctionService.toggleBookmark` |
| F-08 즉시 구매 | 즉시 판매 등록(/sell/product), 목록(/home?view=products), 상세, 구매 — 판매 중일 때 먼저 누른 1명만 판매 완료(조건부 UPDATE), 본인 상품 구매 불가, 내 정보에 판매·구매 기록 | `ProductService`, `ProductRepository.markSold` |
| F-06 내 정보 | 내 경매 목록, 관심 경매, 내 즉시 판매 상품, 구매한 상품, 내 입찰 기록 | `MyPageController` |
| F-09 관리 | 관리자 전용 화면(`/admin`): 대시보드(테이블별 현황), 회원 검색·이용 정지/해제·관리자 지정/해제, 경매 숨기기/다시 공개·입찰 기록, 즉시 구매 상품 삭제(판매 중만), 관리 이력. 모든 작업은 `admin_audit`에 기록 | `AdminController`, `AdminService`, `MemberStateInterceptor`, `SecurityConfig` |

| F-10 비슷한 경매 추천 (ML) | 경매 상세 아래 '이 상품과 비슷한 경매' 최대 4개, 유사도(%) 표시 | `ml/TfIdfRecommender`, `RecommendationService` |

### 머신러닝 추천 (F-10) 동작 방식

1. **데이터** – JPA로 공개(숨기지 않은) 경매를 모두 읽습니다 (`findByArchivedFalse`).
2. **토큰화** – 한글은 글자 2-gram(예: 빈티지 → 빈티, 티지), 영문·숫자는 단어 단위. 어미 같은 불용어는 제외.
3. **TF-IDF** – 상품명은 2배 가중치, IDF = log((1+N)/(1+df)) + 1, 벡터는 L2 정규화.
4. **코사인 유사도** – 정규화된 벡터의 내적. 자기 자신과 끝난 경매를 빼고 높은 순서로 4개.
5. **재학습** – 경매 수나 최종 수정 시각이 바뀌면 모델을 다시 만들고, 바뀌지 않으면 메모리의 모델을 재사용합니다.

외부 라이브러리 없이 Java로 구현했고, `TfIdfRecommenderTest`로 토큰화·순위·유사도 범위를 검증합니다.

### 관리자 기능 동작 규칙

- `/admin/**`는 role이 `ADMIN`인 회원만 들어갈 수 있습니다 (비로그인 → 로그인 화면, 일반 회원 → 403).
- 이용 정지된 회원은 로그인이 거부되고(`/login?suspended`), 로그인 중이었다면 다음 요청에서 바로 로그아웃됩니다.
- 관리자 권한이 해제되면 같은 세션이라도 `/admin`이 바로 막힙니다 (요청마다 DB의 회원 상태를 다시 확인).
- 본인 계정 정지·권한 변경, 관리자 계정 정지, 판매 완료 상품 삭제는 서버에서 거부합니다.
- 숨긴 경매는 목록·상세·입찰에서 빠지지만 데이터는 남습니다. 상품 삭제는 `DELETE … WHERE status = 'selling'` 조건으로 그 사이 판매된 상품은 지우지 않습니다.

## DB 테이블 (JPA 엔티티 7개)

| 테이블 | 엔티티 | 주요 조건 |
|---|---|---|
| `members` | `Member` | username UNIQUE, 비밀번호 BCrypt, role(USER/ADMIN), suspended(이용 정지) |
| `timed_auctions` | `TimedAuction` | 가격 원화 정수, 시각 밀리초, revision |
| `timed_bids` | `TimedBid` | UNIQUE (auction_id, revision) |
| `auction_media` | `AuctionMedia` | status pending/ready |
| `bookmarks` | `Bookmark` | 복합 기본 키 (user_id, auction_id) |
| `products` | `Product` | status selling/sold, 구매자(buyer_id) 기록, 조건부 UPDATE로 1명만 구매 |
| `admin_audit` | `AdminAudit` | 관리자 작업 이력 (actor_id, action, target, detail, created) |

설계 보고서처럼 SQL 외래 키 제약은 만들지 않고(`ConstraintMode.NO_CONSTRAINT`), 참조 대상·소유권은 서비스에서 검사합니다.

## 설계 보고서와 다른 점

- **members 테이블 추가**: 보고서는 호스팅 계정 인증(로컬 users 테이블 없음) 기준이지만, 회원가입·로그인을 DB와 연동하기 위해 회원 테이블을 추가했습니다.
- **저장소**: Cloudflare D1 → MySQL, R2 → 로컬 `uploads/` 폴더.
- **관리자**: 원 설계의 허용 목록 대신 `members.role`(ADMIN)로 권한을 구분하고, `admin_audit`은 원 설계 구성 그대로 구현했습니다. 판매자 상태(seller_status)와 요청 간격 제한(request_gates)은 구현 범위에서 제외했습니다.
- **즉시 구매**: 원 설계의 고정가 상품·예약(fixed_products·demo_orders, 재고·15분 예약)은 중고 거래형 `products` 한 테이블로 단순화했습니다. 상품 1개를 먼저 구매한 한 명에게 판매 완료합니다.
- 결제·정산은 없습니다 (체험 경매).

## 서버 배포 (Docker)

서버에 Java·MySQL이 없어도 Docker만 있으면 실행됩니다. MySQL 8 컨테이너와 앱 컨테이너가 함께 뜹니다.

```bash
git clone https://github.com/tomines-jaeyoung/TermProject.git
cd TermProject
APP_PORT=8081 docker compose up -d --build   # http://서버주소:8081
docker compose logs -f app                   # 실행 로그
git pull && docker compose up -d --build     # 코드 수정 후 다시 배포
```

DB 데이터와 업로드 파일은 Docker 볼륨(`db-data`, `uploads`)에 저장되어 재시작해도 유지됩니다.

`docker compose`가 없는 서버(학교 서버)에서는 기본 `docker` 명령으로 같은 구성을 실행합니다.

```bash
docker network create onair
docker run -d --name onair-db --network onair --restart unless-stopped   -e MYSQL_ROOT_PASSWORD=1234 -e MYSQL_DATABASE=onairmarket -e TZ=Asia/Seoul -v onair-db:/var/lib/mysql   mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
docker build -t onairmarket .
docker run -d --name onair-app --network onair --restart unless-stopped -p 8081:8080   -e SPRING_DATASOURCE_URL="jdbc:mysql://onair-db:3306/onairmarket?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false"   -e ONAIR_UPLOADDIR=/app/uploads -e SPRING_JPA_SHOWSQL=false -v onair-uploads:/app/uploads onairmarket
# 코드 수정 후: git pull && docker build -t onairmarket . && docker rm -f onair-app && (위 docker run onair-app 다시 실행)
```

학교 서버에서는 이미 실행 중인 MySQL 컨테이너(`onair-db`)와 업로드 볼륨을 함께 쓰고, 이 프로젝트를 **8082 포트**로 띄웁니다.

```bash
cd ~ && git clone https://github.com/tomines-jaeyoung/TermProject.git && cd TermProject
docker build -t termproject .
docker run -d --name termproject-app --network onair --restart unless-stopped -p 8082:8080   -e SPRING_DATASOURCE_URL="jdbc:mysql://onair-db:3306/onairmarket?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&useSSL=false"   -e ONAIR_UPLOADDIR=/app/uploads -e SPRING_JPA_SHOWSQL=false -v onair-uploads:/app/uploads termproject
# 코드 수정 후: cd ~/TermProject && git pull && docker build -t termproject . && docker rm -f termproject-app && (위 docker run 다시 실행)
```
