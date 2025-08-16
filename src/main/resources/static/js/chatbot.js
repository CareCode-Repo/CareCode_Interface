// 챗봇 페이지 JavaScript

class ChatbotManager {
    constructor() {
        this.messages = [];
        this.isTyping = false;
        this.sessionId = this.generateSessionId();
        
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadChatHistory();
        this.scrollToBottom();
    }

    generateSessionId() {
        return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    bindEvents() {
        // 메시지 전송
        const messageInput = document.getElementById('messageInput');
        const sendBtn = document.getElementById('sendBtn');

        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        sendBtn.addEventListener('click', () => {
            this.sendMessage();
        });

        // 음성 입력
        document.getElementById('voiceBtn').addEventListener('click', () => {
            this.startVoiceInput();
        });

        // 빠른 질문 버튼들
        document.querySelectorAll('.quick-question-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const question = e.target.dataset.question;
                this.sendQuickQuestion(question);
            });
        });

        // 대화 히스토리 삭제
        document.getElementById('clearHistoryBtn').addEventListener('click', () => {
            this.clearChatHistory();
        });
    }

    async sendMessage() {
        const messageInput = document.getElementById('messageInput');
        const message = messageInput.value.trim();
        
        if (!message) return;

        // 사용자 메시지 추가
        this.addUserMessage(message);
        messageInput.value = '';

        // 타이핑 표시
        this.showTypingIndicator();

        try {
            const response = await apiCall('/chatbot/chat', {
                method: 'POST',
                body: JSON.stringify({
                    userId: '1', // 임시 사용자 ID
                    message: message,
                    sessionId: this.sessionId
                })
            });

            // 타이핑 표시 제거
            this.hideTypingIndicator();

            // 챗봇 응답 추가
            const botResponse = response.response || response.message || '응답을 받지 못했습니다.';
            this.addBotMessage(botResponse);

            // 대화 히스토리에 저장
            this.saveToHistory(message, botResponse);

            // 의도 분석 결과 표시 (개발용)
            if (response.intentType) {
                console.log('의도 분석:', response.intentType, '신뢰도:', response.confidence);
            }

        } catch (error) {
            console.error('챗봇 응답 실패:', error);
            this.hideTypingIndicator();
            this.addBotMessage('죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        }
    }

    sendQuickQuestion(question) {
        const messageInput = document.getElementById('messageInput');
        messageInput.value = question;
        this.sendMessage();
    }

    addUserMessage(message) {
        const chatMessages = document.getElementById('chatMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'flex items-start space-x-3 justify-end';
        messageDiv.innerHTML = `
            <div class="bg-pink-500 text-white rounded-lg p-3 max-w-xs">
                <p class="text-sm">${message}</p>
            </div>
            <div class="w-8 h-8 bg-gradient-to-br from-pink-400 to-purple-500 rounded-full flex items-center justify-center flex-shrink-0">
                <i data-feather="user" class="w-4 h-4 text-white"></i>
            </div>
        `;
        chatMessages.appendChild(messageDiv);
        this.scrollToBottom();
        feather.replace();
    }

    addBotMessage(message) {
        const chatMessages = document.getElementById('chatMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = 'flex items-start space-x-3';
        messageDiv.innerHTML = `
            <div class="w-8 h-8 bg-gradient-to-br from-purple-400 to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                <i data-feather="message-circle" class="w-4 h-4 text-white"></i>
            </div>
            <div class="bg-gray-100 rounded-lg p-3 max-w-xs">
                <p class="text-sm">${this.formatMessage(message)}</p>
            </div>
        `;
        chatMessages.appendChild(messageDiv);
        this.scrollToBottom();
        feather.replace();
    }

    formatMessage(message) {
        // 링크를 클릭 가능하게 만들기
        const urlRegex = /(https?:\/\/[^\s]+)/g;
        return message.replace(urlRegex, '<a href="$1" target="_blank" class="text-blue-500 hover:underline">$1</a>');
    }

    showTypingIndicator() {
        const chatMessages = document.getElementById('chatMessages');
        const typingDiv = document.createElement('div');
        typingDiv.id = 'typing-indicator';
        typingDiv.className = 'flex items-start space-x-3';
        typingDiv.innerHTML = `
            <div class="w-8 h-8 bg-gradient-to-br from-purple-400 to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                <i data-feather="message-circle" class="w-4 h-4 text-white"></i>
            </div>
            <div class="bg-gray-100 rounded-lg p-3">
                <div class="flex space-x-1">
                    <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                    <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0.1s"></div>
                    <div class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay: 0.2s"></div>
                </div>
            </div>
        `;
        chatMessages.appendChild(typingDiv);
        this.scrollToBottom();
        feather.replace();
    }

    hideTypingIndicator() {
        const typingIndicator = document.getElementById('typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    scrollToBottom() {
        const chatMessages = document.getElementById('chatMessages');
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    startVoiceInput() {
        if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            const recognition = new SpeechRecognition();
            
            recognition.lang = 'ko-KR';
            recognition.continuous = false;
            recognition.interimResults = false;
            
            recognition.onstart = () => {
                document.getElementById('voiceBtn').classList.add('bg-red-500', 'text-white');
                showToast('음성 인식을 시작합니다. 말씀해주세요.');
            };
            
            recognition.onresult = (event) => {
                const transcript = event.results[0][0].transcript;
                document.getElementById('messageInput').value = transcript;
                this.sendMessage();
            };
            
            recognition.onerror = (event) => {
                console.error('음성 인식 오류:', event.error);
                showToast('음성 인식에 실패했습니다.', 'error');
            };
            
            recognition.onend = () => {
                document.getElementById('voiceBtn').classList.remove('bg-red-500', 'text-white');
            };
            
            recognition.start();
        } else {
            showToast('이 브라우저는 음성 인식을 지원하지 않습니다.', 'error');
        }
    }

    saveToHistory(userMessage, botMessage) {
        let history = storage.get('chatHistory', []);
        
        // history가 null이거나 undefined인 경우 빈 배열로 초기화
        if (!history) {
            history = [];
        }
        
        history.push({
            userMessage,
            botMessage,
            timestamp: new Date().toISOString()
        });
        
        // 최근 50개 대화만 저장
        if (history.length > 50) {
            history.splice(0, history.length - 50);
        }
        
        storage.set('chatHistory', history);
        this.renderChatHistory();
    }

    loadChatHistory() {
        this.renderChatHistory();
    }

    renderChatHistory() {
        let history = storage.get('chatHistory', []);
        const historyContainer = document.getElementById('chatHistory');
        
        // history가 null이거나 undefined인 경우 빈 배열로 초기화
        if (!history) {
            history = [];
        }
        
        if (history.length === 0) {
            historyContainer.innerHTML = `
                <div class="text-center py-8 text-gray-500">
                    <i data-feather="message-circle" class="w-12 h-12 mx-auto mb-4 text-gray-300"></i>
                    <p>아직 대화 기록이 없습니다.</p>
                </div>
            `;
            if (typeof feather !== 'undefined') {
                feather.replace();
            }
            return;
        }
        
        // 최근 10개 대화만 표시
        const recentHistory = history.slice(-10);
        
        historyContainer.innerHTML = recentHistory.map(item => `
            <div class="border-b border-gray-200 pb-3">
                <div class="flex items-start space-x-3 mb-2">
                    <div class="w-6 h-6 bg-gradient-to-br from-pink-400 to-purple-500 rounded-full flex items-center justify-center flex-shrink-0">
                        <i data-feather="user" class="w-3 h-3 text-white"></i>
                    </div>
                    <div class="flex-1">
                        <p class="text-sm text-gray-700">${truncateText(item.userMessage, 100)}</p>
                        <p class="text-xs text-gray-500 mt-1">${formatDate(item.timestamp)}</p>
                    </div>
                </div>
                <div class="flex items-start space-x-3 ml-9">
                    <div class="w-6 h-6 bg-gradient-to-br from-purple-400 to-pink-500 rounded-full flex items-center justify-center flex-shrink-0">
                        <i data-feather="message-circle" class="w-3 h-3 text-white"></i>
                    </div>
                    <div class="flex-1">
                        <p class="text-sm text-gray-700">${truncateText(item.botMessage, 100)}</p>
                    </div>
                </div>
            </div>
        `).join('');
        
        if (typeof feather !== 'undefined') {
            feather.replace();
        }
    }

    clearChatHistory() {
        if (confirm('정말로 모든 대화 기록을 삭제하시겠습니까?')) {
            storage.remove('chatHistory');
            this.renderChatHistory();
            showToast('대화 기록이 삭제되었습니다.');
        }
    }


}

// 유틸리티 함수들
function formatDate(dateString) {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) {
        return '어제';
    } else if (diffDays === 0) {
        const diffHours = Math.floor(diffTime / (1000 * 60 * 60));
        if (diffHours === 0) {
            const diffMinutes = Math.floor(diffTime / (1000 * 60));
            return `${diffMinutes}분 전`;
        }
        return `${diffHours}시간 전`;
    } else if (diffDays < 7) {
        return `${diffDays}일 전`;
    } else {
        return date.toLocaleDateString('ko-KR');
    }
}

function truncateText(text, maxLength) {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
}

// 페이지 로드 시 초기화
let chatbotManager;
document.addEventListener('DOMContentLoaded', function() {
    chatbotManager = new ChatbotManager();
}); 