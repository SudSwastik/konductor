'use client';

import Icon from '@/components/Icon/Icon';
import { subscriptionStatus } from '@/lib/subscriptionStatus';
import { Subscription } from '@/types';
import styles from './SubscriptionTable.module.scss';

interface Props {
  subscriptions: Subscription[];
  onView: (sub: Subscription) => void;
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  onDeactivate: (id: string) => void;
  loading?: boolean;
}

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Active',
  PENDING: 'Pending',
  DISABLED: 'Disabled',
};

export default function SubscriptionTable({
  subscriptions,
  onView,
  onApprove,
  onReject,
  onDeactivate,
  loading,
}: Props) {
  if (!loading && subscriptions.length === 0) {
    return (
      <div className={styles.empty}>
        <Icon name="inbox" size={40} />
        <p className={styles.emptyTitle}>No subscriptions yet</p>
        <p className={styles.emptyHint}>Add your first subscription to get started.</p>
      </div>
    );
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.headerRow}>
        <div className={styles.cell}>Subscription Name</div>
        <div className={styles.cell}>Type</div>
        <div className={styles.cell}>Parameters</div>
        <div className={styles.cell}>Triggers</div>
        <div className={styles.cell}>Events</div>
        <div className={styles.cell}>Status</div>
        <div className={styles.cell}>Actions</div>
      </div>

      {subscriptions.map((sub) => {
        const status = subscriptionStatus(sub);
        return (
          <div className={styles.row} key={sub.id}>
            <div className={`${styles.cell} ${styles.nameCell}`}>
              <span className={styles.name}>{sub.subscriberName}</span>
              <span className={styles.subId}>{sub.subscriberId}</span>
            </div>

            <div className={styles.cell}>
              <span className={styles.typePill}>
                <Icon name="bolt" size={12} />
                Event
              </span>
            </div>

            <div className={styles.cell}>
              <button className={styles.fieldsBtn} title="View field paths" onClick={() => onView(sub)}>
                {sub.fieldPaths.length}
              </button>
            </div>

            <div className={`${styles.cell} ${styles.triggersCell}`}>
              <span className={styles.chip}>{sub.eventType}</span>
              <span className={`${styles.chip} ${styles.chipMono}`}>{sub.outputTopic}</span>
            </div>

            <div className={styles.cell}>
              <span className={styles.versionPill}>v{sub.subscriptionVersion}</span>
            </div>

            <div className={styles.cell}>
              <span className={`${styles.status} ${styles[status.toLowerCase()]}`}>
                {STATUS_LABEL[status]}
              </span>
            </div>

            <div className={`${styles.cell} ${styles.actionsCell}`}>
              <button className={styles.iconBtn} title="View details" onClick={() => onView(sub)}>
                <Icon name="visibility" size={17} />
              </button>
              {status === 'PENDING' && (
                <>
                  <button className={styles.iconBtn} title="Approve" onClick={() => onApprove(sub.id)}>
                    <Icon name="check" size={17} />
                  </button>
                  <button className={styles.iconBtn} title="Reject" onClick={() => onReject(sub.id)}>
                    <Icon name="close" size={17} />
                  </button>
                </>
              )}
              {status === 'ACTIVE' && (
                <button className={styles.iconBtn} title="Deactivate" onClick={() => onDeactivate(sub.id)}>
                  <Icon name="power_settings_new" size={17} />
                </button>
              )}
              {status === 'DISABLED' && (
                <button className={styles.iconBtn} title="Reactivate" onClick={() => onApprove(sub.id)}>
                  <Icon name="check" size={17} />
                </button>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
