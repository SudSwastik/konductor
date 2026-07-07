'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import Icon from '@/components/Icon/Icon';
import RegisterSubscriptionModal from '@/components/RegisterSubscriptionModal/RegisterSubscriptionModal';
import StatCards from '@/components/StatCards/StatCards';
import SubscriptionDetailsModal from '@/components/SubscriptionDetailsModal/SubscriptionDetailsModal';
import SubscriptionTable from '@/components/SubscriptionTable/SubscriptionTable';
import { api } from '@/lib/api';
import { subscriptionStatus, SubscriptionUiStatus } from '@/lib/subscriptionStatus';
import { RegisterSubscriptionRequest, Subscription } from '@/types';
import styles from './page.module.scss';

type Tab = 'ALL' | SubscriptionUiStatus;

const TABS: { label: string; value: Tab }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Pending', value: 'PENDING' },
  { label: 'Disabled', value: 'DISABLED' },
];

export default function SubscriptionsPage() {
  const [tab, setTab] = useState<Tab>('ALL');
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [showRegister, setShowRegister] = useState(false);
  const [detailsId, setDetailsId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      setSubscriptions(await api.subscriptions.list('ALL'));
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : 'Failed to reach backend');
      setSubscriptions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const filtered = useMemo(
    () => (tab === 'ALL' ? subscriptions : subscriptions.filter((s) => subscriptionStatus(s) === tab)),
    [subscriptions, tab],
  );

  const statCards = useMemo(() => {
    const active = subscriptions.filter((s) => subscriptionStatus(s) === 'ACTIVE').length;
    const pending = subscriptions.filter((s) => subscriptionStatus(s) === 'PENDING').length;
    const disabled = subscriptions.filter((s) => subscriptionStatus(s) === 'DISABLED').length;
    return [
      { label: 'Total subscriptions', value: subscriptions.length },
      { label: 'Active', value: active },
      { label: 'Pending', value: pending },
      { label: 'Disabled', value: disabled },
    ];
  }, [subscriptions]);

  const detailsSub = detailsId ? subscriptions.find((s) => s.id === detailsId) ?? null : null;

  const handleApprove = async (id: string) => {
    try { await api.subscriptions.approve(id); load(); }
    catch (err) { setLoadError(err instanceof Error ? err.message : 'Action failed'); }
  };

  const handleReject = async (id: string) => {
    try { await api.subscriptions.reject(id); load(); }
    catch (err) { setLoadError(err instanceof Error ? err.message : 'Action failed'); }
  };

  const handleDeactivate = async (id: string) => {
    try { await api.subscriptions.deactivate(id); load(); }
    catch (err) { setLoadError(err instanceof Error ? err.message : 'Action failed'); }
  };

  const handleRegister = async (req: RegisterSubscriptionRequest) => {
    await api.subscriptions.register(req);
    setShowRegister(false);
    load();
  };

  return (
    <>
      <div className={styles.breadcrumb}>
        <Icon name="hub" size={16} />
        Konductor
        <span className={styles.sep}>/</span>
        <span className={styles.current}>Subscriptions</span>
      </div>

      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Data subscriptions</h1>
          <p className={styles.subtitle}>Route filtered events to subscriber output topics.</p>
        </div>
        <div className={styles.headerActions}>
          <Link href="/admin/events" className={styles.eventsBtn}>
            <Icon name="sensors" size={18} />
            Event tracking
          </Link>
          <button className={styles.newBtn} onClick={() => setShowRegister(true)}>
            <Icon name="add" size={18} />
            Add new Subscription
          </button>
        </div>
      </div>

      <StatCards cards={statCards} />

      <h2 className={styles.sectionHeading}>Your Subscriptions</h2>

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

      {loadError && <p className={styles.error}>{loadError}</p>}

      <SubscriptionTable
        subscriptions={filtered}
        loading={loading}
        onView={(sub) => setDetailsId(sub.id)}
        onApprove={handleApprove}
        onReject={handleReject}
        onDeactivate={handleDeactivate}
      />

      {showRegister && (
        <RegisterSubscriptionModal
          onClose={() => setShowRegister(false)}
          onSubmit={handleRegister}
        />
      )}

      {detailsSub && (
        <SubscriptionDetailsModal
          subscription={detailsSub}
          onClose={() => setDetailsId(null)}
          onApprove={handleApprove}
          onReject={handleReject}
          onDeactivate={handleDeactivate}
        />
      )}
    </>
  );
}
