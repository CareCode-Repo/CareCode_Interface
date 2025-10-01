#!/bin/bash

# CareCode Docker 배포 스크립트 (최적화 버전)
set -e

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 프로젝트 설정
PROJECT_NAME="carecode"
APP_NAME="carecode-app"
VERSION="${1:-latest}"
ENVIRONMENT="${2:-release}"

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 시스템 리소스 확인
check_system_resources() {
    log_info "시스템 리소스 확인 중..."
    
    # 메모리 확인 (최소 4GB 권장)
    TOTAL_MEM=$(free -m | awk 'NR==2{printf "%.0f", $2/1024}')
    if [ $TOTAL_MEM -lt 4 ]; then
        log_warning "메모리가 ${TOTAL_MEM}GB입니다. 최소 4GB 권장"
    fi
    
    # 디스크 공간 확인 (최소 10GB 권장)
    AVAILABLE_DISK=$(df -BG . | awk 'NR==2 {print $4}' | sed 's/G//')
    if [ $AVAILABLE_DISK -lt 10 ]; then
        log_warning "사용 가능한 디스크 공간이 ${AVAILABLE_DISK}GB입니다. 최소 10GB 권장"
    fi
    
    log_success "시스템 리소스 확인 완료"
}

# JAR 파일 빌드
build_jar() {
    log_info "JAR 파일 빌드 중..."
    
    # Gradle 실행 권한 확인
    if [ ! -x "./gradlew" ]; then
        chmod +x ./gradlew
    fi
    
    # 기존 빌드 정리
    ./gradlew clean
    
    # 최적화된 빌드 (테스트 스킵)
    ./gradlew bootJar -x test --no-daemon --parallel
    
    # JAR 파일 존재 확인
    if [ ! -f "build/libs/${APP_NAME}.jar" ]; then
        log_error "JAR 파일 빌드 실패"
        exit 1
    fi
    
    log_success "JAR 파일 빌드 완료"
}

# Docker 이미지 빌드
build_docker_image() {
    log_info "Docker 이미지 빌드 중..."
    
    # 이전 이미지 제거 (옵션)
    if [ "$3" = "--force" ]; then
        docker rmi ${APP_NAME}:${VERSION} 2>/dev/null || true
    fi
    
    # Docker 이미지 빌드 (멀티스테이지 사용)
    docker build \
        --build-arg VERSION=${VERSION} \
        --build-arg ENVIRONMENT=${ENVIRONMENT} \
        --no-cache \
        -t ${APP_NAME}:${VERSION} .
    
    log_success "Docker 이미지 빌드 완료"
}

# SSL 인증서 생성 (자체 서명)
generate_ssl_certificates() {
    log_info "SSL 인증서 생성 중..."
    
    if [ ! -d "nginx/ssl" ]; then
        mkdir -p nginx/ssl
    fi
    
    if [ ! -f "nginx/ssl/cert.pem" ] || [ ! -f "nginx/ssl/key.pem" ]; then
        # 자체 서명 인증서 생성
        openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
            -keyout nginx/ssl/key.pem \
            -out nginx/ssl/cert.pem \
            -subj "/C=KR/ST=Seoul/L=Seoul/O=CareCode/CN=localhost"
        
        log_success "SSL 인증서 생성 완료"
    else
        log_info "기존 SSL 인증서 사용"
    fi
}

# Docker Compose 서비스 시작
start_services() {
    log_info "Docker Compose 서비스 시작 중..."
    
    # SSL 인증서 생성
    generate_ssl_certificates
    
    # Docker Compose 실행 (백그라운드)
    docker-compose up -d --remove-orphans
    
    log_info "서비스 시작 대기 중..."
    sleep 30
    
    # 서비스 상태 확인
    check_service_health
}

# 서비스 헬스체크
check_service_health() {
    log_info "서비스 헬스체크 실행 중..."
    
    # MySQL 헬스체크
    for i in {1..30}; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost --silent; then
            log_success "MySQL 서비스 정상"
            break
        fi
        if [ $i -eq 30 ]; then
            log_error "MySQL 서비스 응답 없음"
            return 1
        fi
        sleep 2
    done
    
    # Redis 헬스체크
    for i in {1..15}; do
        if docker-compose exec -T redis redis-cli ping | grep -q PONG; then
            log_success "Redis 서비스 정상"
            break
        fi
        if [ $i -eq 15 ]; then
            log_error "Redis 서비스 응답 없음"
            return 1
        fi
        sleep 2
    done
    
    # Spring Boot 앱 헬스체크
    for i in {1..60}; do
        if curl -f http://localhost:8080/health >/dev/null 2>&1; then
            log_success "Spring Boot 애플리케이션 정상"
            break
        fi
        if [ $i -eq 60 ]; then
            log_error "Spring Boot 애플리케이션 응답 없음"
            return 1
        fi
        sleep 3
    done
    
    # Nginx 헬스체크
    for i in {1..10}; do
        if curl -f http://localhost:80 >/dev/null 2>&1; then
            log_success "Nginx 서비스 정상"
            break
        fi
        if [ $i -eq 10 ]; then
            log_warning "Nginx 서비스 응답 없음 (일부 기능만 사용 가능)"
        fi
        sleep 2
    done
}

# 서비스 중지
stop_services() {
    log_info "서비스 중지 중..."
    docker-compose down
    log_success "서비스 중지 완료"
}

# 서비스 재시작
restart_services() {
    log_info "서비스 재시작 중..."
    stop_services
    start_services
    log_success "서비스 재시작 완료"
}

# 로그 확인
show_logs() {
    local service=${1:-app}
    log_info "${service} 서비스 로그 확인:"
    docker-compose logs -f --tail=100 $service
}

# 시스템 정리
cleanup() {
    log_info "시스템 정리 중..."
    
    # 중지된 컨테이너 제거
    docker container prune -f
    
    # 사용하지 않는 이미지 제거
    docker image prune -f
    
    # 사용하지 않는 네트워크 제거
    docker network prune -f
    
    # 사용하지 않는 볼륨 제거 (주의: 데이터 손실 가능)
    if [ "$1" = "--volumes" ]; then
        log_warning "볼륨 데이터가 삭제됩니다."
        read -p "계속하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker volume prune -f
        fi
    fi
    
    log_success "시스템 정리 완료"
}

# 백업
backup_data() {
    log_info "데이터 백업 중..."
    
    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p $BACKUP_DIR
    
    # MySQL 백업
    docker-compose exec -T mysql mysqldump -u carecode -pcarecode123 carecode > ${BACKUP_DIR}/mysql_backup.sql
    
    # 로그 백업
    docker-compose logs > ${BACKUP_DIR}/docker_logs.txt
    
    log_success "백업 완료: $BACKUP_DIR"
}

# 사용법 출력
usage() {
    echo "사용법: $0 <command> [options]"
    echo ""
    echo "Commands:"
    echo "  build          JAR 파일과 Docker 이미지 빌드"
    echo "  start          서비스 시작"
    echo "  stop           서비스 중지"
    echo "  restart        서비스 재시작"
    echo "  logs [service] 로그 확인 (기본: app)"
    echo "  status         서비스 상태 확인"
    echo "  cleanup        시스템 정리"
    echo "  backup         데이터 백업"
    echo "  deploy         전체 배포 (build + start)"
    echo ""
    echo "Options:"
    echo "  --force        강제 실행"
    echo "  --volumes      볼륨까지 정리 (cleanup 시)"
    echo ""
    echo "Examples:"
    echo "  $0 deploy                # 전체 배포"
    echo "  $0 logs app              # 앱 로그 확인"
    echo "  $0 cleanup --volumes     # 볼륨까지 정리"
}

# 메인 실행 로직
main() {
    case "$1" in
        "build")
            check_system_resources
            build_jar
            build_docker_image
            ;;
        "start")
            start_services
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            restart_services
            ;;
        "logs")
            show_logs $2
            ;;
        "status")
            docker-compose ps
            ;;
        "cleanup")
            cleanup $2
            ;;
        "backup")
            backup_data
            ;;
        "deploy")
            check_system_resources
            build_jar
            build_docker_image
            start_services
            log_success "배포 완료!"
            log_info "애플리케이션 접속: http://localhost"
            ;;
        *)
            usage
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@"