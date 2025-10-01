// 육아시설 페이지 JavaScript

class FacilitiesManager {
    constructor() {
        this.facilities = [];
        this.currentPage = 1;
        this.itemsPerPage = 12;
        this.currentView = 'grid';
        this.filters = {
            location: '',
            type: '',
            age: '',
            rating: ''
        };
        
        this.init();
    }

    init() {
        this.loadFacilities();
        this.bindEvents();
        this.setupViewToggle();
    }

    async loadFacilities() {
        try {
            // 스켈레톤 로딩 표시
            showSkeleton('#facilitiesList', 6);
            
            const response = await apiCall('/facilities');
            this.facilities = response;
            
            if (this.facilities.length === 0) {
                showEmptyState('#facilitiesList', '등록된 육아시설이 없습니다. 나중에 다시 확인해주세요.', 'home');
            } else {
                this.renderFacilities();
            }
            
            this.updateTotalCount();
        } catch (error) {
            console.error('시설 목록 로드 실패:', error);
            showErrorState('#facilitiesList', '시설 목록을 불러오는데 실패했습니다.', 'this.loadFacilities()');
        }
    }

    async searchFacilities() {
        const searchData = {
            location: this.filters.location,
            facilityType: this.filters.type,
            minAge: this.filters.age ? this.filters.age.split('-')[0] : null,
            maxAge: this.filters.age ? this.filters.age.split('-')[1] : null,
            minRating: this.filters.rating ? parseFloat(this.filters.rating) : null,
            page: this.currentPage,
            size: this.itemsPerPage,
            sortBy: document.getElementById('sortBy').value
        };

        try {
            // 스켈레톤 로딩 표시
            showSkeleton('#facilitiesList', 6);
            
            const response = await apiCall('/facilities/search', {
                method: 'POST',
                body: JSON.stringify(searchData)
            });
            
            this.facilities = response.content || response;
            
            if (this.facilities.length === 0) {
                showEmptyState('#facilitiesList', '검색 조건에 맞는 시설이 없습니다. 다른 조건으로 검색해보세요.', 'search');
            } else {
                this.renderFacilities();
                showToast('검색이 완료되었습니다.');
            }
            
            this.updateTotalCount();
        } catch (error) {
            console.error('시설 검색 실패:', error);
            showErrorState('#facilitiesList', '검색에 실패했습니다.', 'this.searchFacilities()');
        }
    }

    renderFacilities() {
        const container = document.getElementById('facilitiesList');
        const startIndex = (this.currentPage - 1) * this.itemsPerPage;
        const endIndex = startIndex + this.itemsPerPage;
        const facilitiesToShow = this.facilities.slice(startIndex, endIndex);

        if (facilitiesToShow.length === 0) {
            container.innerHTML = `
                <div class="col-span-full text-center py-12">
                    <i data-feather="search" class="w-16 h-16 text-gray-300 mx-auto mb-4"></i>
                    <h3 class="text-xl font-semibold text-gray-600 mb-2">검색 결과가 없습니다</h3>
                    <p class="text-gray-500">다른 검색 조건을 시도해보세요.</p>
                </div>
            `;
            if (typeof feather !== 'undefined') {
                feather.replace();
            }
            return;
        }

        container.innerHTML = facilitiesToShow.map(facility => this.createFacilityCard(facility)).join('');
        if (typeof feather !== 'undefined') {
            feather.replace();
        }
    }

    createFacilityCard(facility) {
        const ratingStars = renderRating(facility.rating || 0);
        const distance = facility.distance ? `${facility.distance}km` : '거리 정보 없음';
        
        return `
            <div class="card p-6 hover:shadow-lg transition-all duration-300">
                <div class="flex items-start justify-between mb-4">
                    <div class="flex-1">
                        <h3 class="text-lg font-semibold text-gray-800 mb-2">${facility.name}</h3>
                        <p class="text-sm text-gray-600 mb-2">${facility.location}</p>
                        <div class="flex items-center space-x-2 mb-2">
                            <span class="badge">${this.getFacilityTypeName(facility.facilityType)}</span>
                            <span class="text-sm text-gray-500">${distance}</span>
                        </div>
                    </div>
                    <div class="text-right">
                        <div class="flex items-center mb-1">
                            ${ratingStars}
                            <span class="ml-1 text-sm text-gray-600">(${facility.rating || 0})</span>
                        </div>
                        <p class="text-sm text-gray-500">${facility.viewCount || 0}명이 봤어요</p>
                    </div>
                </div>
                
                <div class="mb-4">
                    <p class="text-gray-700 text-sm line-clamp-2">${truncateText(facility.description || '설명이 없습니다.', 100)}</p>
                </div>
                
                <div class="flex items-center justify-between text-sm text-gray-600 mb-4">
                    <span>📞 ${facility.phone || '연락처 없음'}</span>
                    <span>🕒 ${facility.operatingHours || '운영시간 정보 없음'}</span>
                </div>
                
                <div class="flex space-x-2">
                    <button onclick="facilitiesManager.showFacilityDetail(${facility.id})" 
                            class="btn-secondary flex-1 text-sm">
                        상세보기
                    </button>
                    <button onclick="facilitiesManager.showBookingModal(${facility.id})" 
                            class="btn-primary flex-1 text-sm">
                        예약하기
                    </button>
                </div>
            </div>
        `;
    }

    getFacilityTypeName(type) {
        const typeMap = {
            'KINDERGARTEN': '유치원',
            'DAYCARE': '어린이집',
            'PLAYGROUP': '놀이방',
            'NURSERY': '보육원',
            'OTHER': '기타'
        };
        return typeMap[type] || type;
    }

    async showFacilityDetail(facilityId) {
        try {
            const facility = await apiCall(`/facilities/${facilityId}`);
            const modal = document.getElementById('facilityModal');
            const detailContainer = document.getElementById('facilityDetail');
            
            detailContainer.innerHTML = `
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <h4 class="text-2xl font-bold text-gray-800 mb-4">${facility.name}</h4>
                        <div class="space-y-3">
                            <div class="flex items-center">
                                <i data-feather="map-pin" class="w-4 h-4 mr-2 text-gray-500"></i>
                                <span>${facility.location}</span>
                            </div>
                            <div class="flex items-center">
                                <i data-feather="phone" class="w-4 h-4 mr-2 text-gray-500"></i>
                                <span>${facility.phone || '연락처 정보 없음'}</span>
                            </div>
                            <div class="flex items-center">
                                <i data-feather="clock" class="w-4 h-4 mr-2 text-gray-500"></i>
                                <span>${facility.operatingHours || '운영시간 정보 없음'}</span>
                            </div>
                            <div class="flex items-center">
                                <i data-feather="users" class="w-4 h-4 mr-2 text-gray-500"></i>
                                <span>수용 인원: ${facility.capacity || '정보 없음'}명</span>
                            </div>
                        </div>
                        
                        <div class="mt-6">
                            <h5 class="font-semibold mb-2">시설 정보</h5>
                            <div class="grid grid-cols-2 gap-2 text-sm">
                                <span class="badge">${this.getFacilityTypeName(facility.facilityType)}</span>
                                <span class="badge">연령: ${facility.minAge || 0}~${facility.maxAge || 7}세</span>
                                <span class="badge">평점: ${facility.rating || 0}/5</span>
                                <span class="badge">조회수: ${facility.viewCount || 0}</span>
                            </div>
                        </div>
                    </div>
                    
                    <div>
                        <h5 class="font-semibold mb-3">시설 설명</h5>
                        <p class="text-gray-700 mb-4">${facility.description || '설명이 없습니다.'}</p>
                        
                        <h5 class="font-semibold mb-3">편의시설</h5>
                        <div class="flex flex-wrap gap-2 mb-4">
                            ${this.renderAmenities(facility.amenities)}
                        </div>
                        
                        <h5 class="font-semibold mb-3">평점 및 리뷰</h5>
                        <div class="flex items-center mb-2">
                            ${renderRating(facility.rating || 0)}
                            <span class="ml-2 text-sm text-gray-600">(${facility.rating || 0}/5)</span>
                        </div>
                        <p class="text-sm text-gray-600">총 ${facility.reviewCount || 0}개의 리뷰</p>
                    </div>
                </div>
                
                <div class="border-t pt-6 mt-6">
                    <div class="flex space-x-4">
                        <button onclick="facilitiesManager.showBookingModal(${facility.id})" 
                                class="btn-primary">
                            예약하기
                        </button>
                        <button onclick="facilitiesManager.addToFavorites(${facility.id})" 
                                class="btn-secondary">
                            <i data-feather="heart" class="w-4 h-4 mr-2"></i>
                            찜하기
                        </button>
                        <button onclick="facilitiesManager.shareFacility(${facility.id})" 
                                class="btn-secondary">
                            <i data-feather="share-2" class="w-4 h-4 mr-2"></i>
                            공유하기
                        </button>
                    </div>
                </div>
            `;
            
            openModal('facilityModal');
            feather.replace();
            
            // 조회수 증가
            this.incrementViewCount(facilityId);
        } catch (error) {
            console.error('시설 상세 정보 로드 실패:', error);
            showToast('시설 정보를 불러오는데 실패했습니다.', 'error');
        }
    }

    renderAmenities(amenities) {
        if (!amenities || amenities.length === 0) {
            return '<span class="text-gray-500">정보 없음</span>';
        }
        
        const amenityIcons = {
            'PARKING': '🚗',
            'PLAYGROUND': '🎠',
            'LIBRARY': '📚',
            'ART_ROOM': '🎨',
            'MUSIC_ROOM': '🎵',
            'GARDEN': '🌱',
            'CAFETERIA': '🍽️',
            'MEDICAL_ROOM': '🏥'
        };
        
        return amenities.map(amenity => 
            `<span class="badge">${amenityIcons[amenity] || '🏠'} ${amenity}</span>`
        ).join('');
    }

    showBookingModal(facilityId) {
        this.currentFacilityId = facilityId;
        openModal('bookingModal');
    }

    async incrementViewCount(facilityId) {
        try {
            await apiCall(`/facilities/${facilityId}/view`, {
                method: 'POST'
            });
        } catch (error) {
            console.error('조회수 증가 실패:', error);
        }
    }

    addToFavorites(facilityId) {
        // 찜하기 기능 구현
        showToast('찜 목록에 추가되었습니다.');
    }

    shareFacility(facilityId) {
        // 공유하기 기능 구현
        if (navigator.share) {
            navigator.share({
                title: '맘편한 - 육아시설',
                text: '이 육아시설을 확인해보세요!',
                url: window.location.href
            });
        } else {
            // URL 복사
            navigator.clipboard.writeText(window.location.href);
            showToast('링크가 복사되었습니다.');
        }
    }

    updateTotalCount() {
        const totalCount = this.facilities.length;
        document.getElementById('totalCount').textContent = totalCount;
    }

    setupViewToggle() {
        const listViewBtn = document.getElementById('listViewBtn');
        const gridViewBtn = document.getElementById('gridViewBtn');
        const facilitiesList = document.getElementById('facilitiesList');

        listViewBtn.addEventListener('click', () => {
            this.currentView = 'list';
            facilitiesList.className = 'space-y-4';
            listViewBtn.className = 'p-2 rounded-lg bg-pink-500 text-white';
            gridViewBtn.className = 'p-2 rounded-lg bg-white border border-gray-300 hover:bg-gray-50';
        });

        gridViewBtn.addEventListener('click', () => {
            this.currentView = 'grid';
            facilitiesList.className = 'grid-responsive';
            gridViewBtn.className = 'p-2 rounded-lg bg-pink-500 text-white';
            listViewBtn.className = 'p-2 rounded-lg bg-white border border-gray-300 hover:bg-gray-50';
        });
    }

    bindEvents() {
        // 검색 버튼
        document.getElementById('searchBtn').addEventListener('click', () => {
            this.currentPage = 1;
            this.searchFacilities();
        });

        // 초기화 버튼
        document.getElementById('resetBtn').addEventListener('click', () => {
            this.resetFilters();
            this.loadFacilities();
        });

        // 필터 변경 시 자동 검색
        const filterInputs = ['locationFilter', 'typeFilter', 'ageFilter', 'ratingFilter'];
        filterInputs.forEach(id => {
            document.getElementById(id).addEventListener('change', (e) => {
                this.filters[id.replace('Filter', '')] = e.target.value;
            });
        });

        // 정렬 변경
        document.getElementById('sortBy').addEventListener('change', () => {
            this.searchFacilities();
        });

        // 예약 폼 제출
        document.getElementById('bookingForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            await this.submitBooking();
        });
    }

    resetFilters() {
        this.filters = {
            location: '',
            type: '',
            age: '',
            rating: ''
        };
        
        document.getElementById('locationFilter').value = '';
        document.getElementById('typeFilter').value = '';
        document.getElementById('ageFilter').value = '';
        document.getElementById('ratingFilter').value = '';
    }

    async submitBooking() {
        const formData = new FormData(document.getElementById('bookingForm'));
        const bookingData = {
            facilityId: this.currentFacilityId,
            bookingDate: formData.get('bookingDate'),
            bookingTime: formData.get('bookingTime'),
            bookingCount: parseInt(formData.get('bookingCount')),
            specialRequest: formData.get('specialRequest')
        };

        try {
            const response = await apiCall(`/facilities/${this.currentFacilityId}/bookings`, {
                method: 'POST',
                body: JSON.stringify(bookingData)
            });
            
            showToast('예약이 완료되었습니다!');
            closeModal('bookingModal');
            document.getElementById('bookingForm').reset();
        } catch (error) {
            console.error('예약 실패:', error);
            showToast('예약에 실패했습니다.', 'error');
        }
    }
}

// 페이지 로드 시 초기화
let facilitiesManager;
document.addEventListener('DOMContentLoaded', function() {
    facilitiesManager = new FacilitiesManager();
}); 