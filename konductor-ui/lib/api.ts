import {
  DeadLetterEvent,
  DlqStatus,
  RegisterSubscriptionRequest,
  Subscription,
  SubscriptionStatus,
} from '@/types';

const BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status} ${text}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

// Subscriptions
export const api = {
  subscriptions: {
    list: (status: SubscriptionStatus = 'ALL') =>
      request<Subscription[]>(`/api/v1/admin/subscriptions?status=${status}`),

    register: (body: RegisterSubscriptionRequest) =>
      request<Subscription>('/api/v1/admin/subscriptions', {
        method: 'POST',
        body: JSON.stringify(body),
      }),

    approve: (id: string) =>
      request<Subscription>(`/api/v1/admin/subscriptions/${id}/approve`, { method: 'PUT' }),

    reject: (id: string) =>
      request<Subscription>(`/api/v1/admin/subscriptions/${id}/reject`, { method: 'PUT' }),

    deactivate: (id: string) =>
      request<void>(`/api/v1/admin/subscriptions/${id}`, { method: 'DELETE' }),
  },

  dlq: {
    list: (params?: { status?: DlqStatus; eventType?: string }) => {
      const qs = new URLSearchParams();
      if (params?.status) qs.set('status', params.status);
      if (params?.eventType) qs.set('eventType', params.eventType);
      const query = qs.toString() ? `?${qs}` : '';
      return request<DeadLetterEvent[]>(`/api/v1/admin/dlq${query}`);
    },

    retry: (id: string) =>
      request<void>(`/api/v1/admin/dlq/${id}/retry`, { method: 'POST' }),

    discard: (id: string) =>
      request<void>(`/api/v1/admin/dlq/${id}/discard`, { method: 'POST' }),
  },
};
