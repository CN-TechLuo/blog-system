// ============================================================
// k6 压测脚本：上线前基础性能验证
// 安装: https://k6.io/docs/get-started/installation/
// 运行: k6 run --vus 50 --duration 60s ops/k6/load-test.js
// 通过标准（单机 2C4G 基线，仅供参考）:
//   - p95 延迟 < 500ms
//   - 错误率 < 1%（429 限流属预期，不计入失败）
// ============================================================
import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
}

export default function () {
  // 公开接口：文章列表（访客主路径）
  const list = http.get(`${BASE}/api/article/list?page=1&pageSize=10`)
  check(list, { 'list 200': r => r.status === 200 })

  const hot = http.get(`${BASE}/api/article/feed/hot?page=1&pageSize=10`)
  check(hot, { 'hot 200': r => r.status === 200 })

  // 健康检查
  const health = http.get(`${BASE}/actuator/health`)
  check(health, { 'health 200': r => r.status === 200 })

  // 验证码获取（公开）
  const captcha = http.get(`${BASE}/api/captcha`)
  check(captcha, { 'captcha 200': r => r.status === 200 })

  // 登录撞库模拟（每 VU 少量次，观察限流/锁定是否生效）
  const login = http.post(`${BASE}/api/user/login`, JSON.stringify({
    username: `loadtest_${Math.floor(Math.random() * 100)}`,
    password: 'WrongPass@123',
    captchaId: 'x',
    captchaAnswer: 'x',
  }), { headers: { 'Content-Type': 'application/json' } })
  check(login, {
    'login 4xx/429 正常': r => r.status === 400 || r.status === 401 || r.status === 429,
  })

  sleep(1)
}
