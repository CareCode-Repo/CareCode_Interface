# 업로드 파일 보안

> 관련 이슈: #49

업로드는 건강기록 첨부와 프로필 이미지 두 곳이다. 저장은 `FileStorageService` 하나를 거친다.

## 검사 순서

| 단계 | 무엇을 | 실패 시 |
|------|--------|---------|
| 1. 크기 | 10MB (`STORAGE_MAX_FILE_SIZE`) | 400 |
| 2. 확장자 | jpg·jpeg·png·gif·webp·heic·pdf | 400 |
| 3. Content-Type | 위 형식의 MIME | 400 |
| 4. **내용(시그니처)** | 앞 16바이트가 확장자 형식인지 | 400 `C006` |
| 5. **악성코드** | ClamAV (켠 경우) | 400 `C007` / 검사기 장애 503 `C008` |

2·3 은 클라이언트가 정하는 값이라 속일 수 있다. HTML·SVG 를 `.png` 로 올리면 둘 다 통과하고,
브라우저가 내용을 보고 문서로 해석하면 저장형 XSS 가 된다. 4 가 이것을 막는다.

저장은 **검사한 바이트 그대로** 쓴다. 스트림을 다시 열면 검사한 것과 다른 내용이 저장될 여지가 있다.

## 악성코드 검사 켜기

기본은 꺼져 있다(`NoOpFileScanner`). 켜면 clamd 의 INSTREAM 명령으로 검사한다(`ClamAvFileScanner`, 추가 의존성 없음).

```bash
# 로컬
STORAGE_SCAN_ENABLED=true docker compose --profile scan up --build
```

운영은 서버에 clamd 를 띄우고 `STORAGE_SCAN_ENABLED=true`, `CLAMD_HOST`, `CLAMD_PORT` 를 준다.

**검사기에 닿지 못하면 업로드를 받지 않는다(fail-closed, 503).** 검사를 켠 환경에서 장애 때 조용히
통과시키면 켠 의미가 없다. 검사기 가용성이 업로드 가용성이 된다는 뜻이므로 clamd 를 헬스체크 대상에 넣는다.

## 내려줄 때

- 건강기록 첨부는 정적 경로(`/files/**`)로 공개하지 않는다. 소유권을 확인한 뒤 서버가 직접 내려준다.
- 항상 `Content-Disposition: attachment` — 브라우저가 페이지로 열지 않는다.
- Spring Security 기본 헤더 `X-Content-Type-Options: nosniff` 로 내용 추측을 막는다.
- 공개 경로는 프로필 이미지(`/files/profile-images/**`) 뿐이다.

## 남은 일

- 외부 저장소(S3 등) + 서명 URL. 저장소는 `FileStorageService` 로 추상화돼 있어 구현체만 추가하면 된다.
  비용과 계정이 필요한 결정이라 따로 진행한다. 다중 인스턴스로 늘리기 전에는 반드시 필요하다
  (지금은 로컬 디스크라 인스턴스끼리 파일을 공유하지 못한다).
