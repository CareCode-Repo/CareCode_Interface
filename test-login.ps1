# CareCode 로그인 테스트 스크립트

# 테스트 사용자 정보
$testUsers = @(
    @{
        email = "admin@carecode.com"
        password = "admin123"
        name = "관리자"
    },
    @{
        email = "test1@carecode.com"
        password = "password123"
        name = "테스트 사용자 1"
    },
    @{
        email = "test2@carecode.com"
        password = "password123"
        name = "테스트 사용자 2"
    }
)

# API 기본 URL
$baseUrl = "http://localhost:8080"

Write-Host "=== CareCode 로그인 테스트 ===" -ForegroundColor Green

foreach ($user in $testUsers) {
    Write-Host "`n테스트 사용자: $($user.name) ($($user.email))" -ForegroundColor Yellow
    
    # 로그인 요청 데이터
    $loginData = @{
        email = $user.email
        password = $user.password
    } | ConvertTo-Json
    
    try {
        # 로그인 API 호출
        $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData -ContentType "application/json"
        
        if ($response.accessToken) {
            Write-Host "✅ 로그인 성공!" -ForegroundColor Green
            Write-Host "Access Token: $($response.accessToken.Substring(0, 50))..." -ForegroundColor Cyan
            Write-Host "User ID: $($response.userId)" -ForegroundColor Cyan
            Write-Host "Role: $($response.role)" -ForegroundColor Cyan
        } else {
            Write-Host "❌ 로그인 실패: 응답에 토큰이 없습니다" -ForegroundColor Red
        }
    }
    catch {
        Write-Host "❌ 로그인 실패: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode
            Write-Host "HTTP Status: $statusCode" -ForegroundColor Red
        }
    }
}

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Green 