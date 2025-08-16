// 정책 페이지 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('정책 페이지 로드됨');
    
    // 초기 데이터 로드
    loadPolicies();
    loadPolicyCategories();
    loadPolicyStats();
    
    // 이벤트 리스너 설정
    setupEventListeners();
});

// 정책 목록 로드
async function loadPolicies() {
    try {
        showSkeleton('#policies-container', 5);
        
        const response = await apiCall('/policies');
        
        if (response && response.length > 0) {
            renderPolicies(response);
        } else {
            showEmptyState('#policies-container', '등록된 정책이 없습니다.', 'file-text');
        }
    } catch (error) {
        console.error('정책 목록 로드 실패:', error);
        showErrorState('#policies-container', '정책을 불러오는데 실패했습니다.', loadPolicies);
    }
}

// 정책 카테고리 로드
async function loadPolicyCategories() {
    try {
        const response = await apiCall('/policies/categories');
        
        if (response && response.length > 0) {
            renderCategories(response);
        }
    } catch (error) {
        console.error('정책 카테고리 로드 실패:', error);
    }
}

// 정책 통계 로드
async function loadPolicyStats() {
    try {
        const response = await apiCall('/policies/statistics');
        
        if (response) {
            renderStats(response);
        }
    } catch (error) {
        console.error('정책 통계 로드 실패:', error);
    }
}

// 정책 렌더링
function renderPolicies(policies) {
    const container = document.getElementById('policies-container');
    if (!container) return;
    
    container.innerHTML = '';
    
    policies.forEach(policy => {
        const policyElement = createPolicyElement(policy);
        container.appendChild(policyElement);
    });
}

// 정책 요소 생성
function createPolicyElement(policy) {
    const policyDiv = document.createElement('div');
    policyDiv.className = 'bg-white rounded-lg shadow-md p-6 mb-4 hover-lift';
    policyDiv.innerHTML = `
        <div class="flex items-start justify-between mb-4">
            <div class="flex-1">
                <div class="flex items-center space-x-2 mb-2">
                    <span class="px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">
                        ${policy.category || '기타'}
                    </span>
                    <span class="px-2 py-1 text-xs font-medium bg-green-100 text-green-800 rounded-full">
                        ${policy.location || '전국'}
                    </span>
                </div>
                <h3 class="text-xl font-bold text-gray-900 mb-2 cursor-pointer hover:text-blue-600" 
                    onclick="viewPolicy(${policy.id})">
                    ${policy.title}
                </h3>
                <p class="text-gray-600 line-clamp-3 mb-3">${policy.description}</p>
            </div>
            <div class="flex items-center space-x-2 ml-4">
                <span class="text-sm text-gray-500">${formatDate(policy.createdAt)}</span>
            </div>
        </div>
        
        <div class="flex items-center justify-between text-sm text-gray-500">
            <div class="flex items-center space-x-4">
                <span class="flex items-center space-x-1">
                    <i data-feather="eye" class="w-4 h-4"></i>
                    <span>${policy.viewCount || 0}</span>
                </span>
                <span class="flex items-center space-x-1">
                    <i data-feather="users" class="w-4 h-4"></i>
                    <span>${policy.applicantCount || 0}명 지원</span>
                </span>
                <span class="flex items-center space-x-1">
                    <i data-feather="calendar" class="w-4 h-4"></i>
                    <span>${policy.deadline ? formatDate(policy.deadline) : '상시'}</span>
                </span>
            </div>
            <div class="flex items-center space-x-2">
                <span class="px-2 py-1 text-xs bg-yellow-100 text-yellow-800 rounded">
                    ${policy.supportAmount ? formatPrice(policy.supportAmount) : '정보 없음'}
                </span>
            </div>
        </div>
    `;
    
    return policyDiv;
}

// 카테고리 렌더링
function renderCategories(categories) {
    const container = document.getElementById('categories-container');
    if (!container) return;
    
    container.innerHTML = '';
    
    categories.forEach(category => {
        const categoryElement = document.createElement('div');
        categoryElement.className = 'flex items-center justify-between p-3 bg-white rounded-lg shadow-sm hover:shadow-md cursor-pointer';
        categoryElement.innerHTML = `
            <div class="flex items-center space-x-3">
                <div class="w-8 h-8 bg-gradient-to-r from-blue-400 to-purple-400 rounded-full flex items-center justify-center">
                    <i data-feather="folder" class="w-4 h-4 text-white"></i>
                </div>
                <div>
                    <h4 class="font-medium text-gray-900">${category.name}</h4>
                    <p class="text-sm text-gray-500">${category.description}</p>
                </div>
            </div>
            <div class="text-right">
                <div class="text-lg font-bold text-blue-600">${category.policyCount || 0}</div>
                <div class="text-xs text-gray-500">개 정책</div>
            </div>
        `;
        categoryElement.onclick = () => filterByCategory(category.id);
        container.appendChild(categoryElement);
    });
}

// 통계 렌더링
function renderStats(stats) {
    const container = document.getElementById('stats-container');
    if (!container) return;
    
    container.innerHTML = `
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div class="bg-gradient-to-r from-blue-400 to-blue-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">전체 정책</p>
                        <p class="text-2xl font-bold">${stats.totalPolicies || 0}</p>
                    </div>
                    <i data-feather="file-text" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
            <div class="bg-gradient-to-r from-green-400 to-green-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">신청 가능</p>
                        <p class="text-2xl font-bold">${stats.activePolicies || 0}</p>
                    </div>
                    <i data-feather="check-circle" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
            <div class="bg-gradient-to-r from-purple-400 to-purple-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">총 지원금</p>
                        <p class="text-2xl font-bold">${formatPrice(stats.totalSupportAmount || 0)}</p>
                    </div>
                    <i data-feather="dollar-sign" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
            <div class="bg-gradient-to-r from-orange-400 to-orange-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">신청자 수</p>
                        <p class="text-2xl font-bold">${stats.totalApplicants || 0}</p>
                    </div>
                    <i data-feather="users" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
        </div>
    `;
}

// 정책 상세 보기
async function viewPolicy(policyId) {
    try {
        const response = await apiCall(`/policies/${policyId}`);
        
        if (response) {
            openPolicyModal(response);
        }
    } catch (error) {
        console.error('정책 상세 조회 실패:', error);
        showToast('정책을 불러오는데 실패했습니다.', 'error');
    }
}

// 정책 모달 열기
function openPolicyModal(policy) {
    const modal = document.getElementById('policy-modal');
    const modalContent = document.getElementById('policy-modal-content');
    
    if (!modal || !modalContent) return;
    
    modalContent.innerHTML = `
        <div class="bg-white rounded-lg p-6 max-w-4xl mx-auto">
            <div class="flex items-center justify-between mb-6">
                <div class="flex items-center space-x-3">
                    <div class="w-12 h-12 bg-gradient-to-r from-blue-400 to-purple-400 rounded-full flex items-center justify-center">
                        <i data-feather="file-text" class="w-6 h-6 text-white"></i>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900">${policy.category || '기타'}</h3>
                        <p class="text-sm text-gray-500">${policy.location || '전국'}</p>
                    </div>
                </div>
                <button onclick="closeModal('policy-modal')" class="text-gray-400 hover:text-gray-600">
                    <i data-feather="x" class="w-6 h-6"></i>
                </button>
            </div>
            
            <div class="mb-6">
                <h1 class="text-2xl font-bold text-gray-900 mb-4">${policy.title}</h1>
                <div class="prose max-w-none">
                    <p class="text-gray-700 mb-4">${policy.description}</p>
                    
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                        <div class="bg-gray-50 rounded-lg p-4">
                            <h4 class="font-semibold text-gray-900 mb-2">지원 내용</h4>
                            <p class="text-gray-700">${policy.supportContent || '정보 없음'}</p>
                        </div>
                        <div class="bg-gray-50 rounded-lg p-4">
                            <h4 class="font-semibold text-gray-900 mb-2">지원 금액</h4>
                            <p class="text-gray-700">${policy.supportAmount ? formatPrice(policy.supportAmount) : '정보 없음'}</p>
                        </div>
                        <div class="bg-gray-50 rounded-lg p-4">
                            <h4 class="font-semibold text-gray-900 mb-2">신청 기간</h4>
                            <p class="text-gray-700">${policy.deadline ? formatDate(policy.deadline) : '상시'}</p>
                        </div>
                        <div class="bg-gray-50 rounded-lg p-4">
                            <h4 class="font-semibold text-gray-900 mb-2">신청 방법</h4>
                            <p class="text-gray-700">${policy.applicationMethod || '정보 없음'}</p>
                        </div>
                    </div>
                    
                    <div class="bg-blue-50 rounded-lg p-4 mb-4">
                        <h4 class="font-semibold text-blue-900 mb-2">신청 자격</h4>
                        <p class="text-blue-800">${policy.eligibility || '정보 없음'}</p>
                    </div>
                    
                    <div class="bg-yellow-50 rounded-lg p-4">
                        <h4 class="font-semibold text-yellow-900 mb-2">필요 서류</h4>
                        <p class="text-yellow-800">${policy.requiredDocuments || '정보 없음'}</p>
                    </div>
                </div>
            </div>
            
            <div class="border-t pt-4">
                <div class="flex items-center justify-between">
                    <div class="flex items-center space-x-4 text-sm text-gray-500">
                        <span class="flex items-center space-x-1">
                            <i data-feather="eye" class="w-4 h-4"></i>
                            <span>${policy.viewCount || 0} 조회</span>
                        </span>
                        <span class="flex items-center space-x-1">
                            <i data-feather="users" class="w-4 h-4"></i>
                            <span>${policy.applicantCount || 0}명 지원</span>
                        </span>
                    </div>
                    <button onclick="applyPolicy(${policy.id})" class="btn-primary">
                        신청하기
                    </button>
                </div>
            </div>
        </div>
    `;
    
    modal.classList.remove('hidden');
    feather.replace();
}

// 카테고리별 필터링
async function filterByCategory(categoryId) {
    try {
        showSkeleton('#policies-container', 3);
        
        const response = await apiCall(`/policies/categories/${categoryId}/policies`);
        
        if (response && response.length > 0) {
            renderPolicies(response);
        } else {
            showEmptyState('#policies-container', '해당 카테고리의 정책이 없습니다.', 'folder');
        }
    } catch (error) {
        console.error('카테고리 필터링 실패:', error);
        showToast('필터링에 실패했습니다.', 'error');
    }
}

// 정책 검색
async function searchPolicies() {
    const keyword = document.getElementById('search-input').value.trim();
    
    if (!keyword) {
        loadPolicies();
        return;
    }
    
    try {
        showSkeleton('#policies-container', 3);
        
        const response = await apiCall(`/policies/search?keyword=${encodeURIComponent(keyword)}`);
        
        if (response && response.length > 0) {
            renderPolicies(response);
        } else {
            showEmptyState('#policies-container', `"${keyword}"에 대한 검색 결과가 없습니다.`, 'search');
        }
    } catch (error) {
        console.error('정책 검색 실패:', error);
        showErrorState('#policies-container', '검색에 실패했습니다.', () => searchPolicies());
    }
}

// 정책 신청
async function applyPolicy(policyId) {
    try {
        // 로그인 확인
        showToast('로그인이 필요합니다.', 'info');
        // 실제 구현에서는 신청 API 호출
        // const response = await apiCall(`/policies/${policyId}/apply`, { method: 'POST' });
    } catch (error) {
        console.error('정책 신청 실패:', error);
        showToast('신청에 실패했습니다.', 'error');
    }
}

// 이벤트 리스너 설정
function setupEventListeners() {
    // 검색 버튼
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', searchPolicies);
    }
    
    // 검색 입력 필드 (엔터키)
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchPolicies();
            }
        });
    }
    
    // 필터 버튼들
    const filterButtons = document.querySelectorAll('.filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const filterType = this.dataset.filter;
            applyFilter(filterType);
            
            // 활성 버튼 표시
            filterButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

// 필터 적용
async function applyFilter(filterType) {
    try {
        showSkeleton('#policies-container', 3);
        
        let response;
        switch (filterType) {
            case 'latest':
                response = await apiCall('/policies/latest');
                break;
            case 'popular':
                response = await apiCall('/policies/popular');
                break;
            case 'deadline':
                response = await apiCall('/policies?sort=deadline');
                break;
            default:
                response = await apiCall('/policies');
        }
        
        if (response && response.length > 0) {
            renderPolicies(response);
        } else {
            showEmptyState('#policies-container', '해당 조건의 정책이 없습니다.', 'filter');
        }
    } catch (error) {
        console.error('필터 적용 실패:', error);
        showErrorState('#policies-container', '필터링에 실패했습니다.', () => applyFilter(filterType));
    }
}

// 유틸리티 함수들
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

function formatPrice(amount) {
    if (!amount) return '정보 없음';
    return new Intl.NumberFormat('ko-KR').format(amount) + '원';
}

// 모달 닫기
function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('hidden');
    }
}

// 페이지 로드 시 Feather 아이콘 초기화
document.addEventListener('DOMContentLoaded', function() {
    feather.replace();
}); 