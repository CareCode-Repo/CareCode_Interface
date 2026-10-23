# 배포 시크릿 설정

`main` 에 머지해도 운영에 반영되지 않는 상태를 끝내기 위한 문서입니다. (#94)

## 지금 상태

리포지토리 시크릿이 **하나도 설정돼 있지 않습니다**(확인: `gh secret list` → 0개).
그래서 `main` 머지마다 `Deploy to Production` 이 첫 스텝에서 멈췄습니다.

```
success  Test and Build
success  Security Scan
success  Build Docker Image
failure  Deploy to Production     ← Check required secrets
```

#143~#147 까지 **다섯 번 연속** 이 모양이었습니다. 컨테이너를 띄우기 전에 멈추므로 운영
인스턴스를 건드리지는 않지만, 문제는 그 반대입니다 — **아무것도 배포되지 않는데 매번
빨간불로 끝나서 그 빨간불이 정상처럼 보입니다.**

그래서 워크플로를 먼저 고쳤습니다. 시크릿 상태를 세 가지로 구분합니다.

| 상태 | 결과 |
|------|------|
| 전부 설정됨 | 배포한다 |
| **하나도 없음** | **건너뛴다.** 실패가 아니고, 잡 요약에 "운영 배포를 건너뜀" 이 남는다 |
| 일부만 있음 | **실패한다.** 누군가 설정하다 멈춘 것이므로 조용히 넘어가면 안 된다 |

이제 미설정 상태에서도 파이프라인은 초록불이고, 배포가 안 됐다는 사실은 **모든 실행의
요약에 적힙니다.** 빨간불을 무시하게 되는 것보다 낫습니다.

## 설정할 것

### 필수 4개

| 이름 | 값 | 예 |
|------|-----|-----|
| `PRODUCTION_DEPLOY_HOST` | 배포 대상 서버 주소 | `43.200.1.2` 또는 `api.carecode.kr` |
| `PRODUCTION_DEPLOY_USER` | SSH 계정 | `ubuntu` |
| `PRODUCTION_SSH_KEY` | **SSH 개인키 전문** (`-----BEGIN ...` 부터 끝까지) | 파일에서 읽어 넣는다 |
| `PRODUCTION_HEALTH_URL` | 외부에서 보이는 주소. 워크플로가 뒤에 `/actuator/health` 를 붙인다 | `https://api.carecode.kr` |

### 권장 2개

| 이름 | 없으면 | 왜 넣는 게 좋은가 |
|------|--------|-------------------|
| `PRODUCTION_SSH_KNOWN_HOSTS` | `ssh-keyscan` 으로 대체하고 경고를 남긴다 | keyscan 은 최초 접속을 그냥 믿는다. 호스트키를 고정해야 중간자 공격을 막는다 |
| `OPS_SLACK_WEBHOOK_URL` | 잡 요약에만 남는다 | 배포 실패와 운영 알림이 사람에게 닿는다 |

### 넣는 방법

개인키를 **채팅·이슈·PR 에 붙여넣지 마세요.** 아래처럼 파일에서 바로 읽습니다.

```bash
cd CareCode_Interface

gh secret set PRODUCTION_DEPLOY_HOST --body '배포서버주소'
gh secret set PRODUCTION_DEPLOY_USER --body 'ubuntu'
gh secret set PRODUCTION_HEALTH_URL  --body 'https://배포서버주소'

# 개인키: 값을 터미널에 찍지 않고 파일에서 읽는다
gh secret set PRODUCTION_SSH_KEY < ~/.ssh/carecode_deploy

# 호스트키 고정 (서버가 떠 있어야 한다)
ssh-keyscan -H 배포서버주소 | gh secret set PRODUCTION_SSH_KNOWN_HOSTS
```

확인:

```bash
gh secret list          # 이름만 보인다. 값은 다시 볼 수 없다
```

## 배포용 키를 새로 만드는 경우

로그인용 키를 그대로 쓰지 말고 배포 전용 키를 따로 만듭니다. 유출 시 그 키만 폐기하면 됩니다.

```bash
# 로컬에서
ssh-keygen -t ed25519 -f ~/.ssh/carecode_deploy -C 'github-actions-deploy' -N ''

# 공개키를 서버에 등록
ssh-copy-id -i ~/.ssh/carecode_deploy.pub ubuntu@배포서버주소
```

서버에서 이 키의 권한을 좁히려면 `~/.ssh/authorized_keys` 의 해당 줄 앞에 제약을 붙입니다.

```
restrict,pty,command="..." ssh-ed25519 AAAA...
```

다만 지금 워크플로는 `docker login`·`docker run`·`curl` 등 여러 명령을 실행하므로
`command=` 로 하나만 묶을 수 없습니다. 묶으려면 서버에 배포 스크립트를 두고 그것만
허용하는 쪽으로 바꿔야 합니다 — 지금은 범위를 좁히지 못한 상태입니다.

## 서버에 미리 있어야 하는 것

워크플로는 서버에서 `docker` 를 직접 부릅니다.

```bash
# 서버에서 한 번
docker --version                 # 없으면 설치
sudo usermod -aG docker ubuntu   # sudo 없이 docker 를 쓸 수 있어야 한다 (재로그인 필요)
free -h                          # 스왑 확인 — 1GB 인스턴스는 스왑 2GB 가 필요하다
```

스왑이 없으면 기동 중 OOM 으로 죽습니다. 설정 명령과 근거(메모리 실측 포함)는
[`docs/features/operations.md` 의 "1GB 인스턴스(프리티어)에 올리기"](../features/operations.md#1gb-인스턴스프리티어에-올리기)
를 보세요.

## 첫 배포에서 확인할 것

시크릿을 넣은 뒤 `main` 에 머지하거나 `workflow_dispatch` 로 돌립니다. 순서는 이렇습니다.

1. `Check deploy secrets` — 초록. 여기서 "일부만" 이 나오면 빠진 이름이 로그에 찍힙니다
2. `Set up SSH` — 실패하면 키·계정 문제입니다
3. `Deploy` — 새 이미지를 예비 포트에서 먼저 띄워 헬스체크하고, 통과할 때만 교체합니다.
   실패하면 **이전 이미지 ID 로 되돌립니다**(태그가 아니라 ID 로 되돌리므로 같은 태그가
   덮어써졌어도 안전합니다)
4. `Verify from outside` — 서버 안에서는 UP 인데 밖에서 안 보이면 리버스 프록시나
   보안그룹 문제입니다. 메시지가 그렇게 구분해 알려줍니다

## 아직 남은 것

- **무중단은 아닙니다.** 교체 순간에 짧은 순단이 있습니다. 진짜 무중단은 nginx 를 제어할 수
  있어야 성립하고, 그 도구가 아직 없습니다. 깨진 이미지가 올라가는 일은 막습니다
- **SSH 키 권한을 좁히지 못했습니다.** 위 `command=` 항목 참고
- 운영 DB·Redis·메일·카카오 등 **애플리케이션 환경 변수**는 이 문서의 범위가 아닙니다.
  서버의 `.env` 로 넣고 있으며, 그쪽을 시크릿으로 옮기는 일은 따로 다뤄야 합니다
