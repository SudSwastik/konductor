'use client';

import { useCallback, useEffect, useState } from 'react';
import DlqTable from '@/components/DlqTable/DlqTable';
import { api } from '@/lib/api';
import { DeadLetterEvent, DlqStatus } from '@/types';
import styles from './page.module.scss';

export default function DlqPage() {
  const [events, setEvents] = useState<DeadLetterEvent[]>([]);
  const [status, setStatus] = useState<DlqStatus | ''>('');
  const [eventType, setEventType] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setEvents(
        await api.dlq.list({
          status: status || undefined,
          eventType: eventType.trim() || undefined,
        }),
      );
    } finally {
      setLoading(false);
    }
  }, [status, eventType]);

  useEffect(() => { load(); }, [load]);

  const handleRetry = async (id: string) => {
    await api.dlq.retry(id);
    load();
  };

  const handleDiscard = async (id: string) => {
    await api.dlq.discard(id);
    load();
  };

  return (
    <>
      <div className={styles.header}>
        <h1 className={styles.title}>Dead Letter Queue</h1>
      </div>

      <div className={styles.filters}>
        <div className={styles.filterGroup}>
          <label>Status</label>
          <select value={status} onChange={(e) => setStatus(e.target.value as DlqStatus | '')}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="RETRIED">Retried</option>
            <option value="DISCARDED">Discarded</option>
          </select>
        </div>
        <div className={styles.filterGroup}>
          <label>Event Type</label>
          <input
            value={eventType}
            onChange={(e) => setEventType(e.target.value)}
            placeholder="e.g. order.created"
          />
        </div>
        <button className={styles.refreshBtn} onClick={load}>Refresh</button>
      </div>

      {loading ? (
        <p style={{ color: '#8b90a8' }}>Loading…</p>
      ) : (
        <DlqTable events={events} onRetry={handleRetry} onDiscard={handleDiscard} />
      )}
    </>
  );
}
