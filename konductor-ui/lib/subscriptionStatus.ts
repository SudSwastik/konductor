import { Subscription } from '@/types';

export type SubscriptionUiStatus = 'ACTIVE' | 'PENDING' | 'DISABLED';

// The backend has no status enum — active + approvedAt already distinguish
// a subscription that's never been approved from one that was approved and
// later deactivated, so we derive the display status instead of storing it.
export function subscriptionStatus(sub: Subscription): SubscriptionUiStatus {
  if (sub.active) return 'ACTIVE';
  if (sub.approvedAt) return 'DISABLED';
  return 'PENDING';
}
