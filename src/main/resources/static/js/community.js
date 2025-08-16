// 커뮤니티 페이지 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('커뮤니티 페이지 로드됨');
    
    // 초기 데이터 로드
    loadPosts();
    loadTags();
    
    // 이벤트 리스너 설정
    setupEventListeners();
});

// 게시글 목록 로드
async function loadPosts() {
    try {
        showSkeleton('#posts-container', 5);
        
        const response = await apiCall('/community/posts');
        
        if (response && response.length > 0) {
            renderPosts(response);
        } else {
            showEmptyState('#posts-container', '아직 게시글이 없습니다.', 'message-circle');
        }
    } catch (error) {
        console.error('게시글 목록 로드 실패:', error);
        showErrorState('#posts-container', '게시글을 불러오는데 실패했습니다.', loadPosts);
    }
}

// 태그 목록 로드
async function loadTags() {
    try {
        const response = await apiCall('/community/tags');
        
        if (response && response.length > 0) {
            renderTags(response);
        }
    } catch (error) {
        console.error('태그 목록 로드 실패:', error);
    }
}

// 게시글 렌더링
function renderPosts(posts) {
    const container = document.getElementById('posts-container');
    if (!container) return;
    
    container.innerHTML = '';
    
    posts.forEach(post => {
        const postElement = createPostElement(post);
        container.appendChild(postElement);
    });
}

// 게시글 요소 생성
function createPostElement(post) {
    const postDiv = document.createElement('div');
    postDiv.className = 'bg-white rounded-lg shadow-md p-6 mb-4 hover-lift';
    postDiv.innerHTML = `
        <div class="flex items-start justify-between mb-4">
            <div class="flex items-center space-x-3">
                <div class="w-10 h-10 bg-gradient-to-r from-pink-400 to-purple-400 rounded-full flex items-center justify-center">
                    <i data-feather="user" class="w-5 h-5 text-white"></i>
                </div>
                <div>
                    <h3 class="font-semibold text-gray-900">${post.authorName || '익명'}</h3>
                    <p class="text-sm text-gray-500">${formatDate(post.createdAt)}</p>
                </div>
            </div>
            <div class="flex items-center space-x-2">
                <span class="px-2 py-1 text-xs font-medium bg-pink-100 text-pink-800 rounded-full">
                    ${post.category}
                </span>
            </div>
        </div>
        
        <div class="mb-4">
            <h2 class="text-xl font-bold text-gray-900 mb-2 cursor-pointer hover:text-pink-600" 
                onclick="viewPost(${post.id})">
                ${post.title}
            </h2>
            <p class="text-gray-600 line-clamp-3">${post.content}</p>
        </div>
        
        <div class="flex items-center justify-between text-sm text-gray-500">
            <div class="flex items-center space-x-4">
                <span class="flex items-center space-x-1">
                    <i data-feather="eye" class="w-4 h-4"></i>
                    <span>${post.viewCount || 0}</span>
                </span>
                <span class="flex items-center space-x-1">
                    <i data-feather="message-circle" class="w-4 h-4"></i>
                    <span>${post.commentCount || 0}</span>
                </span>
                <span class="flex items-center space-x-1">
                    <i data-feather="heart" class="w-4 h-4"></i>
                    <span>${post.likeCount || 0}</span>
                </span>
            </div>
            <div class="flex items-center space-x-2">
                ${post.tags ? post.tags.map(tag => 
                    `<span class="px-2 py-1 text-xs bg-gray-100 text-gray-600 rounded">${tag.name}</span>`
                ).join('') : ''}
            </div>
        </div>
    `;
    
    return postDiv;
}

// 태그 렌더링
function renderTags(tags) {
    const container = document.getElementById('tags-container');
    if (!container) return;
    
    container.innerHTML = '';
    
    tags.forEach(tag => {
        const tagElement = document.createElement('span');
        tagElement.className = 'inline-block px-3 py-1 bg-pink-100 text-pink-800 rounded-full text-sm font-medium mr-2 mb-2 cursor-pointer hover:bg-pink-200';
        tagElement.textContent = tag.name;
        tagElement.onclick = () => filterByTag(tag.id);
        container.appendChild(tagElement);
    });
}

// 게시글 상세 보기
async function viewPost(postId) {
    try {
        const response = await apiCall(`/community/posts/${postId}`);
        
        if (response) {
            openPostModal(response);
        }
    } catch (error) {
        console.error('게시글 상세 조회 실패:', error);
        showToast('게시글을 불러오는데 실패했습니다.', 'error');
    }
}

// 게시글 모달 열기
function openPostModal(post) {
    const modal = document.getElementById('post-modal');
    const modalContent = document.getElementById('post-modal-content');
    
    if (!modal || !modalContent) return;
    
    modalContent.innerHTML = `
        <div class="bg-white rounded-lg p-6 max-w-4xl mx-auto">
            <div class="flex items-center justify-between mb-6">
                <div class="flex items-center space-x-3">
                    <div class="w-12 h-12 bg-gradient-to-r from-pink-400 to-purple-400 rounded-full flex items-center justify-center">
                        <i data-feather="user" class="w-6 h-6 text-white"></i>
                    </div>
                    <div>
                        <h3 class="font-semibold text-gray-900">${post.authorName || '익명'}</h3>
                        <p class="text-sm text-gray-500">${formatDate(post.createdAt)}</p>
                    </div>
                </div>
                <button onclick="closeModal('post-modal')" class="text-gray-400 hover:text-gray-600">
                    <i data-feather="x" class="w-6 h-6"></i>
                </button>
            </div>
            
            <div class="mb-6">
                <h1 class="text-2xl font-bold text-gray-900 mb-4">${post.title}</h1>
                <div class="prose max-w-none">
                    ${post.content}
                </div>
            </div>
            
            <div class="border-t pt-4">
                <h4 class="font-semibold text-gray-900 mb-3">댓글</h4>
                <div id="comments-container" class="space-y-3">
                    ${post.comments ? post.comments.map(comment => `
                        <div class="bg-gray-50 rounded-lg p-3">
                            <div class="flex items-center space-x-2 mb-2">
                                <span class="font-medium text-sm">${comment.authorName || '익명'}</span>
                                <span class="text-xs text-gray-500">${formatDate(comment.createdAt)}</span>
                            </div>
                            <p class="text-gray-700">${comment.content}</p>
                        </div>
                    `).join('') : '<p class="text-gray-500">아직 댓글이 없습니다.</p>'}
                </div>
            </div>
        </div>
    `;
    
    modal.classList.remove('hidden');
    feather.replace();
}

// 태그로 필터링
async function filterByTag(tagId) {
    try {
        const response = await apiCall(`/community/posts?tagId=${tagId}`);
        
        if (response && response.length > 0) {
            renderPosts(response);
        } else {
            showEmptyState('#posts-container', '해당 태그의 게시글이 없습니다.', 'tag');
        }
    } catch (error) {
        console.error('태그 필터링 실패:', error);
        showToast('필터링에 실패했습니다.', 'error');
    }
}

// 게시글 검색
async function searchPosts() {
    const keyword = document.getElementById('search-input').value.trim();
    
    if (!keyword) {
        loadPosts();
        return;
    }
    
    try {
        showSkeleton('#posts-container', 3);
        
        const response = await apiCall(`/community/posts/search?keyword=${encodeURIComponent(keyword)}`);
        
        if (response && response.length > 0) {
            renderPosts(response);
        } else {
            showEmptyState('#posts-container', `"${keyword}"에 대한 검색 결과가 없습니다.`, 'search');
        }
    } catch (error) {
        console.error('게시글 검색 실패:', error);
        showErrorState('#posts-container', '검색에 실패했습니다.', () => searchPosts());
    }
}

// 이벤트 리스너 설정
function setupEventListeners() {
    // 검색 버튼
    const searchBtn = document.getElementById('search-btn');
    if (searchBtn) {
        searchBtn.addEventListener('click', searchPosts);
    }
    
    // 검색 입력 필드 (엔터키)
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchPosts();
            }
        });
    }
    
    // 새 게시글 작성 버튼
    const writeBtn = document.getElementById('write-post-btn');
    if (writeBtn) {
        writeBtn.addEventListener('click', () => {
            // 로그인 확인 후 게시글 작성 페이지로 이동
            showToast('로그인이 필요합니다.', 'info');
        });
    }
    
    // 카테고리 탭
    const categoryTabs = document.querySelectorAll('.category-tab');
    categoryTabs.forEach(tab => {
        tab.addEventListener('click', function() {
            const category = this.dataset.category;
            filterByCategory(category);
            
            // 활성 탭 표시
            categoryTabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

// 카테고리별 필터링
async function filterByCategory(category) {
    try {
        showSkeleton('#posts-container', 3);
        
        const response = await apiCall(`/community/posts?category=${category}`);
        
        if (response && response.length > 0) {
            renderPosts(response);
        } else {
            showEmptyState('#posts-container', `${category} 카테고리의 게시글이 없습니다.`, 'folder');
        }
    } catch (error) {
        console.error('카테고리 필터링 실패:', error);
        showErrorState('#posts-container', '필터링에 실패했습니다.', () => filterByCategory(category));
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