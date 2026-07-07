'use client';

import Badge from '@/components/Badge/Badge';
import Icon from '@/components/Icon/Icon';
import { DeadLetterEvent } from '@/types';
import styles from './EventsTable.module.scss';

interface Props {
  events: DeadLetterEvent[];
}

function fmtTime(iso: string) {
  return new Date(iso).toLocaleTimeString();
}

export default function EventsTable({ events }: Props) {
  if (events.length === 0) {
    return <p className={styles.empty}>No events found.</p>;
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.headerRow}>
        <div>Event</div>
        <div>Subscription</div>
        <div>Time</div>
        <div className={styles.deliveryHead}>Delivery</div>
      </div>
      {events.map((ev) => (
        <div className={styles.row} key={ev.id}>
          <div className={styles.event}>
            <Icon name="bolt" size={16} />
            {ev.eventType}
          </div>
          <div className={styles.subscriber}>{ev.subscriberId ?? '—'}</div>
          <div className={styles.time}>{fmtTime(ev.createdAt)}</div>
          <div className={styles.delivery}>
            <Badge label={ev.status} />
          </div>
        </div>
      ))}
    </div>
  );
}
