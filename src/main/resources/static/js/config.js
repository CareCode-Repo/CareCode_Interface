// 환경별 설정
const CONFIG = {
    // 현재 호스트 자동 감지
    API_BASE_URL: window.location.origin,
    
    // 개발/운영 환경 감지
    IS_PRODUCTION: window.location.hostname !== 'localhost',
    
    // API 경로
    API_ENDPOINTS: {
        LOGIN: '/auth/login',
        LOGOUT: '/auth/logout',
        USER_INFO: '/auth/user',
        FACILITIES: '/facilities',
        COMMUNITY: '/community/posts',
        HEALTH: '/health/records',
        POLICIES: '/policies',
        CHATBOT: '/chatbot/chat'
    },
    
    // 기타 설정
    DEFAULT_PAGE_SIZE: 10,
    DEBOUNCE_DELAY: 300
};

// 전역으로 사용할 수 있도록 window 객체에 추가
window.APP_CONFIG = CONFIG;

// 디버깅용 로그
console.log('App Config loaded:', CONFIG);