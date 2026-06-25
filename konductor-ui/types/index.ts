export interface Subscription {
  id: string;
  subscriberId: string;
  subscriberName: string;
  eventType: string;
  fieldPaths: string[];
  outputTopic: string;
  active: boolean;
  subscriptionVersion: number;
  createdAt: string;
  approvedAt: string | null;
}

export interface RegisterSubscriptionRequest {
  subscriberId: string;
  subscriberName: string;
  eventType: string;
  fieldPaths: string[];
  outputTopic: string;
}

export interface DeadLetterEvent {
  id: string;
  eventId: string;
  eventType: string;
  originalEvent: string;
  subscriberId: string;
  failureReason: string;
  retryCount: number;
  lastRetryAt: string | null;
  status: 'PENDING' | 'RETRIED' | 'DISCARDED';
  createdAt: string;
}

export type SubscriptionStatus = 'PENDING' | 'APPROVED' | 'ALL';
export type DlqStatus = 'PENDING' | 'RETRIED' | 'DISCARDED';
