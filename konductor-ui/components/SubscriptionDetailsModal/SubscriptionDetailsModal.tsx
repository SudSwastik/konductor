'use client';

import Icon from '@/components/Icon/Icon';
import { subscriptionStatus } from '@/lib/subscriptionStatus';
import { Subscription } from '@/types';
import styles from './SubscriptionDetailsModal.module.scss';

interface Props {
  subscription: Subscription;
  onClose: () => void;
  onEdit: (sub: Subscription) => void;
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  onDeactivate: (id: string) => void;
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Active',
  PENDING: 'Pending',
  DISABLED: 'Disabled',
};

export default function SubscriptionDetailsModal({
  subscription: sub,
  onClose,
  onEdit,
  onApprove,
  onReject,
  onDeactivate,
}: Props) {
  const status = subscriptionStatus(sub);

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
        <div className={styles.header}>
          <div>
            <p className={styles.eyebrow}>Subscription details</p>
            <h2 className={styles.title}>{sub.subscriberName}</h2>
          </div>
          <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
            <Icon name="close" size={20} />
          </button>
        </div>

        <div className={styles.body}>
          <div className={styles.row}>
            <span className={styles.rowLabel}>Subscriber ID</span>
            <span className={styles.mono}>{sub.subscriberId}</span>
          </div>
          <div className={styles.row}>
            <span className={styles.rowLabel}>Type</span>
            <span className={styles.value}>Event</span>
          </div>
          <div className={styles.row}>
            <span className={styles.rowLabel}>Status</span>
            <span className={`${styles.value} ${styles[status.toLowerCase()]}`}>
              {STATUS_LABEL[status]}
            </span>
          </div>
          <div className={styles.row}>
            <span className={styles.rowLabel}>Output topic</span>
            <span className={styles.mono}>{sub.outputTopic}</span>
          </div>
          <div className={styles.row}>
            <span className={styles.rowLabel}>Version</span>
            <span className={styles.value}>v{sub.subscriptionVersion}</span>
          </div>

          <div>
            <p className={styles.rowLabel}>Triggering event</p>
            <div className={styles.chipRow}>
              <span className={styles.eventChip}>{sub.eventType}</span>
            </div>
          </div>

          <div>
            <p className={styles.rowLabel}>Data fields ({sub.fieldPaths.length})</p>
            <div className={styles.chipRow}>
              {sub.fieldPaths.map((p) => (
                <span key={p} className={styles.fileChip}>
                  <Icon name="description" size={14} />
                  {p}
                </span>
              ))}
              {sub.fieldPaths.length === 0 && (
                <span className={styles.noFiles}>No field paths selected yet</span>
              )}
            </div>
          </div>
        </div>

        <div className={styles.footer}>
          <button
            className={styles.editBtn}
            onClick={() => { onEdit(sub); onClose(); }}
          >
            <Icon name="edit" size={16} />
            Edit subscription
          </button>
          {status === 'PENDING' && (
            <>
              <button
                className={styles.rejectBtn}
                onClick={() => { onReject(sub.id); onClose(); }}
              >
                Reject
              </button>
              <button
                className={styles.approveBtn}
                onClick={() => { onApprove(sub.id); onClose(); }}
              >
                Approve
              </button>
            </>
          )}
          {status === 'ACTIVE' && (
            <button
              className={styles.deactivateBtn}
              onClick={() => { onDeactivate(sub.id); onClose(); }}
            >
              Deactivate
            </button>
          )}
          {status === 'DISABLED' && (
            <button
              className={styles.approveBtn}
              onClick={() => { onApprove(sub.id); onClose(); }}
            >
              Reactivate
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
