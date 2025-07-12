#!/bin/bash

# CareCode Blue/Green 배포 스크립트
# 사용법: ./blue-green-deploy.sh [blue|green] [build|deploy|switch|rollback]

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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

# 현재 활성 환경 확인
get_active_environment() {
    # Nginx 설정에서 현재 활성 환경 확인
    # 실제 구현에서는 Redis나 데이터베이스에서 상태를 확인
    echo "blue"
}

# 헬스체크 함수
health_check() {
    local environment=$1
    local max_attempts=30
    local attempt=1
    
    log_info "헬스체크 시작: $environment 환경"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost/health" > /dev/null; then
            log_success "$environment 환경이 정상입니다."
            return 0
        fi
        
        log_warning "헬스체크 시도 $attempt/$max_attempts 실패"
        sleep 2
        ((attempt++))
    done
    
    log_error "$environment 환경 헬스체크 실패"
    return 1
}

# 환경 빌드
build_environment() {
    local environment=$1
    
    log_info "$environment 환경 빌드 시작"
    
    # Docker 이미지 빌드
    docker-compose -f docker-compose.blue-green.yml build carecode-$environment
    
    log_success "$environment 환경 빌드 완료"
}

# 환경 배포
deploy_environment() {
    local environment=$1
    
    log_info "$environment 환경 배포 시작"
    
    # 컨테이너 시작
    docker-compose -f docker-compose.blue-green.yml up -d carecode-$environment
    
    # 헬스체크 대기
    sleep 10
    
    # 헬스체크 수행
    if health_check $environment; then
        log_success "$environment 환경 배포 완료"
    else
        log_error "$environment 환경 배포 실패"
        exit 1
    fi
}

# 환경 전환
switch_environment() {
    local target_environment=$1
    local current_environment=$(get_active_environment)
    
    if [ "$target_environment" = "$current_environment" ]; then
        log_warning "이미 $target_environment 환경이 활성화되어 있습니다."
        return 0
    fi
    
    log_info "환경 전환 시작: $current_environment → $target_environment"
    
    # Nginx 설정 업데이트 (실제 구현에서는 Redis나 데이터베이스 사용)
    # 여기서는 간단한 예시로 파일 기반 전환
    echo "$target_environment" > .active_environment
    
    # Nginx 설정 리로드
    docker-compose -f docker-compose.blue-green.yml exec nginx nginx -s reload
    
    log_success "환경 전환 완료: $target_environment"
}

# 롤백
rollback() {
    local current_environment=$(get_active_environment)
    local rollback_environment=""
    
    if [ "$current_environment" = "blue" ]; then
        rollback_environment="green"
    else
        rollback_environment="blue"
    fi
    
    log_warning "롤백 시작: $current_environment → $rollback_environment"
    switch_environment $rollback_environment
}

# 메인 로직
main() {
    local action=$1
    local environment=$2
    
    case $action in
        "build")
            if [ -z "$environment" ]; then
                log_error "환경을 지정해주세요 (blue 또는 green)"
                exit 1
            fi
            build_environment $environment
            ;;
        "deploy")
            if [ -z "$environment" ]; then
                log_error "환경을 지정해주세요 (blue 또는 green)"
                exit 1
            fi
            deploy_environment $environment
            ;;
        "switch")
            if [ -z "$environment" ]; then
                log_error "전환할 환경을 지정해주세요 (blue 또는 green)"
                exit 1
            fi
            switch_environment $environment
            ;;
        "rollback")
            rollback
            ;;
        *)
            echo "사용법: $0 [blue|green] [build|deploy|switch|rollback]"
            echo ""
            echo "명령어:"
            echo "  build [environment]   - 지정된 환경 빌드"
            echo "  deploy [environment]  - 지정된 환경 배포"
            echo "  switch [environment]  - 지정된 환경으로 전환"
            echo "  rollback             - 이전 환경으로 롤백"
            echo ""
            echo "예시:"
            echo "  $0 blue build"
            echo "  $0 green deploy"
            echo "  $0 green switch"
            echo "  $0 rollback"
            exit 1
            ;;
    esac
}

# 스크립트 실행
main "$@" 