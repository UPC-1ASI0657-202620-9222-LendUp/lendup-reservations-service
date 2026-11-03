import http from 'k6/http';
import { check } from 'k6';

export const options = {
  scenarios: {
    authenticated_reads: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '60s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<=2000'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';

export default function () {
  if (!__ENV.FIREBASE_ID_TOKEN) throw new Error('FIREBASE_ID_TOKEN is required');
  const response = http.get(`${baseUrl}/api/v1/reservas`, {
    headers: {
      Authorization: `Bearer ${__ENV.FIREBASE_ID_TOKEN}`,
      'X-Correlation-ID': `k6-${__VU}-${__ITER}`,
    },
  });
  check(response, { 'reservas returns 200': (result) => result.status === 200 });
}
