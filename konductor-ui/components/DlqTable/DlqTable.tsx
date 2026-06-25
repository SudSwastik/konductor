'use client';

import { useState } from 'react';
import Badge from '@/components/Badge/Badge';
import { DeadLetterEvent } from '@/types';
import styles from './DlqTable.module.scss';

interface Props {
  events: DeadLetterEvent[];
  onRetry: (id: string) => void;
  onDiscard: (id: string) => void;
}

export default function DlqTable({ events, onRetry, onDiscard }: Props) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const toggle = (id: string) =>
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) { next.delete(id); } else { next.add(id); }
      return next;
    });

  const fmt = (json: string) => {
    try { return JSON.stringify(JSON.parse(json), null, 2); }
    catch { return json; }
  };

  if (events.length === 0) {
    return <p className={styles.empty}>No dead-letter events found.</p>;
  }

  return (
    <div className={styles.wrap}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>Event Type</th>
            <th>Subscriber</th>
            <th>Failure Reason</th>
            <th>Retries</th>
            <th>Last Retry</th>
            <th>Status</th>
            <th>Created</th>
            <th>Original Event</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {events.map((ev) => (
            <tr key={ev.id}>
              <td>{ev.eventType}</td>
              <td>{ev.subscriberId ?? '—'}</td>
              <td><span className={styles.reason}>{ev.failureReason}</span></td>
              <td>{ev.retryCount}</td>
              <td>{ev.lastRetryAt ? new Date(ev.lastRetryAt).toLocaleString() : '—'}</td>
              <td><Badge label={ev.status} /></td>
              <td>{new Date(ev.createdAt).toLocaleDateString()}</td>
              <td>
                <button className={styles.expandBtn} onClick={() => toggle(ev.id)}>
                  {expanded.has(ev.id) ? 'Hide' : 'View'}
                </button>
                {expanded.has(ev.id) && (
                  <pre className={styles.jsonBlock}>{fmt(ev.originalEvent)}</pre>
                )}
              </td>
              <td>
                <div className={styles.actions}>
                  <button
                    className={`${styles.btn} ${styles.retry}`}
                    disabled={ev.status === 'DISCARDED'}
                    onClick={() => onRetry(ev.id)}
                  >
                    Retry
                  </button>
                  <button
                    className={`${styles.btn} ${styles.discard}`}
                    disabled={ev.status === 'DISCARDED'}
                    onClick={() => onDiscard(ev.id)}
                  >
                    Discard
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
