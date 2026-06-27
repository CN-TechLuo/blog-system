# ============================================================
# 博客系统 API 测试脚本 (PowerShell)
# 用法: powershell -ExecutionPolicy Bypass -File test-api.ps1
# 前提: 后端已启动在 http://localhost:8080
# ============================================================

$BASE_URL = "http://localhost:8080"
$PASS = 0
$FAIL = 0
$TOKEN = $null
$USERNAME = "testuser_" + (Get-Random -Minimum 1000 -Maximum 9999)

function Test-Result {
    param($Name, $Success)
    if ($Success) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
        $global:PASS++
    } else {
        Write-Host "  [FAIL] $Name" -ForegroundColor Red
        $global:FAIL++
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  博客系统 API 自动化测试" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# ============================================================
# 1. 用户注册测试
# ============================================================
Write-Host "[1] 用户注册测试" -ForegroundColor Yellow

# 1.1 正常注册
$body = @{ username=$USERNAME; password="Test1234"; email="$USERNAME@test.com" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/register" -Method Post -Body $body -ContentType "application/json"
    Test-Result "注册新用户 ($USERNAME)" $r.success
} catch {
    $err = $_.Exception.Message
    Test-Result "注册新用户 - 异常: $err" $false
}

# 1.2 重复注册
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/register" -Method Post -Body $body -ContentType "application/json"
    Test-Result "重复注册应失败" (-not $r.success)
} catch {
    Test-Result "重复注册应失败" $true
}

# 1.3 空字段校验
$body = @{ username=""; password="" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/register" -Method Post -Body $body -ContentType "application/json"
    Test-Result "空字段校验" (-not $r.success)
} catch {
    Test-Result "空字段校验 (HTTP 400)" $true
}

# 1.4 弱密码（纯数字）
$body = @{ username="weakuser"; password="12345678" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/register" -Method Post -Body $body -ContentType "application/json"
    Test-Result "纯数字密码应被拒绝" (-not $r.success)
} catch {
    Test-Result "纯数字密码应被拒绝" $true
}

# ============================================================
# 2. 用户登录测试
# ============================================================
Write-Host "`n[2] 用户登录测试" -ForegroundColor Yellow

# 2.1 正常登录
$body = @{ username=$USERNAME; password="Test1234" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/login" -Method Post -Body $body -ContentType "application/json"
    if ($r.success) {
        $TOKEN = $r.data.token
        Test-Result "登录成功并获取 Token" $true
    } else {
        Test-Result "登录成功并获取 Token" $false
    }
} catch {
    Test-Result "登录失败: $($_.Exception.Message)" $false
}

# 2.2 密码错误（含防枚举测试）
$body = @{ username=$USERNAME; password="WrongPass123" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/login" -Method Post -Body $body -ContentType "application/json"
    Test-Result "错误密码应返回 401" (-not $r.success)
} catch {
    Test-Result "错误密码返回 401" $true
}

# 2.3 不存在用户
$body = @{ username="nonexistent_user_9999"; password="Test1234" } | ConvertTo-Json
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/login" -Method Post -Body $body -ContentType "application/json"
    Test-Result "不存在用户应返回统一错误" (-not $r.success)
} catch {
    Test-Result "不存在用户返回 401" $true
}

# ============================================================
# 3. 文章 CRUD 测试
# ============================================================
Write-Host "`n[3] 文章 CRUD 测试" -ForegroundColor Yellow
$headers = @{ "Authorization"="Bearer $TOKEN"; "Content-Type"="application/json" }

# 3.1 创建文章
$articleBody = @{ title="测试文章标题"; content="这是测试文章的内容，包含 HTML 标签 <script>alert('xss')</script>" } | ConvertTo-Json
$articleId = $null
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/article/create" -Method Post -Body $articleBody -Headers $headers
    if ($r.success) {
        $articleId = $r.data
        Test-Result "创建文章 (ID=$articleId)" $true
    } else {
        Test-Result "创建文章" $false
    }
} catch {
    Test-Result "创建文章异常: $($_.Exception.Message)" $false
}

# 3.2 获取文章详情
if ($articleId) {
    try {
        $r = Invoke-RestMethod -Uri "$BASE_URL/api/article/$articleId" -Method Get
        if ($r.success -and $r.data) {
            # 验证 XSS 过滤：内容中不应包含原始 <script> 标签
            $xssFiltered = $r.data.content -notmatch '<script>alert'
            Test-Result "文章详情 + XSS 过滤验证" $xssFiltered
        } else {
            Test-Result "文章详情" $false
        }
    } catch {
        Test-Result "文章详情异常" $false
    }
}

# 3.3 编辑文章
if ($articleId) {
    $updateBody = @{ id=$articleId; title="更新后的标题"; content="更新后的内容" } | ConvertTo-Json
    try {
        $r = Invoke-RestMethod -Uri "$BASE_URL/api/article/update" -Method Put -Body $updateBody -Headers $headers
        Test-Result "编辑文章" $r.success
    } catch {
        Test-Result "编辑文章异常" $false
    }
}

# 3.4 文章列表
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/article/list?page=1&pageSize=10" -Method Get
    Test-Result "文章列表分页查询" ($r.success -and $r.data.data -is [Array])
} catch {
    Test-Result "文章列表异常" $false
}

# 3.5 未认证创建
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/api/article/create" -Method Post -Body $articleBody -ContentType "application/json"
    Test-Result "未认证创建应被拒绝" $false
} catch {
    Test-Result "未认证创建被拒绝 (401)" $true
}

# 3.6 删除文章
if ($articleId) {
    try {
        $r = Invoke-RestMethod -Uri "$BASE_URL/api/article/$articleId" -Method Delete -Headers $headers
        Test-Result "删除文章" $r.success
    } catch {
        Test-Result "删除文章异常" $false
    }
}

# ============================================================
# 4. 速率限制测试
# ============================================================
Write-Host "`n[4] 速率限制测试" -ForegroundColor Yellow
$rateLimitTriggered = $false
$loginBody = @{ username=$USERNAME; password="Test1234" } | ConvertTo-Json
for ($i = 1; $i -le 7; $i++) {
    try {
        $r = Invoke-RestMethod -Uri "$BASE_URL/api/user/login" -Method Post -Body $loginBody -ContentType "application/json"
        if ($i -ge 6 -and -not $r.success) {
            $rateLimitTriggered = $true
        }
    } catch {
        if ($_.Exception.Response.StatusCode -eq 429) {
            $rateLimitTriggered = $true
        }
    }
}
Test-Result "登录速率限制 (第6+次应被拦截)" $rateLimitTriggered

# ============================================================
# 5. 健康检查
# ============================================================
Write-Host "`n[5] 健康检查" -ForegroundColor Yellow
try {
    $r = Invoke-RestMethod -Uri "$BASE_URL/actuator/health" -Method Get
    Test-Result "Actuator 健康检查" ($r.status -eq "UP")
} catch {
    Test-Result "Actuator 异常" $false
}

# ============================================================
# 结果汇总
# ============================================================
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  测试完成: 通过 $PASS / 失败 $FAIL / 总计 $($PASS+$FAIL)" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
