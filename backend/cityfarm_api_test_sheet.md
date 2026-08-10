# CityFarm API 테스트 시트

- 기준 서버: http://54.86.192.52:8080/swagger-ui/index.html
- 작성일: 2026-08-10
- 사용법: 이 문서를 그대로 복사해서 Notion 페이지에 붙여넣으면 표가 자동으로 Notion 테이블로 변환됩니다. 테스트 진행하면서 `결과` 칸에 Pass/Fail/Blocked를 채우고, 실패 건은 `비고`에 재현 방법을 남기세요.
- 공통 인증: `인증` = O 인 API는 로그인 후 발급받은 Access Token을 `Authorization: Bearer {token}` 헤더에 넣어야 합니다.
- 실서버(위 주소) OpenAPI 스펙과 대조 완료: 엔드포인트 25개 전체, 인증 필요 여부, 요청 DTO 필수값/enum 값 모두 이 표와 일치함을 확인했습니다.
- 🔴 **실제 버그 확인**: `POST /api/auth/logout`에 토큰 없이(또는 잘못된 토큰으로) 요청하면 401이 아니라 **500 Internal Server Error**가 반환됩니다(1-3번 표 참고). 원인과 재현 방법은 아래 1-3 표에 정리해뒀습니다.

---

## 1. 회원관리 API (`/api/auth`)

### 1-1. POST /api/auth/signup — 회원가입 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 회원가입 | email/password/passwordConfirm/name/nickname 모두 정상 입력 | 200 | "회원가입 성공" |  |
| 2 | 이메일 누락 | email="" | 400 | "이메일은 필수 입력값입니다." |  |
| 3 | 이메일 중복 | 이미 가입된 email 재사용 | 409 | AUTH_DUPLICATED_EMAIL "이미 사용중인 이메일입니다" |  |
| 4 | 닉네임 중복 | 이미 사용중인 nickname 재사용 | 409 | AUTH_DUPLICATED_NICKNAME "이미 사용중인 닉네임입니다" |  |
| 5 | 비밀번호/비밀번호 확인 불일치 | password != passwordConfirm | 400 | AUTH_PASSWORD_VALID "비밀번호가 일치하지 않습니다" |  |
| 6 | 비밀번호 누락 | password="" | 400 | "비밀번호는 필수 입력값입니다." |  |
| 7 | nickname 누락(값 검증 없음 확인용) | nickname 없이 요청 | ⚠확인필요 | nickname에는 `@NotBlank`가 없어 null 통과 가능성 있음 — 실제 동작 확인 |  |

### 1-2. POST /api/auth/login — 로그인 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 로그인 | 가입된 email/password | 200 | LoginResponseDto (accessToken 등) 반환 |  |
| 2 | 이메일/비밀번호 불일치 | 틀린 password | 400 | AUTH_LOGIN_FAILED "아이디 또는 비밀번호가 일치하지 않습니다" |  |
| 3 | 존재하지 않는 이메일 | 미가입 email | 400 | AUTH_LOGIN_FAILED |  |
| 4 | 이메일 누락 | email="" | 400 | validation 오류 |  |

### 1-3. POST /api/auth/logout — 로그아웃 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 로그아웃 | 유효한 토큰 | 200 | 정상 처리, refreshToken 무효화 확인 |  |
| 2 | 토큰 없이 요청 | Authorization 헤더 미포함 | 🔴500(버그, 401이어야 함) | 실서버 확인 결과 `INTERNAL_SERVER_ERROR` 반환됨. 원인: `/api/auth/**`가 SecurityConfig에서 permitAll이라 인증 없이 컨트롤러까지 통과 → `@AuthenticationPrincipal`이 null → `customUserDetails.getUserId()`에서 NPE → 500 |  |
| 3 | 위조/형식이상 토큰 | `Authorization: Bearer garbage.invalid.token` | 🔴500(버그, 401이어야 함) | 실서버 확인 결과 동일하게 500 발생 (토큰 검증 실패 시에도 인증 객체가 안 채워져 위와 같은 NPE 경로) |  |
| 4 | 만료된 토큰 | 만료된 access token | ⚠확인필요 | 위와 같은 경로라 500일 가능성 높음, 실제 만료 토큰으로 재확인 필요 |  |
| 5 | 로그아웃 후 같은 토큰 재사용 | 로그아웃한 accessToken으로 다른 API 재호출 | ⚠확인필요 | 블랙리스트 처리 여부 확인 (accessToken 자체는 만료 전까지 유효할 수 있음) |  |

---

## 2. 게시판 API (`/api/board`)

### 2-1. GET /api/board — 게시글 목록 조회 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 전체 조회(파라미터 없음) | - | 200 | 최신순 페이징 목록 |  |
| 2 | 제목 검색 | type=title&keyword=상추 | 200 | 제목에 keyword 포함된 글만 |  |
| 3 | 내용 검색 | type=content&keyword=팁 | 200 | 내용에 keyword 포함된 글만 |  |
| 4 | 작성자 검색 | type=author&keyword=닉네임 | 200 | 해당 작성자 글만 |  |
| 5 | 잘못된 type 값 | type=invalid&keyword=상추 | 400 | INVALID_INPUT_VALUE (단, `BOARD_TYPE_ERROR` 코드가 별도 존재하는데 실제로 안 쓰이고 있음 — 메시지 일치 여부 확인) |  |
| 6 | 페이징/정렬 | page=1&size=5&sort=likeCount,desc | 200 | 요청한 페이지/정렬 기준대로 반환 |  |

### 2-2. GET /api/board/{boardId} — 게시글 상세 조회 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 존재하는 게시글 조회 | 정상 boardId | 200 | 게시글 상세 정보 |  |
| 2 | 존재하지 않는 게시글 조회 | boardId=999999 | 404 | BOARD_NOT_FOUND |  |
| 3 | boardId에 문자열 입력 | boardId=abc | 400 | 타입 변환 오류 (INVALID_INPUT_VALUE 등) |  |

### 2-3. POST /api/board — 게시글 등록 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 일반 유저가 FREE/QNA 등록 | category=FREE | 201 | 게시글 등록 성공 |  |
| 2 | 일반 유저가 NOTICE 등록 시도 | category=NOTICE | 403 | AUTH_FORBIDDEN "접근 권한이 없습니다" (최근 401→403 수정 반영 확인) |  |
| 3 | 관리자가 NOTICE 등록 | category=NOTICE, ADMIN 계정 | 201 | 정상 등록 |  |
| 4 | 제목/내용 누락 | title="" | 400 | "제목은 필수입니다." |  |
| 5 | category 누락 | category=null | 400 | "카테고리는 필수입니다." |  |
| 6 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

### 2-4. PUT /api/board/{boardId} — 게시글 수정 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 게시글 정상 수정 | 본인 작성 글 title/content 변경 | 200 | 수정 성공 |  |
| 2 | 타인 게시글 수정 시도 | 다른 유저 작성 글 | 403 | BOARD_NOT_OWNER |  |
| 3 | 존재하지 않는 게시글 수정 | boardId=999999 | 404 | BOARD_NOT_FOUND |  |
| 4 | 일반 유저가 NOTICE로 수정 | category=NOTICE로 변경 시도 | 403 | AUTH_FORBIDDEN (401→403 수정 반영 확인) |  |
| 5 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

### 2-5. DELETE /api/board/{boardId} — 게시글 삭제 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 게시글 삭제 | 본인 작성 글 | 204 | No Content, 목록에서 제거 확인 |  |
| 2 | 타인 게시글 삭제 시도 | 다른 유저 작성 글 | 403 | BOARD_NOT_OWNER |  |
| 3 | 존재하지 않는 게시글 삭제 | boardId=999999 | 404 | BOARD_NOT_FOUND |  |
| 4 | 삭제한 게시글의 댓글/답글/좋아요 연쇄 삭제 확인 | 댓글·답글·좋아요 있는 글 삭제 후 조회 | ⚠확인필요 | cascade 설정 여부 실제 확인 필요 |  |

---

## 3. 게시글 댓글 API (`/api/board/{boardId}/board-comments`, `/api/board-comments/{commentId}`)

### 3-1. POST /api/board/{boardId}/board-comments — 댓글 등록 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 등록 | content="상추 먹고싶네요" | 201 | "댓글이 등록되었습니다." |  |
| 2 | 존재하지 않는 게시글에 등록 | boardId=999999 | 404 | BOARD_NOT_FOUND |  |
| 3 | 내용 누락 | content="" | 400 | "댓글 내용을 입력해주세요" |  |
| 4 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

### 3-2. GET /api/board/{boardId}/board-comments — 댓글 목록 조회 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 댓글 있는 게시글 조회 | - | 200 | 댓글 리스트 반환 |  |
| 2 | 댓글 없는 게시글 조회 | - | 200 | 빈 배열 반환 |  |
| 3 | 존재하지 않는 게시글 | boardId=999999 | ⚠확인필요 | 404 or 빈 배열, 실제 동작 확인 |  |

### 3-3. PATCH /api/board-comments/{commentId} — 댓글 수정 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 댓글 수정 | content 변경 | 200 | "댓글이 수정되었습니다." |  |
| 2 | 타인 댓글 수정 시도 | 다른 유저 댓글 | 403 | COMMENT_NOT_OWNER |  |
| 3 | 존재하지 않는 댓글 수정 | commentId=999999 | 404 | COMMENT_NOT_FOUND |  |

### 3-4. DELETE /api/board-comments/{commentId} — 댓글 삭제 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 댓글 삭제 | - | 200 | "댓글이 삭제되었습니다." |  |
| 2 | 타인 댓글 삭제 시도 | 다른 유저 댓글 | 403 | COMMENT_NOT_OWNER |  |
| 3 | 존재하지 않는 댓글 삭제 | commentId=999999 | 404 | COMMENT_NOT_FOUND |  |

---

## 4. 게시글 좋아요 API (`/api/board/{boardId}/likes`) — 전체 인증 O

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 좋아요 등록 | POST, 정상 boardId | 200 | liked=true, likeCount +1 |  |
| 2 | 이미 좋아요한 글에 중복 등록 | POST 재요청 | 200 | liked=true, likeCount 변화 없음(멱등 처리 확인) |  |
| 3 | 존재하지 않는 게시글에 좋아요 | POST boardId=999999 | 404 | BOARD_NOT_FOUND |  |
| 4 | 좋아요 취소 | DELETE, 좋아요 상태에서 | 200 | liked=false, likeCount -1 |  |
| 5 | 좋아요 안 한 상태에서 취소 시도 | DELETE 재요청 | 200 | liked=false, likeCount 변화 없음(멱등 처리 확인) |  |
| 6 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

---

## 5. 프로필 API (`/api/profile`) — 인증 O

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 프로필 조회 | 유효한 토큰 | 200 | 내 프로필 정보 반환 |  |
| 2 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |
| 3 | 탈퇴/비활성 계정으로 조회 | status=WITHDRAWN 계정 토큰 | ⚠확인필요 | 별도 차단 로직 있는지 확인 |  |

---

## 6. 답글 API (`/api/board/{id}/reply`, `/api/reply/{id}`)

### 6-1. GET /api/board/{id}/reply — 답글 목록 조회 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 답글 있는 게시글 조회 | - | 200 | 답글 리스트 반환 |  |
| 2 | 답글 없는 게시글 조회 | - | 200 | 빈 배열 |  |
| 3 | 존재하지 않는 게시글 조회 | id=999999 | 404 | BOARD_NOT_FOUND |  |

### 6-2. POST /api/board/{id}/reply — 답글 작성 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 답글 작성 | content 입력 | 200 | 답글 등록 성공 (다른 등록 API와 달리 201이 아닌 200 반환 — 일관성 확인) |  |
| 2 | 존재하지 않는 게시글에 작성 | id=999999 | 404 | BOARD_NOT_FOUND |  |
| 3 | 내용 누락 | content="" | 400 | "내용을 입력해주세요" |  |
| 4 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

### 6-3. PATCH /api/reply/{id} — 답글 수정 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 답글 수정 | content 변경 | 200 | 수정 성공 |  |
| 2 | 타인 답글 수정 시도 | 다른 유저 답글 | 403 | REPLY_NOT_OWNER |  |
| 3 | 존재하지 않는 답글 수정 | id=999999 | 404 | REPLY_NOT_FOUND |  |
| 4 | 관리자가 타인 답글 수정 시도 | ADMIN 계정으로 타인 답글 수정 | ⚠확인필요 | editReply는 삭제와 달리 ADMIN 예외 로직이 없어 403 REPLY_NOT_OWNER 발생 예상 — 삭제 API와의 정책 일관성 확인 |  |

### 6-4. DELETE /api/reply/{id} — 답글 삭제 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 답글 삭제 | - | 200 | "삭제 성공" |  |
| 2 | 관리자가 타인 답글 삭제 | ADMIN 계정 | 200 | 삭제 성공(관리자 예외 로직 있음) |  |
| 3 | 일반 유저가 타인 답글 삭제 시도 | - | 404 | REPLY_NOT_FOUND (소유자 아닐 시 403이 아니라 조회 자체가 안 돼 404로 응답 — 다른 API의 403 패턴과 다름, 실제 응답 확인 필요) |  |
| 4 | 존재하지 않는 답글 삭제 | id=999999 | 404 | REPLY_NOT_FOUND |  |

---

## 7. 답변 댓글 API (`/api/reply/{replyId}/reply-comments`, `/api/reply-comments/{commentId}`)

### 7-1. POST /api/reply/{replyId}/reply-comments — 답변 댓글 등록 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 등록 | content 입력 | 201 | "답변 댓글이 등록되었습니다." |  |
| 2 | 존재하지 않는 답글에 등록 | replyId=999999 | 404 | REPLY_NOT_FOUND |  |
| 3 | 내용 누락 | content="" | 400 | "댓글 내용을 입력해주세요" |  |
| 4 | 토큰 없이 요청 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

### 7-2. GET /api/reply/{replyId}/reply-comments — 답변 댓글 목록 조회 (인증 불필요)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 댓글 목록 조회 | - | 200 | 댓글 리스트 반환 |  |
| 2 | 댓글 없는 답글 조회 | - | 200 | 빈 배열 |  |

### 7-3. PATCH /api/reply-comments/{commentId} — 답변 댓글 수정 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 댓글 수정 | content 변경 | 200 | "답변 댓글이 수정되었습니다." |  |
| 2 | 타인 댓글 수정 시도 | 다른 유저 댓글 | 403 | COMMENT_NOT_OWNER |  |
| 3 | 존재하지 않는 댓글 수정 | commentId=999999 | 404 | COMMENT_NOT_FOUND |  |

### 7-4. DELETE /api/reply-comments/{commentId} — 답변 댓글 삭제 (인증 O)

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 본인 댓글 삭제 | - | 200 | "답변 댓글이 삭제되었습니다." |  |
| 2 | 타인 댓글 삭제 시도 | 다른 유저 댓글 | 403 | COMMENT_NOT_OWNER |  |
| 3 | 존재하지 않는 댓글 삭제 | commentId=999999 | 404 | COMMENT_NOT_FOUND |  |

---

## 8. 관리자 API (`/api/admin`) — 전체 인증 O, ROLE_ADMIN 필수

### 8-1. GET /api/admin/users — 전체 유저 목록 조회

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 관리자 계정으로 조회 | ADMIN 토큰 | 200 | 유저 목록 페이징 반환 |  |
| 2 | 일반 유저 계정으로 접근 | USER 토큰 | 403 | Access Denied (SecurityConfig `hasRole(ADMIN)`) |  |
| 3 | 토큰 없이 접근 | Authorization 헤더 미포함 | 401 | AUTH_UNAUTHORIZED |  |

### 8-2. PATCH /api/admin/users/{id} — 유저 정보/상태 수정

| No | 테스트 케이스 | 요청 데이터 예시 | 기대 상태코드 | 기대 응답 / 비고 | 결과 |
|---|---|---|---|---|---|
| 1 | 정상 상태 변경 | status=INACTIVE | 200 | 변경된 유저 정보 반환 |  |
| 2 | 정상 권한 변경 | roleType=ADMIN | 200 | 변경된 유저 정보 반환 |  |
| 3 | 존재하지 않는 유저 수정 | id=999999 | 404 | USER_NOT_FOUND |  |
| 4 | status 필드를 null로 요청 | status 미포함 | ⚠확인필요 | 검증 로직 없이 null로 그대로 업데이트될 가능성 — `USER_STATUS_ERROR` 코드가 정의만 되어 있고 실제로 사용되지 않음, 의도한 동작인지 확인 |  |
| 5 | 정의되지 않은 status 문자열 | status="DELETED" | 400 | JSON 역직렬화 오류로 INVALID_INPUT_VALUE 예상 |  |
| 6 | 일반 유저 계정으로 접근 | USER 토큰 | 403 | Access Denied |  |

---

## 참고: 발견된 이슈 정리

### 🔴 실제 버그 (실서버 확인 완료)
- **`POST /api/auth/logout`에 토큰 없이/잘못된 토큰으로 요청 시 401이 아닌 500 반환.** `SecurityConfig`에서 `/api/auth/**`가 permitAll이라 인증 없이 컨트롤러까지 도달하고, `@AuthenticationPrincipal`이 null인 상태로 `.getUserId()`를 호출해 NPE → 전역 500 핸들러로 떨어짐. `/api/auth/logout`만 별도 인증 필요 경로로 SecurityConfig에서 분리하거나, 컨트롤러에서 null 체크가 필요.

### ⚠ 확인/논의 필요 (기획 의도 확인용, 버그 여부 불명확)
- `/api/board` 목록 조회: `type` 값이 유효하지 않을 때 `BOARD_TYPE_ERROR`(전용 코드)가 아닌 `INVALID_INPUT_VALUE`가 반환됨 — 의도한 설계인지 확인.
- 게시판 댓글/답글 목록 조회 시 게시글/답글 자체가 없는 경우의 응답이 API마다 다를 수 있음(404 vs 빈 배열) — 전체 일관성 점검 권장.
- 관리자 유저 상태 변경 API에 `Status`/`RoleType` null 또는 잘못된 값에 대한 명시적 검증이 없음(`USER_STATUS_ERROR` 미사용 상태).
- 로그아웃 후 기존 accessToken이 만료 전까지 재사용 가능한지(블랙리스트 처리 여부) 확인 필요.

### ✅ 확인 결과 의도된 동작 (팀 확인 완료, 재테스트만 필요)
- 답글(Reply) 삭제 시 소유자가 아니면 403이 아니라 404로 응답되는 것, 답글 수정에는 관리자 예외 처리가 없고 삭제에만 있는 것 — 팀 확인 결과 의도된 정책. 정상 동작 여부만 테스트로 확인.
