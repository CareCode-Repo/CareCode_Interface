#!/bin/bash

# CareCode API 문서 생성 스크립트
# 사용법: ./generate-asciidoc.sh [옵션]
# 옵션:
#   --list      : 간단한 API 목록 생성
#   --detailed  : 상세한 API 문서 생성 (기본값)
#   --all       : 모든 문서 생성

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 프로젝트 루트 디렉토리로 이동
cd "$(dirname "$0")/.."

# 출력 디렉토리 생성
OUTPUT_DIR="src/docs/asciidoc"
mkdir -p "$OUTPUT_DIR"

log_info "CareCode API 문서 생성을 시작합니다..."

# 애플리케이션이 실행 중인지 확인
check_app_running() {
    if curl -s http://localhost:8080/health > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# 애플리케이션 시작 대기
wait_for_app() {
    log_info "애플리케이션이 시작될 때까지 대기 중..."
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if check_app_running; then
            log_success "애플리케이션이 준비되었습니다!"
            return 0
        fi
        
        log_info "시도 $attempt/$max_attempts - 애플리케이션 대기 중..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    log_error "애플리케이션이 시작되지 않았습니다. 애플리케이션을 먼저 실행해주세요."
    exit 1
}

# 간단한 API 목록 생성
generate_api_list() {
    log_info "간단한 API 목록을 생성합니다..."
    
    ./gradlew generateApiList
    
    if [ $? -eq 0 ]; then
        log_success "API 목록 생성 완료: $OUTPUT_DIR/api-list.adoc"
    else
        log_error "API 목록 생성 실패"
        exit 1
    fi
}

# 상세한 API 문서 생성
generate_detailed_docs() {
    log_info "상세한 API 문서를 생성합니다..."
    
    # 애플리케이션이 실행 중인지 확인
    if ! check_app_running; then
        log_warning "애플리케이션이 실행되지 않았습니다. 애플리케이션을 먼저 시작해주세요."
        log_info "애플리케이션 시작 명령어: ./gradlew bootRun"
        exit 1
    fi
    
    ./gradlew generateDetailedApiDocs
    
    if [ $? -eq 0 ]; then
        log_success "상세 API 문서 생성 완료: $OUTPUT_DIR/api-docs-detailed.adoc"
    else
        log_error "상세 API 문서 생성 실패"
        exit 1
    fi
}

# HTML 및 PDF 생성
generate_outputs() {
    local adoc_file="$1"
    local base_name=$(basename "$adoc_file" .adoc)
    
    log_info "HTML 및 PDF 문서를 생성합니다..."
    
    # HTML 생성
    if command -v asciidoctor > /dev/null; then
        asciidoctor "$adoc_file" -o "$OUTPUT_DIR/${base_name}.html"
        log_success "HTML 생성 완료: $OUTPUT_DIR/${base_name}.html"
    else
        log_warning "asciidoctor가 설치되지 않았습니다. HTML 생성을 건너뜁니다."
        log_info "설치 명령어: brew install asciidoctor"
    fi
    
    # PDF 생성
    if command -v asciidoctor-pdf > /dev/null; then
        asciidoctor-pdf "$adoc_file" -o "$OUTPUT_DIR/${base_name}.pdf"
        log_success "PDF 생성 완료: $OUTPUT_DIR/${base_name}.pdf"
    else
        log_warning "asciidoctor-pdf가 설치되지 않았습니다. PDF 생성을 건너뜁니다."
        log_info "설치 명령어: gem install asciidoctor-pdf"
    fi
}

# 메인 로직
case "${1:---detailed}" in
    --list)
        generate_api_list
        generate_outputs "$OUTPUT_DIR/api-list.adoc"
        ;;
    --detailed)
        generate_detailed_docs
        generate_outputs "$OUTPUT_DIR/api-docs-detailed.adoc"
        ;;
    --all)
        generate_api_list
        generate_detailed_docs
        generate_outputs "$OUTPUT_DIR/api-list.adoc"
        generate_outputs "$OUTPUT_DIR/api-docs-detailed.adoc"
        ;;
    --help|-h)
        echo "CareCode API 문서 생성 스크립트"
        echo ""
        echo "사용법: $0 [옵션]"
        echo ""
        echo "옵션:"
        echo "  --list      간단한 API 목록 생성"
        echo "  --detailed  상세한 API 문서 생성 (기본값)"
        echo "  --all       모든 문서 생성"
        echo "  --help      이 도움말 표시"
        echo ""
        echo "예시:"
        echo "  $0 --list      # API 목록만 생성"
        echo "  $0 --detailed  # 상세 문서 생성"
        echo "  $0 --all       # 모든 문서 생성"
        ;;
    *)
        log_error "알 수 없는 옵션: $1"
        echo "사용법: $0 --help"
        exit 1
        ;;
esac

log_success "문서 생성이 완료되었습니다!"
log_info "생성된 파일들:"
ls -la "$OUTPUT_DIR"/*.adoc "$OUTPUT_DIR"/*.html "$OUTPUT_DIR"/*.pdf 2>/dev/null || true 