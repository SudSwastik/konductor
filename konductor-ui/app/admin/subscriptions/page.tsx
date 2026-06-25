'use client';

import { useCallback, useEffect, useState } from 'react';
import SubscriptionTable from '@/components/SubscriptionTable/SubscriptionTable';
import { api } from '@/lib/api';
import { RegisterSubscriptionRequest, Subscription, SubscriptionStatus } from '@/types';
import styles from './page.module.scss';

const TABS: { label: string; value: SubscriptionStatus }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Approved', value: 'APPROVED' },
];

const EMPTY_FORM: RegisterSubscriptionRequest = {
  subscriberId: '',
  subscriberName: '',
  eventType: '',
  fieldPaths: [],
  outputTopic: '',
};

export default function SubscriptionsPage() {
  const [tab, setTab] = useState<SubscriptionStatus>('ALL');
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<RegisterSubscriptionRequest>(EMPTY_FORM);
  const [fieldPathsRaw, setFieldPathsRaw] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSubscriptions(await api.subscriptions.list(tab));
    } finally {
      setLoading(false);
    }
  }, [tab]);

  useEffect(() => { load(); }, [load]);

  const handleApprove = async (id: string) => {
    await api.subscriptions.approve(id);
    load();
  };

  const handleReject = async (id: string) => {
    await api.subscriptions.reject(id);
    load();
  };

  const handleDeactivate = async (id: string) => {
    await api.subscriptions.deactivate(id);
    load();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const paths = fieldPathsRaw.split('\n').map((s) => s.trim()).filter(Boolean);
      await api.subscriptions.register({ ...form, fieldPaths: paths });
      setShowForm(false);
      setForm(EMPTY_FORM);
      setFieldPathsRaw('');
      load();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    }
  };

  return (
    <>
      <div className={styles.header}>
        <h1 className={styles.title}>Subscriptions</h1>
        <button className={styles.newBtn} onClick={() => setShowForm(true)}>
          + New Subscription
        </button>
      </div>

      <div className={styles.tabs}>
        {TABS.map(({ label, value }) => (
          <button
            key={value}
            className={`${styles.tab} ${tab === value ? styles.activeTab : ''}`}
            onClick={() => setTab(value)}
          >
            {label}
          </button>
        ))}
      </div>

      <SubscriptionTable
        subscriptions={subscriptions}
        loading={loading}
        onApprove={handleApprove}
        onReject={handleReject}
        onDeactivate={handleDeactivate}
      />

      {showForm && (
        <div className={styles.overlay} onClick={() => setShowForm(false)}>
          <div className={styles.modal} onClick={(e) => e.stopPropagation()}>
            <h2 className={styles.modalTitle}>Register Subscription</h2>
            {error && <p className={styles.error}>{error}</p>}
            <form onSubmit={handleSubmit}>
              <div className={styles.field}>
                <label>Subscriber ID</label>
                <input required value={form.subscriberId}
                  onChange={(e) => setForm({ ...form, subscriberId: e.target.value })} />
              </div>
              <div className={styles.field}>
                <label>Subscriber Name</label>
                <input required value={form.subscriberName}
                  onChange={(e) => setForm({ ...form, subscriberName: e.target.value })} />
              </div>
              <div className={styles.field}>
                <label>Event Type</label>
                <input required value={form.eventType}
                  onChange={(e) => setForm({ ...form, eventType: e.target.value })}
                  placeholder="e.g. order.created" />
              </div>
              <div className={styles.field}>
                <label>Field Paths</label>
                <textarea required value={fieldPathsRaw}
                  onChange={(e) => setFieldPathsRaw(e.target.value)}
                  placeholder="$.orderId&#10;$.amount" />
                <p className={styles.hint}>One JSONPath per line</p>
              </div>
              <div className={styles.field}>
                <label>Output Topic</label>
                <input required value={form.outputTopic}
                  onChange={(e) => setForm({ ...form, outputTopic: e.target.value })}
                  placeholder="e.g. my-service-events" />
              </div>
              <div className={styles.modalActions}>
                <button type="button" className={styles.cancelBtn} onClick={() => setShowForm(false)}>
                  Cancel
                </button>
                <button type="submit" className={styles.submitBtn}>Register</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
