// k6 load test for the flash-sale CLAIM (Phase 7).
//
//   5,000 different customers press "Claim" at the same moment for a deal with 100 units.
//   Expected: exactly 100 x 201 (won), 4,900 x 409 "Sold out", 0 errors, and p95 under 500 ms.
//
// Each virtual user (VU) is one customer. Logging in 5,000 times would only measure the password hashing, so the
// test makes each customer's login cookie itself (same HS256 token the server makes) - see run-flash-load.ps1.
//
//   k6 run -e BASE=http://localhost:8090 -e ITEM=1 flash-claim.js
//
// SHAPE=burst  (default) all 5,000 press at the same instant - the worst case.
// SHAPE=spread the same 5,000 people arrive over 5 seconds (1,000 per second) - a more normal sale start.
// TARGET=health  the same burst at GET /api/health (no claim, no database): the CEILING of this machine + Tomcat.
//
// A short warm-up (setup) runs first, like a server that has been up for a while: 300 real requests so the JVM has
// compiled the hot code. Warm-up requests are not counted in the results.
import http from 'k6/http'
import { check } from 'k6'
import { Counter } from 'k6/metrics'
import { hmac } from 'k6/crypto'
import encoding from 'k6/encoding'
import { SharedArray } from 'k6/data'
import exec from 'k6/execution'

const BASE = __ENV.BASE || 'http://localhost:8090'
const ITEM = __ENV.ITEM || '1'
const PEOPLE = Number(__ENV.PEOPLE || 5000)
const SHAPE = __ENV.SHAPE || 'burst'
const TARGET = __ENV.TARGET || 'claim'
const OUT = __ENV.OUT || './results/flash-claim-summary.json'
// the same secret the load-test server runs with (the dev fallback in application.yml)
const SECRET = __ENV.JWT_SECRET || 'dev-only-secret-not-for-production-0123456789'

const users = new SharedArray('users', () =>
  open('./results/load-users.csv').trim().split('\n').map((line) => {
    const [id, email] = line.split(',')
    return { id, email: email.trim() }
  }))

const won = new Counter('claims_won')
const soldOut = new Counter('claims_sold_out')
const other = new Counter('claims_other')

const scenarios = {
  burst: { executor: 'per-vu-iterations', vus: PEOPLE, iterations: 1, maxDuration: '3m' },
  spread: { executor: 'constant-arrival-rate', rate: 1000, timeUnit: '1s', duration: '5s', preAllocatedVUs: 1500, maxVUs: PEOPLE },
}

export const options = {
  scenarios: { everyone: scenarios[SHAPE] },
  thresholds: TARGET === 'health' ? {} : {
    http_req_duration: ['p(95)<500'],
    claims_other: ['count==0'],
    claims_won: ['count==100'],
  },
}

// warm-up: a login-cookie request to a deal id that does not exist (404) walks the whole request path
// (security filter, user lookup, claim code) without taking a unit
export function setup() {
  for (let i = 0; i < 300; i++) {
    const user = users[i % users.length]
    http.post(`${BASE}/api/flash-sales/items/999999/claim`, null, {
      headers: { 'X-XSRF-TOKEN': 'load-test', Cookie: `TRIVOKO_TOKEN=${loginCookie(user)}; XSRF-TOKEN=load-test` },
      tags: { warmup: 'yes' },
    })
    http.get(`${BASE}/api/health`, { tags: { warmup: 'yes' } })
  }
}

// 201 (won) and 409 (sold out) are both correct answers
http.setResponseCallback(http.expectedStatuses(200, 201, 404, 409))

const b64url = (obj) => encoding.b64encode(JSON.stringify(obj), 'rawurl')

function loginCookie(user) {
  const now = Math.floor(Date.now() / 1000)
  const head = b64url({ alg: 'HS256' })
  const body = b64url({ iss: 'trivoko', sub: String(user.id), iat: now, exp: now + 3600, email: user.email, roles: ['CUSTOMER'] })
  const signature = hmac('sha256', SECRET, `${head}.${body}`, 'base64rawurl')
  return `${head}.${body}.${signature}`
}

export default function () {
  if (TARGET === 'health') {
    check(http.get(`${BASE}/api/health`), { 'health 200': (r) => r.status === 200 })
    return
  }
  const person = SHAPE === 'burst' ? __VU - 1 : exec.scenario.iterationInTest
  const user = users[person % users.length]
  const res = http.post(`${BASE}/api/flash-sales/items/${ITEM}/claim`, null, {
    headers: { 'X-XSRF-TOKEN': 'load-test', Cookie: `TRIVOKO_TOKEN=${loginCookie(user)}; XSRF-TOKEN=load-test` },
  })
  if (res.status === 201) won.add(1)
  else if (res.status === 409 && String(res.body).includes('Sold out')) soldOut.add(1)
  else other.add(1)
  check(res, { 'answered 201 or 409': (r) => r.status === 201 || r.status === 409 })
}

export function handleSummary(data) {
  return { [OUT]: JSON.stringify(data, null, 2) }
}
