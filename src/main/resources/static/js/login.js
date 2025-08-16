// 로그인 페이지 JavaScript
document.addEventListener('DOMContentLoaded', function() {
    console.log('로그인 페이지 로드됨');
    
    // 이미 로그인된 경우 메인 페이지로 리다이렉트
    if (getToken()) {
        window.location.href = '/user';
        return;
    }
    
    // 이벤트 리스너 설정
    setupEventListeners();
});

// 이벤트 리스너 설정
function setupEventListeners() {
    // 로그인 폼 제출
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }
    
    // 비밀번호 표시/숨기기 토글
    const passwordToggle = document.getElementById('password-toggle');
    const passwordInput = document.getElementById('password');
    
    if (passwordToggle && passwordInput) {
        passwordToggle.addEventListener('click', function() {
            const type = passwordInput.type === 'password' ? 'text' : 'password';
            passwordInput.type = type;
            
            const icon = this.querySelector('i[data-feather]');
            if (icon) {
                icon.setAttribute('data-feather', type === 'password' ? 'eye' : 'eye-off');
                feather.replace();
            }
        });
    }
    
    // 소셜 로그인 버튼들
    const socialButtons = document.querySelectorAll('.social-login-btn');
    socialButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();
            const provider = this.dataset.provider;
            handleSocialLogin(provider);
        });
    });
    
    // 회원가입 링크
    const registerLink = document.getElementById('register-link');
    if (registerLink) {
        registerLink.addEventListener('click', function(e) {
            e.preventDefault();
            window.location.href = '/user/register';
        });
    }
    
    // 비밀번호 찾기 링크
    const forgotPasswordLink = document.getElementById('forgot-password-link');
    if (forgotPasswordLink) {
        forgotPasswordLink.addEventListener('click', function(e) {
            e.preventDefault();
            showForgotPasswordModal();
        });
    }
}

// 로그인 처리
async function handleLogin(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const email = formData.get('email');
    const password = formData.get('password');
    const rememberMe = formData.get('remember-me') === 'on';
    
    // 폼 검증
    if (!validateLoginForm(email, password)) {
        return;
    }
    
    try {
        // 로딩 상태 표시
        const loginButton = document.getElementById('login-button');
        const originalText = loginButton.textContent;
        loginButton.disabled = true;
        loginButton.innerHTML = `
            <div class="flex items-center space-x-2">
                <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white"></div>
                <span>로그인 중...</span>
            </div>
        `;
        
        // 로그인 API 호출
        const success = await login(email, password);
        
        if (success) {
            // 로그인 성공 시 메인 페이지로 리다이렉트
            window.location.href = '/user';
        }
    } catch (error) {
        console.error('로그인 처리 오류:', error);
        showToast('로그인 중 오류가 발생했습니다.', 'error');
    } finally {
        // 버튼 상태 복원
        const loginButton = document.getElementById('login-button');
        loginButton.disabled = false;
        loginButton.textContent = '로그인';
    }
}

// 로그인 폼 검증
function validateLoginForm(email, password) {
    let isValid = true;
    
    // 이메일 검증
    const emailInput = document.getElementById('email');
    const emailError = document.getElementById('email-error');
    
    if (!email || !email.trim()) {
        showFieldError(emailInput, emailError, '이메일을 입력해주세요.');
        isValid = false;
    } else if (!isValidEmail(email)) {
        showFieldError(emailInput, emailError, '올바른 이메일 형식을 입력해주세요.');
        isValid = false;
    } else {
        clearFieldError(emailInput, emailError);
    }
    
    // 비밀번호 검증
    const passwordInput = document.getElementById('password');
    const passwordError = document.getElementById('password-error');
    
    if (!password || !password.trim()) {
        showFieldError(passwordInput, passwordError, '비밀번호를 입력해주세요.');
        isValid = false;
    } else if (password.length < 6) {
        showFieldError(passwordInput, passwordError, '비밀번호는 6자 이상이어야 합니다.');
        isValid = false;
    } else {
        clearFieldError(passwordInput, passwordError);
    }
    
    return isValid;
}

// 이메일 형식 검증
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// 필드 에러 표시
function showFieldError(input, errorElement, message) {
    input.classList.add('border-red-500', 'focus:border-red-500');
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.classList.remove('hidden');
    }
}

// 필드 에러 제거
function clearFieldError(input, errorElement) {
    input.classList.remove('border-red-500', 'focus:border-red-500');
    if (errorElement) {
        errorElement.classList.add('hidden');
    }
}

// 소셜 로그인 처리
function handleSocialLogin(provider) {
    // 실제 구현에서는 OAuth2 리다이렉트
    showToast(`${provider} 로그인은 준비 중입니다.`, 'info');
}

// 비밀번호 찾기 모달 표시
function showForgotPasswordModal() {
    const modal = document.getElementById('forgot-password-modal');
    if (modal) {
        modal.classList.remove('hidden');
    }
}

// 비밀번호 찾기 처리
async function handleForgotPassword(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const email = formData.get('email');
    
    if (!email || !isValidEmail(email)) {
        showToast('올바른 이메일을 입력해주세요.', 'error');
        return;
    }
    
    try {
        const response = await fetch('/auth/forgot-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email })
        });
        
        if (response.ok) {
            showToast('비밀번호 재설정 이메일이 발송되었습니다.', 'success');
            closeModal('forgot-password-modal');
        } else {
            const data = await response.json();
            showToast(data.message || '비밀번호 찾기에 실패했습니다.', 'error');
        }
    } catch (error) {
        console.error('비밀번호 찾기 오류:', error);
        showToast('비밀번호 찾기 중 오류가 발생했습니다.', 'error');
    }
}

// 데모 로그인 (개발용)
function demoLogin() {
    const demoEmail = 'demo@example.com';
    const demoPassword = 'password123';
    
    document.getElementById('email').value = demoEmail;
    document.getElementById('password').value = demoPassword;
    
    // 로그인 폼 제출
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.dispatchEvent(new Event('submit'));
    }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 자동 포커스
    const emailInput = document.getElementById('email');
    if (emailInput) {
        emailInput.focus();
    }
    
    // Feather 아이콘 초기화
    feather.replace();
    
    // Enter 키로 로그인
    const passwordInput = document.getElementById('password');
    if (passwordInput) {
        passwordInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                const loginForm = document.getElementById('login-form');
                if (loginForm) {
                    loginForm.dispatchEvent(new Event('submit'));
                }
            }
        });
    }
}); 