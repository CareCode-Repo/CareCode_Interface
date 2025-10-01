// 공통 JavaScript 함수들

// API 기본 URL 설정 (환경별 자동 감지)
const API_BASE_URL = window.location.origin;

// API 호출 함수
async function apiCall(url, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const config = { ...defaultOptions, ...options };

    // 토큰이 있으면 헤더에 추가
    const token = getToken();
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        // 로딩 상태 표시
        showLoading();
        
        // URL이 상대 경로인 경우 기본 URL 추가
        const fullUrl = url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
        const response = await fetch(fullUrl, config);
        
        // 응답 타입 확인
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('text/html')) {
            // HTML 응답이 오면 로그인 페이지로 리다이렉트된 것
            hideLoading();
            showToast('로그인이 필요합니다. 로그인 페이지로 이동합니다.', 'error');
            setTimeout(() => {
                window.location.href = '/user/login';
            }, 2000);
            throw new Error('Authentication required');
        }
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        hideLoading();
        return data;
    } catch (error) {
        console.error('API 호출 오류:', error);
        hideLoading();
        
        // 에러 타입에 따른 메시지
        let errorMessage = '오류가 발생했습니다. 다시 시도해주세요.';
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            errorMessage = '네트워크 연결을 확인해주세요.';
        } else if (error.message.includes('401') || error.message.includes('Authentication required')) {
            errorMessage = '로그인이 필요합니다.';
            // 토큰이 만료되었거나 유효하지 않은 경우 토큰 삭제
            removeToken();
            updateUserInfo();
        } else if (error.message.includes('403')) {
            errorMessage = '접근 권한이 없습니다.';
        } else if (error.message.includes('404')) {
            errorMessage = '요청한 정보를 찾을 수 없습니다.';
        } else if (error.message.includes('500')) {
            errorMessage = '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        }
        
        showToast(errorMessage, 'error');
        throw error;
    }
}

// 토큰 관리 함수들
function setToken(token) {
    localStorage.setItem('accessToken', token);
}

function getToken() {
    return localStorage.getItem('accessToken');
}

function removeToken() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
}

function setRefreshToken(token) {
    localStorage.setItem('refreshToken', token);
}

function getRefreshToken() {
    return localStorage.getItem('refreshToken');
}

// 사용자 정보 관리
function setUserInfo(user) {
    localStorage.setItem('userInfo', JSON.stringify(user));
}

function getUserInfo() {
    const userInfo = localStorage.getItem('userInfo');
    return userInfo && userInfo !== 'undefined' ? JSON.parse(userInfo) : null;
}

function removeUserInfo() {
    localStorage.removeItem('userInfo');
}

// 로그인 함수
async function login(email, password) {
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();

        if (data.success) {
            setToken(data.accessToken);
            setRefreshToken(data.refreshToken);
            if (data.user && data.user !== 'undefined') {
                setUserInfo(data.user);
            }
            updateUserInfo();
            showToast('로그인되었습니다.', 'success');
            return true;
        } else {
            showToast(data.message || '로그인에 실패했습니다.', 'error');
            return false;
        }
    } catch (error) {
        console.error('로그인 오류:', error);
        showToast('로그인 중 오류가 발생했습니다.', 'error');
        return false;
    }
}

// 로그아웃 함수
async function logout() {
    try {
        await apiCall('/auth/logout', { method: 'POST' });
    } catch (error) {
        console.error('로그아웃 API 오류:', error);
    }
    
    removeToken();
    removeUserInfo();
    updateUserInfo();
    showToast('로그아웃되었습니다.', 'success');
    window.location.href = '/user/login';
}

// 토큰 갱신 함수
async function refreshToken() {
    const refreshToken = getRefreshToken();
    if (!refreshToken) {
        throw new Error('Refresh token not found');
    }

    try {
        const response = await fetch('/auth/refresh', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ refreshToken })
        });

        if (!response.ok) {
            throw new Error('Token refresh failed');
        }

        const data = await response.json();
        setToken(data.accessToken);
        setRefreshToken(data.refreshToken);
        return data.accessToken;
    } catch (error) {
        console.error('토큰 갱신 실패:', error);
        removeToken();
        removeUserInfo();
        updateUserInfo();
        throw error;
    }
}

// 사용자 정보 업데이트 함수
function updateUserInfo() {
    const userInfo = getUserInfo();
    const userInfoElement = document.getElementById('user-info');
    const loginButton = document.getElementById('login-button');
    const logoutButton = document.getElementById('logout-button');

    if (userInfo && userInfoElement) {
        userInfoElement.innerHTML = `
            <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-gradient-to-r from-pink-400 to-purple-400 rounded-full flex items-center justify-center">
                    <i data-feather="user" class="w-4 h-4 text-white"></i>
                </div>
                <div class="text-sm">
                    <div class="font-medium text-gray-900">${userInfo.name || userInfo.email}님</div>
                    <div class="text-gray-500">환영합니다</div>
                </div>
            </div>
        `;
        
        if (loginButton) loginButton.style.display = 'none';
        if (logoutButton) logoutButton.style.display = 'block';
    } else {
        if (userInfoElement) {
            userInfoElement.innerHTML = `
                <div class="flex items-center space-x-3">
                    <div class="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                        <i data-feather="user" class="w-4 h-4 text-gray-500"></i>
                    </div>
                    <div class="text-sm text-gray-500">로그인이 필요합니다</div>
                </div>
            `;
        }
        
        if (loginButton) loginButton.style.display = 'block';
        if (logoutButton) logoutButton.style.display = 'none';
    }
    
    if (typeof feather !== 'undefined') {
        feather.replace();
    }
}

// 인증 상태 확인 함수
async function checkAuthStatus() {
    const token = getToken();
    if (!token) {
        updateUserInfo();
        return false;
    }

    try {
        const response = await fetch('/auth/me', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (response.ok) {
            const userInfo = await response.json();
            setUserInfo(userInfo);
            updateUserInfo();
            return true;
        } else {
            // 토큰이 만료된 경우 갱신 시도
            try {
                await refreshToken();
                return await checkAuthStatus();
            } catch (refreshError) {
                removeToken();
                removeUserInfo();
                updateUserInfo();
                return false;
            }
        }
    } catch (error) {
        console.error('인증 상태 확인 실패:', error);
        removeToken();
        removeUserInfo();
        updateUserInfo();
        return false;
    }
}

// 토스트 메시지 표시
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `fixed top-4 right-4 z-50 px-6 py-3 rounded-lg shadow-lg text-white transform transition-all duration-300 translate-x-full`;
    
    switch (type) {
        case 'success':
            toast.classList.add('bg-green-500');
            break;
        case 'error':
            toast.classList.add('bg-red-500');
            break;
        case 'warning':
            toast.classList.add('bg-yellow-500');
            break;
        default:
            toast.classList.add('bg-blue-500');
    }
    
    toast.textContent = message;
    document.body.appendChild(toast);
    
    // 애니메이션
    setTimeout(() => {
        toast.classList.remove('translate-x-full');
    }, 100);
    
    // 자동 제거
    setTimeout(() => {
        toast.classList.add('translate-x-full');
        setTimeout(() => {
            document.body.removeChild(toast);
        }, 300);
    }, 3000);
}

// 로딩 상태 표시
function showLoading() {
    let loading = document.getElementById('loading-overlay');
    if (!loading) {
        loading = document.createElement('div');
        loading.id = 'loading-overlay';
        loading.className = 'fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50';
        loading.innerHTML = `
            <div class="bg-white rounded-lg p-6 flex items-center space-x-3">
                <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-pink-500"></div>
                <span class="text-gray-700">로딩 중...</span>
            </div>
        `;
        document.body.appendChild(loading);
    }
    loading.style.display = 'flex';
}

// 로딩 상태 숨기기
function hideLoading() {
    const loading = document.getElementById('loading-overlay');
    if (loading) {
        loading.style.display = 'none';
    }
}

// 빈 상태 표시
function showEmptyState(container, message, icon = 'inbox') {
    const element = document.querySelector(container);
    if (element) {
        element.innerHTML = `
            <div class="text-center py-12">
                <i data-feather="${icon}" class="w-16 h-16 text-gray-300 mx-auto mb-4"></i>
                <h3 class="text-lg font-semibold text-gray-600 mb-2">${message}</h3>
                <p class="text-gray-500">새로운 내용을 추가해보세요!</p>
            </div>
        `;
        if (typeof feather !== 'undefined') {
            feather.replace();
        }
    }
}

// 에러 상태 표시
function showErrorState(container, message, retryFunction = null) {
    const element = document.querySelector(container);
    if (element) {
        element.innerHTML = `
            <div class="text-center py-12">
                <i data-feather="alert-circle" class="w-16 h-16 text-red-300 mx-auto mb-4"></i>
                <h3 class="text-lg font-semibold text-gray-600 mb-2">오류가 발생했습니다</h3>
                <p class="text-gray-500 mb-4">${message}</p>
                ${retryFunction ? `
                    <button onclick="${retryFunction.name}()" class="btn-primary">
                        다시 시도
                    </button>
                ` : ''}
            </div>
        `;
        if (typeof feather !== 'undefined') {
            feather.replace();
        }
    }
}

// 스켈레톤 로딩 표시
function showSkeleton(container, count = 3) {
    const element = document.querySelector(container);
    if (element) {
        element.innerHTML = Array(count).fill(`
            <div class="bg-white rounded-lg shadow-md p-6 mb-4 animate-pulse">
                <div class="flex items-start space-x-4">
                    <div class="w-10 h-10 bg-gray-200 rounded-full"></div>
                    <div class="flex-1 space-y-2">
                        <div class="h-4 bg-gray-200 rounded w-3/4"></div>
                        <div class="h-3 bg-gray-200 rounded w-1/2"></div>
                        <div class="h-3 bg-gray-200 rounded w-2/3"></div>
                    </div>
                </div>
            </div>
        `).join('');
    }
}

// 모달 열기
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('hidden');
    }
}

// 모달 닫기
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('hidden');
    }
}

// 날짜 포맷팅
function formatDate(dateString) {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) {
        return '어제';
    } else if (diffDays < 7) {
        return `${diffDays}일 전`;
    } else {
        return date.toLocaleDateString('ko-KR');
    }
}

// 평점 렌더링
function renderRating(rating, maxRating = 5) {
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;
    const emptyStars = maxRating - fullStars - (hasHalfStar ? 1 : 0);
    
    let stars = '';
    for (let i = 0; i < fullStars; i++) {
        stars += '<i data-feather="star" class="w-4 h-4 text-yellow-400 fill-current"></i>';
    }
    if (hasHalfStar) {
        stars += '<i data-feather="star" class="w-4 h-4 text-yellow-400 fill-current" style="clip-path: inset(0 50% 0 0);"></i>';
    }
    for (let i = 0; i < emptyStars; i++) {
        stars += '<i data-feather="star" class="w-4 h-4 text-gray-300"></i>';
    }
    
    return stars;
}

// 가격 포맷팅
function formatPrice(price) {
    if (!price) return '정보 없음';
    return new Intl.NumberFormat('ko-KR').format(price) + '원';
}

// 텍스트 자르기
function truncateText(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

// 폼 검증
function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;
    
    const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
    let isValid = true;
    
    inputs.forEach(input => {
        if (!input.value.trim()) {
            input.classList.add('border-red-500');
            isValid = false;
        } else {
            input.classList.remove('border-red-500');
        }
    });
    
    return isValid;
}

// URL 파라미터 관리
function getSearchParams() {
    const urlParams = new URLSearchParams(window.location.search);
    const params = {};
    for (const [key, value] of urlParams) {
        params[key] = value;
    }
    return params;
}

function setSearchParams(params) {
    const url = new URL(window.location);
    Object.keys(params).forEach(key => {
        url.searchParams.set(key, params[key]);
    });
    window.history.pushState({}, '', url);
}

// 로컬 스토리지 래퍼
const storage = {
    get: (key) => {
        try {
            return JSON.parse(localStorage.getItem(key));
        } catch {
            return localStorage.getItem(key);
        }
    },
    set: (key, value) => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch {
            localStorage.setItem(key, value);
        }
    },
    remove: (key) => {
        localStorage.removeItem(key);
    }
};

// 디바운스 함수
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 쓰로틀 함수
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// 키보드 네비게이션 설정
function setupKeyboardNavigation() {
    document.addEventListener('keydown', function(e) {
        // ESC 키로 모달 닫기
        if (e.key === 'Escape') {
            const modals = document.querySelectorAll('.modal:not(.hidden)');
            modals.forEach(modal => {
                closeModal(modal.id);
            });
        }
        
        // Enter 키로 폼 제출
        if (e.key === 'Enter' && e.target.tagName === 'INPUT') {
            const form = e.target.closest('form');
            if (form) {
                e.preventDefault();
                form.dispatchEvent(new Event('submit'));
            }
        }
    });
}

// 스크린 리더 지원 설정
function setupScreenReaderSupport() {
    // 버튼에 aria-label 추가
    const buttons = document.querySelectorAll('button:not([aria-label])');
    buttons.forEach(button => {
        if (!button.textContent.trim()) {
            const icon = button.querySelector('i[data-feather]');
            if (icon) {
                const iconName = icon.getAttribute('data-feather');
                button.setAttribute('aria-label', iconName);
            }
        }
    });
    
    // 이미지에 alt 텍스트 확인
    const images = document.querySelectorAll('img:not([alt])');
    images.forEach(img => {
        if (!img.alt) {
            img.setAttribute('alt', '이미지');
        }
    });
}

// 스크린 리더에 메시지 전달
function announceToScreenReader(message) {
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', 'polite');
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;
    document.body.appendChild(announcement);
    
    setTimeout(() => {
        document.body.removeChild(announcement);
    }, 1000);
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 인증 상태 확인
    checkAuthStatus();
    
    // 키보드 네비게이션 설정
    setupKeyboardNavigation();
    
    // 스크린 리더 지원 설정
    setupScreenReaderSupport();
    
    // Feather 아이콘 초기화
    if (typeof feather !== 'undefined') {
        feather.replace();
    }
    
    // 로그아웃 버튼 이벤트 리스너
    const logoutBtn = document.getElementById('logout-button');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', logout);
    }
    
    // 로그인 버튼 이벤트 리스너
    const loginBtn = document.getElementById('login-button');
    if (loginBtn) {
        loginBtn.addEventListener('click', function() {
            window.location.href = '/user/login';
        });
    }
}); 