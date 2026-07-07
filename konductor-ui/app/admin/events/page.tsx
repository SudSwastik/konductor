'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import EventsTable from '@/components/EventsTable/EventsTable';
import Icon from '@/components/Icon/Icon';
import { api } from '@/lib/api';
import { DeadLetterEvent } from '@/types';
import styles from './page.module.scss';

export default function EventsPage() {
  const [events, setEvents] = useState<DeadLetterEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setEvents(await api.dlq.list());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reach backend');
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  return (
    <>
      <Link href="/admin/subscriptions" className={styles.backLink}>
        <Icon name="arrow_back" size={19} />
        Back to subscriptions
      </Link>

      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Event tracking</h1>
          <p className={styles.subtitle}>Live feed of events generated across your workspace.</p>
        </div>
        <div className={styles.live}>
          <span className={styles.liveDot} />
          Live
        </div>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      {loading ? (
        <p className={styles.loading}>Loading…</p>
      ) : (
        <EventsTable events={events} />
      )}
    </>
  );
}
