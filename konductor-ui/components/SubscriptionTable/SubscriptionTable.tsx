'use client';

import Badge from '@/components/Badge/Badge';
import { Subscription } from '@/types';
import styles from './SubscriptionTable.module.scss';

interface Props {
  subscriptions: Subscription[];
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  onDeactivate: (id: string) => void;
  loading?: boolean;
}

export default function SubscriptionTable({
  subscriptions,
  onApprove,
  onReject,
  onDeactivate,
  loading,
}: Props) {
  if (!loading && subscriptions.length === 0) {
    return <p className={styles.empty}>No subscriptions found.</p>;
  }

  return (
    <div className={styles.wrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Subscriber</th>
            <th>Event Type</th>
            <th>Field Paths</th>
            <th>Output Topic</th>
            <th>Version</th>
            <th>Status</th>
            <th>Created</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {subscriptions.map((sub) => (
            <tr key={sub.id}>
              <td>
                <div>{sub.subscriberId}</div>
                <div style={{ fontSize: 12, opacity: 0.6 }}>{sub.subscriberName}</div>
              </td>
              <td>{sub.eventType}</td>
              <td>
                <div className={styles.fieldPaths}>
                  {sub.fieldPaths.map((p) => (
                    <code key={p} className={styles.path}>{p}</code>
                  ))}
                </div>
              </td>
              <td>{sub.outputTopic}</td>
              <td>v{sub.subscriptionVersion}</td>
              <td>
                <Badge label={sub.active ? 'APPROVED' : 'PENDING'} />
              </td>
              <td>{new Date(sub.createdAt).toLocaleDateString()}</td>
              <td>
                <div className={styles.actions}>
                  {!sub.active && (
                    <button className={`${styles.btn} ${styles.approve}`} onClick={() => onApprove(sub.id)}>
                      Approve
                    </button>
                  )}
                  {!sub.active && (
                    <button className={`${styles.btn} ${styles.reject}`} onClick={() => onReject(sub.id)}>
                      Reject
                    </button>
                  )}
                  {sub.active && (
                    <button className={`${styles.btn} ${styles.deactivate}`} onClick={() => onDeactivate(sub.id)}>
                      Deactivate
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
