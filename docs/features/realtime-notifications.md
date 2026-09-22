# 실시간 알림 (SSE)

> 관련 이슈: #45

## 결정 — WebSocket 이 아니라 SSE

| | SSE | WebSocket |
|---|---|---|
| 방향 | 서버 → 클라이언트 | 양방향 |
| 인증 | 기존 JWT 필터 그대로 | 핸드셰이크 인증을 따로 구현 |
| 인가·CORS·레이트 리밋 | 기존 규칙 그대로 (`/notifications/**` 인증) | 별도 |
| 프록시 | 평범한 HTTP. 버퍼링만 끄면 된다 | Upgrade 설정 필요 |

알림은 한 방향이다. 양방향 채널이 주는 이점이 없고, 인증·인가를 두 벌 만드는 비용만 생긴다.

## 흐름

```
알림 저장 (어느 경로든: 서비스, 관리자 발송, 빈자리 알림 …)
  → @PostPersist (NotificationPersistListener) → NotificationCreatedEvent
  → 커밋 후 (@TransactionalEventListener AFTER_COMMIT)
  → NotificationStreamService → 받는 사람의 열린 연결마다 전송
```

- 알림을 만드는 곳마다 발송 코드를 넣지 않는다. 엔티티 저장에 걸어서 새 생성 경로가 생겨도 빠지지 않는다.
- **커밋 뒤에** 보낸다. 롤백된 알림이 화면에 떴다가 목록에서 사라지는 일이 없다.

## 계약

`GET /notifications/stream` — `Authorization: Bearer <accessToken>`, 응답 `text/event-stream`

| 이벤트 | 언제 | data |
|--------|------|------|
| `connected` | 연결 직후 | `ok` |
| `notification` | 새 알림 | 알림 JSON (목록 API 의 항목과 같은 모양). `id:` 는 알림 id |
| `: ping` (주석) | 25초마다 | — |

브라우저 `EventSource` 는 헤더를 못 붙이므로 `fetch` 스트림으로 읽는다 (CareCode_FE `useNotificationStream`).

## 끊김·재연결·중복

- 연결은 30분 뒤 서버가 닫는다. 클라이언트는 다시 연결한다(지수 백오프).
- 끊긴 사이의 알림은 다시 보내 주지 않는다. 대신 **`connected` 를 받으면 목록을 새로 불러온다.**
  목록이 진실의 원천이고 SSE 는 "새로 불러올 때가 됐다" 는 신호다.
- 같은 알림을 두 번 받아도 목록을 다시 불러올 뿐이라 화면에 두 번 나오지 않는다.
- 사용자당 연결은 5개까지. 넘치면 가장 오래된 것을 닫는다 (재연결을 반복하는 클라이언트가 연결을 쌓지 못하게).

## 운영

| 항목 | 내용 |
|------|------|
| 지표 | `carecode.notification.stream.connections` — 열린 연결 수 (`/actuator/prometheus`) |
| 프록시 | 응답에 `X-Accel-Buffering: no`. Nginx 를 앞에 두면 `proxy_read_timeout` 을 heartbeat(25초)보다 길게 |
| 설정 | `app.notification.stream.timeout-millis`(30분), `heartbeat-millis`(25초), `max-connections-per-user`(5) |
| 보안 | 연결 종료 시 컨테이너의 ASYNC 디스패치는 인가를 다시 하지 않는다 (원 요청에서 이미 통과) |

## 한계

연결을 인스턴스 메모리에 둔다. 지금 운영은 단일 인스턴스라 충분하다.
**여러 대로 늘리면** 알림이 저장된 인스턴스와 사용자가 연결된 인스턴스가 다를 수 있으므로,
`NotificationCreatedEvent` 를 Redis pub/sub 로 모든 인스턴스에 퍼뜨려야 한다.
