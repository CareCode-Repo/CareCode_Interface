#!/bin/bash

# CareCode Blue/Green 배포 모니터링 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

# 컨테이너 상태 확인
check_container_status() {
    local container_name=$1
    
    if docker ps --format "table {{.Names}}\t{{.Status}}" | grep -q "$container_name"; then
        local status=$(docker ps --format "table {{.Names}}\t{{.Status}}" | grep "$container_name" | awk '{print $2}')
        echo "$status"
    else
        echo "not running"
    fi
}

# 헬스체크
check_health() {
    local environment=$1
    local max_attempts=5
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "http://localhost/health" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
        ((attempt++))
    done
    
    return 1
}

# 메트릭 수집
collect_metrics() {
    local environment=$1
    
    # CPU 사용률
    local cpu_usage=$(docker stats --no-stream --format "table {{.CPUPerc}}" carecode-$environment | tail -n 1)
    
    # 메모리 사용률
    local memory_usage=$(docker stats --no-stream --format "table {{.MemPerc}}" carecode-$environment | tail -n 1)
    
    # 네트워크 사용량
    local network_io=$(docker stats --no-stream --format "table {{.NetIO}}" carecode-$environment | tail -n 1)
    
    echo "CPU: $cpu_usage, Memory: $memory_usage, Network: $network_io"
}

# 로그 모니터링
monitor_logs() {
    local environment=$1
    local lines=10
    
    log_info "$environment 환경 최근 로그:"
    docker logs --tail $lines carecode-$environment 2>/dev/null || echo "로그를 가져올 수 없습니다."
}

# 메인 모니터링 함수
monitor_deployment() {
    log_info "CareCode Blue/Green 배포 모니터링 시작"
    echo "=========================================="
    
    # Blue 환경 상태
    log_info "Blue 환경 상태:"
    local blue_status=$(check_container_status "carecode-blue")
    echo "  컨테이너 상태: $blue_status"
    
    if check_health "blue"; then
        log_success "  헬스체크: 정상"
    else
        log_error "  헬스체크: 실패"
    fi
    
    local blue_metrics=$(collect_metrics "blue")
    echo "  메트릭: $blue_metrics"
    
    # Green 환경 상태
    log_info "Green 환경 상태:"
    local green_status=$(check_container_status "carecode-green")
    echo "  컨테이너 상태: $green_status"
    
    if check_health "green"; then
        log_success "  헬스체크: 정상"
    else
        log_error "  헬스체크: 실패"
    fi
    
    local green_metrics=$(collect_metrics "green")
    echo "  메트릭: $green_metrics"
    
    # Nginx 상태
    log_info "Nginx 상태:"
    local nginx_status=$(check_container_status "carecode-nginx")
    echo "  컨테이너 상태: $nginx_status"
    
    # MariaDB 상태
    log_info "MariaDB 상태:"
    local mariadb_status=$(check_container_status "carecode-mariadb")
    echo "  컨테이너 상태: $mariadb_status"
    
    # Redis 상태
    log_info "Redis 상태:"
    local redis_status=$(check_container_status "carecode-redis")
    echo "  컨테이너 상태: $redis_status"
    
    echo "=========================================="
    
    # 현재 활성 환경 확인
    local active_env=$(cat .active_environment 2>/dev/null || echo "blue")
    log_info "현재 활성 환경: $active_env"
    
    # 로그 모니터링
    echo ""
    log_info "활성 환경 로그 모니터링:"
    monitor_logs $active_env
}

# 실시간 모니터링
monitor_realtime() {
    log_info "실시간 모니터링 시작 (Ctrl+C로 종료)"
    
    while true; do
        clear
        monitor_deployment
        sleep 5
    done
}

# 메인 로직
case "${1:-status}" in
    "status")
        monitor_deployment
        ;;
    "realtime")
        monitor_realtime
        ;;
    "logs")
        local environment=${2:-blue}
        log_info "$environment 환경 로그:"
        docker logs -f carecode-$environment
        ;;
    *)
        echo "사용법: $0 [status|realtime|logs [environment]]"
        echo ""
        echo "명령어:"
        echo "  status              - 현재 배포 상태 확인"
        echo "  realtime           - 실시간 모니터링"
        echo "  logs [environment] - 특정 환경 로그 확인"
        echo ""
        echo "예시:"
        echo "  $0 status"
        echo "  $0 realtime"
        echo "  $0 logs blue"
        echo "  $0 logs green"
        exit 1
        ;;
esac 