// 건강 관리 페이지 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('건강 관리 페이지 로드됨');
    
    // 초기 데이터 로드
    loadHealthRecords();
    loadHealthStats();
    loadHealthGoals();
    
    // 이벤트 리스너 설정
    setupEventListeners();
    
    // 차트 초기화
    initializeCharts();
});

// 건강 기록 로드
async function loadHealthRecords() {
    try {
        showSkeleton('#health-records-container', 5);
        
        // 실제 구현에서는 사용자 ID를 사용
        const response = await apiCall('/health/records?userId=1');
        
        if (response && response.length > 0) {
            renderHealthRecords(response);
        } else {
            showEmptyState('#health-records-container', '등록된 건강 기록이 없습니다.', 'activity');
        }
    } catch (error) {
        console.error('건강 기록 로드 실패:', error);
        showErrorState('#health-records-container', '건강 기록을 불러오는데 실패했습니다.', loadHealthRecords);
    }
}

// 건강 통계 로드
async function loadHealthStats() {
    try {
        const response = await apiCall('/health/statistics?userId=1');
        
        if (response) {
            renderHealthStats(response);
        }
    } catch (error) {
        console.error('건강 통계 로드 실패:', error);
    }
}

// 건강 목표 로드
async function loadHealthGoals() {
    try {
        const response = await apiCall('/health/goals?userId=1');
        
        if (response) {
            renderHealthGoals(response);
        }
    } catch (error) {
        console.error('건강 목표 로드 실패:', error);
    }
}

// 건강 기록 렌더링
function renderHealthRecords(records) {
    const container = document.getElementById('health-records-container');
    if (!container) return;
    
    container.innerHTML = '';
    
    records.forEach(record => {
        const recordElement = createHealthRecordElement(record);
        container.appendChild(recordElement);
    });
}

// 건강 기록 요소 생성
function createHealthRecordElement(record) {
    const recordDiv = document.createElement('div');
    recordDiv.className = 'bg-white rounded-lg shadow-md p-6 mb-4 hover-lift';
    recordDiv.innerHTML = `
        <div class="flex items-start justify-between mb-4">
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-gradient-to-r from-green-400 to-blue-400 rounded-full flex items-center justify-center">
                    <i data-feather="${getHealthRecordIcon(record.recordType)}" class="w-5 h-5 text-white"></i>
                </div>
                <div>
                    <h3 class="font-semibold text-gray-900">${record.title}</h3>
                    <p class="text-sm text-gray-500">${formatDate(record.recordDate)}</p>
                </div>
            </div>
            <div class="flex items-center space-x-2">
                <span class="px-2 py-1 text-xs font-medium bg-green-100 text-green-800 rounded-full">
                    ${record.recordType}
                </span>
                ${record.isCompleted ? 
                    '<span class="px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">완료</span>' : 
                    '<span class="px-2 py-1 text-xs font-medium bg-yellow-100 text-yellow-800 rounded-full">예정</span>'
                }
            </div>
        </div>
        
        <div class="mb-4">
            <p class="text-gray-600 line-clamp-3">${record.description}</p>
        </div>
        
        <div class="flex items-center justify-between text-sm text-gray-500">
            <div class="flex items-center space-x-4">
                ${record.location ? `
                    <span class="flex items-center space-x-1">
                        <i data-feather="map-pin" class="w-4 h-4"></i>
                        <span>${record.location}</span>
                    </span>
                ` : ''}
                ${record.doctorName ? `
                    <span class="flex items-center space-x-1">
                        <i data-feather="user" class="w-4 h-4"></i>
                        <span>${record.doctorName}</span>
                    </span>
                ` : ''}
                ${record.nextDate ? `
                    <span class="flex items-center space-x-1">
                        <i data-feather="calendar" class="w-4 h-4"></i>
                        <span>다음: ${formatDate(record.nextDate)}</span>
                    </span>
                ` : ''}
            </div>
            <div class="flex items-center space-x-2">
                <button onclick="viewHealthRecord(${record.id})" class="text-blue-600 hover:text-blue-700">
                    상세보기
                </button>
                <button onclick="editHealthRecord(${record.id})" class="text-green-600 hover:text-green-700">
                    수정
                </button>
            </div>
        </div>
    `;
    
    return recordDiv;
}

// 건강 통계 렌더링
function renderHealthStats(stats) {
    const container = document.getElementById('health-stats-container');
    if (!container) return;
    
    container.innerHTML = `
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div class="bg-gradient-to-r from-green-400 to-green-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">완료된 기록</p>
                        <p class="text-2xl font-bold">${stats.completedRecords || 0}</p>
                    </div>
                    <i data-feather="check-circle" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
            <div class="bg-gradient-to-r from-blue-400 to-blue-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">예정된 기록</p>
                        <p class="text-2xl font-bold">${stats.pendingRecords || 0}</p>
                    </div>
                    <i data-feather="clock" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
            <div class="bg-gradient-to-r from-purple-400 to-purple-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">건강 점수</p>
                        <p class="text-2xl font-bold">${stats.healthScore || 0}/100</p>
                    </div>
                    <i data-feather="activity" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
            <div class="bg-gradient-to-r from-orange-400 to-orange-600 rounded-lg p-4 text-white">
                <div class="flex items-center justify-between">
                    <div>
                        <p class="text-sm opacity-90">다가오는 일정</p>
                        <p class="text-2xl font-bold">${stats.upcomingEvents || 0}</p>
                    </div>
                    <i data-feather="calendar" class="w-8 h-8 opacity-80"></i>
                </div>
            </div>
        </div>
    `;
}

// 건강 목표 렌더링
function renderHealthGoals(goals) {
    const container = document.getElementById('health-goals-container');
    if (!container) return;
    
    if (!goals || goals.length === 0) {
        container.innerHTML = `
            <div class="text-center py-8">
                <i data-feather="target" class="w-16 h-16 text-gray-300 mx-auto mb-4"></i>
                <h3 class="text-lg font-semibold text-gray-600 mb-2">건강 목표가 없습니다</h3>
                <p class="text-gray-500">새로운 건강 목표를 설정해보세요!</p>
            </div>
        `;
        return;
    }
    
    container.innerHTML = '';
    
    goals.forEach(goal => {
        const goalElement = document.createElement('div');
        goalElement.className = 'bg-white rounded-lg shadow-md p-4 mb-3';
        goalElement.innerHTML = `
            <div class="flex items-center justify-between mb-3">
                <h4 class="font-semibold text-gray-900">${goal.title}</h4>
                <span class="px-2 py-1 text-xs font-medium bg-blue-100 text-blue-800 rounded-full">
                    ${goal.progress || 0}%
                </span>
            </div>
            <div class="w-full bg-gray-200 rounded-full h-2 mb-2">
                <div class="bg-blue-600 h-2 rounded-full" style="width: ${goal.progress || 0}%"></div>
            </div>
            <p class="text-sm text-gray-600">${goal.description}</p>
        `;
        container.appendChild(goalElement);
    });
}

// 건강 기록 상세 보기
async function viewHealthRecord(recordId) {
    try {
        const response = await apiCall(`/health/records/${recordId}`);
        
        if (response) {
            openHealthRecordModal(response);
        }
    } catch (error) {
        console.error('건강 기록 상세 조회 실패:', error);
        showToast('건강 기록을 불러오는데 실패했습니다.', 'error');
    }
}

// 건강 기록 모달 열기
function openHealthRecordModal(record) {
    const modal = document.getElementById('health-record-modal');
    const modalContent = document.getElementById('health-record-modal-content');
    
    if (!modal || !modalContent) return;
    
    modalContent.innerHTML = `
        <div class="bg-white rounded-lg p-6 max-w-2xl mx-auto">
            <div class="flex items-center justify-between mb-6">
                <div class="flex items-center space-x-3">
                    <div class="w-12 h-12 bg-gradient-to-r from-green-400 to-blue-400 rounded-full flex items-center justify-center">
                        <i data-feather="${getHealthRecordIcon(record.recordType)}" class="w-6 h-6 text-white"></i>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900">${record.recordType}</h3>
                        <p class="text-sm text-gray-500">${formatDate(record.recordDate)}</p>
                    </div>
                </div>
                <button onclick="closeModal('health-record-modal')" class="text-gray-400 hover:text-gray-600">
                    <i data-feather="x" class="w-6 h-6"></i>
                </button>
            </div>
            
            <div class="mb-6">
                <h1 class="text-2xl font-bold text-gray-900 mb-4">${record.title}</h1>
                <div class="prose max-w-none">
                    <p class="text-gray-700 mb-4">${record.description}</p>
                    
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                        ${record.location ? `
                            <div class="bg-gray-50 rounded-lg p-4">
                                <h4 class="font-semibold text-gray-900 mb-2">진료 장소</h4>
                                <p class="text-gray-700">${record.location}</p>
                            </div>
                        ` : ''}
                        ${record.doctorName ? `
                            <div class="bg-gray-50 rounded-lg p-4">
                                <h4 class="font-semibold text-gray-900 mb-2">담당 의사</h4>
                                <p class="text-gray-700">${record.doctorName}</p>
                            </div>
                        ` : ''}
                        ${record.nextDate ? `
                            <div class="bg-gray-50 rounded-lg p-4">
                                <h4 class="font-semibold text-gray-900 mb-2">다음 일정</h4>
                                <p class="text-gray-700">${formatDate(record.nextDate)}</p>
                            </div>
                        ` : ''}
                        <div class="bg-gray-50 rounded-lg p-4">
                            <h4 class="font-semibold text-gray-900 mb-2">상태</h4>
                            <p class="text-gray-700">${record.isCompleted ? '완료' : '예정'}</p>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="border-t pt-4">
                <div class="flex items-center justify-between">
                    <div class="flex items-center space-x-4">
                        <button onclick="editHealthRecord(${record.id})" class="btn-secondary">
                            수정
                        </button>
                        <button onclick="deleteHealthRecord(${record.id})" class="btn-secondary text-red-600 hover:text-red-700">
                            삭제
                        </button>
                    </div>
                    <button onclick="completeHealthRecord(${record.id})" class="btn-primary">
                        ${record.isCompleted ? '완료 취소' : '완료 처리'}
                    </button>
                </div>
            </div>
        </div>
    `;
    
    modal.classList.remove('hidden');
    feather.replace();
}

// 건강 기록 수정
async function editHealthRecord(recordId) {
    try {
        const response = await apiCall(`/health/records/${recordId}`);
        
        if (response) {
            openEditHealthRecordModal(response);
        }
    } catch (error) {
        console.error('건강 기록 수정 실패:', error);
        showToast('건강 기록을 불러오는데 실패했습니다.', 'error');
    }
}

// 건강 기록 삭제
async function deleteHealthRecord(recordId) {
    if (!confirm('정말로 이 건강 기록을 삭제하시겠습니까?')) {
        return;
    }
    
    try {
        await apiCall(`/health/records/${recordId}`, { method: 'DELETE' });
        
        showToast('건강 기록이 삭제되었습니다.');
        closeModal('health-record-modal');
        loadHealthRecords();
    } catch (error) {
        console.error('건강 기록 삭제 실패:', error);
        showToast('건강 기록 삭제에 실패했습니다.', 'error');
    }
}

// 건강 기록 완료 처리
async function completeHealthRecord(recordId) {
    try {
        await apiCall(`/health/records/${recordId}/complete`, { method: 'PUT' });
        
        showToast('건강 기록 상태가 업데이트되었습니다.');
        closeModal('health-record-modal');
        loadHealthRecords();
        loadHealthStats();
    } catch (error) {
        console.error('건강 기록 완료 처리 실패:', error);
        showToast('상태 업데이트에 실패했습니다.', 'error');
    }
}

// 건강 기록 추가
async function addHealthRecord() {
    const formData = new FormData(document.getElementById('health-record-form'));
    
    const recordData = {
        title: formData.get('title'),
        description: formData.get('description'),
        recordType: formData.get('recordType'),
        recordDate: formData.get('recordDate'),
        nextDate: formData.get('nextDate'),
        location: formData.get('location'),
        doctorName: formData.get('doctorName'),
        childId: '1' // 실제 구현에서는 선택된 아이 ID 사용
    };
    
    try {
        await apiCall('/health/records', {
            method: 'POST',
            body: JSON.stringify(recordData)
        });
        
        showToast('건강 기록이 추가되었습니다.');
        closeModal('add-health-record-modal');
        document.getElementById('health-record-form').reset();
        loadHealthRecords();
        loadHealthStats();
    } catch (error) {
        console.error('건강 기록 추가 실패:', error);
        showToast('건강 기록 추가에 실패했습니다.', 'error');
    }
}

// 차트 초기화
function initializeCharts() {
    // 성장 차트
    const growthCtx = document.getElementById('growth-chart');
    if (growthCtx) {
        new Chart(growthCtx, {
            type: 'line',
            data: {
                labels: ['1개월', '2개월', '3개월', '4개월', '5개월', '6개월'],
                datasets: [{
                    label: '키 (cm)',
                    data: [50, 55, 60, 65, 68, 70],
                    borderColor: 'rgb(59, 130, 246)',
                    backgroundColor: 'rgba(59, 130, 246, 0.1)',
                    tension: 0.1
                }, {
                    label: '몸무게 (kg)',
                    data: [3.5, 5.0, 6.5, 7.5, 8.0, 8.5],
                    borderColor: 'rgb(16, 185, 129)',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    title: {
                        display: true,
                        text: '성장 추이'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
    
    // 건강 기록 타입별 분포 차트
    const recordTypeCtx = document.getElementById('record-type-chart');
    if (recordTypeCtx) {
        new Chart(recordTypeCtx, {
            type: 'doughnut',
            data: {
                labels: ['예방접종', '정기검진', '치료', '상담'],
                datasets: [{
                    data: [30, 25, 20, 25],
                    backgroundColor: [
                        'rgba(59, 130, 246, 0.8)',
                        'rgba(16, 185, 129, 0.8)',
                        'rgba(245, 158, 11, 0.8)',
                        'rgba(239, 68, 68, 0.8)'
                    ]
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    title: {
                        display: true,
                        text: '건강 기록 타입별 분포'
                    }
                }
            }
        });
    }
}

// 유틸리티 함수들
function getHealthRecordIcon(recordType) {
    const iconMap = {
        'VACCINATION': 'shield',
        'CHECKUP': 'activity',
        'TREATMENT': 'heart',
        'CONSULTATION': 'message-circle',
        'MEDICATION': 'pill'
    };
    return iconMap[recordType] || 'activity';
}

function formatDate(dateString) {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR');
}

// 이벤트 리스너 설정
function setupEventListeners() {
    // 건강 기록 추가 버튼
    const addRecordBtn = document.getElementById('add-health-record-btn');
    if (addRecordBtn) {
        addRecordBtn.addEventListener('click', () => {
            openModal('add-health-record-modal');
        });
    }
    
    // 건강 기록 폼 제출
    const healthRecordForm = document.getElementById('health-record-form');
    if (healthRecordForm) {
        healthRecordForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await addHealthRecord();
        });
    }
    
    // 필터 버튼들
    const filterButtons = document.querySelectorAll('.health-filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const filterType = this.dataset.filter;
            filterHealthRecords(filterType);
            
            // 활성 버튼 표시
            filterButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

// 건강 기록 필터링
async function filterHealthRecords(filterType) {
    try {
        showSkeleton('#health-records-container', 3);
        
        let response;
        switch (filterType) {
            case 'completed':
                response = await apiCall('/health/records?userId=1&completed=true');
                break;
            case 'pending':
                response = await apiCall('/health/records?userId=1&completed=false');
                break;
            case 'vaccination':
                response = await apiCall('/health/records?userId=1&recordType=VACCINATION');
                break;
            case 'checkup':
                response = await apiCall('/health/records?userId=1&recordType=CHECKUP');
                break;
            default:
                response = await apiCall('/health/records?userId=1');
        }
        
        if (response && response.length > 0) {
            renderHealthRecords(response);
        } else {
            showEmptyState('#health-records-container', '해당 조건의 건강 기록이 없습니다.', 'activity');
        }
    } catch (error) {
        console.error('건강 기록 필터링 실패:', error);
        showErrorState('#health-records-container', '필터링에 실패했습니다.', () => filterHealthRecords(filterType));
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

// 페이지 로드 시 Feather 아이콘 초기화
document.addEventListener('DOMContentLoaded', function() {
    feather.replace();
}); 