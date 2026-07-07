'use client';

import { FormEvent, MouseEvent, useState } from 'react';
import Icon from '@/components/Icon/Icon';
import { RegisterSubscriptionRequest, Subscription } from '@/types';
import styles from './RegisterSubscriptionModal.module.scss';

interface Props {
  subscription?: Subscription | null;
  onClose: () => void;
  onSubmit: (req: RegisterSubscriptionRequest) => Promise<void>;
}

const METADATA_FIELDS = [
  'eventId',
  'eventType',
  'eventTimestamp',
  'sourceConfigId',
  'configType',
  'schemaVersion',
  'correlationId',
  'traceId',
];

const PAYLOAD_FIELDS = ['order_id', 'amount', 'currency', 'status', 'items', 'customer', 'metadata'];

const METADATA_PREFIX = '$.metadata.';
const PAYLOAD_PREFIX = '$.payload.';

const slugify = (s: string) =>
  s.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');

export default function RegisterSubscriptionModal({ subscription, onClose, onSubmit }: Props) {
  const isEdit = Boolean(subscription);
  const [step, setStep] = useState<1 | 2>(1);
  const [name, setName] = useState(subscription?.subscriberName ?? '');
  const [type, setType] = useState<'API Callback' | 'Event'>('Event');
  const [eventType, setEventType] = useState(subscription?.eventType ?? '');
  const [outputTopic, setOutputTopic] = useState(subscription?.outputTopic ?? '');
  const [selectedMetadata, setSelectedMetadata] = useState<string[]>(() =>
    subscription
      ? subscription.fieldPaths
          .filter((p) => p.startsWith(METADATA_PREFIX))
          .map((p) => p.slice(METADATA_PREFIX.length))
      : [],
  );
  const [selectedPayload, setSelectedPayload] = useState<string[]>(() =>
    subscription
      ? subscription.fieldPaths
          .filter((p) => p.startsWith(PAYLOAD_PREFIX))
          .map((p) => p.slice(PAYLOAD_PREFIX.length))
          .filter((f) => PAYLOAD_FIELDS.includes(f))
      : [],
  );
  const [error, setError] = useState('');

  const stop = (e: MouseEvent) => e.stopPropagation();

  const proceedToFields = () => {
    if (!name.trim()) return;
    setError('');
    setStep(2);
  };

  const toggleMetadata = (field: string) =>
    setSelectedMetadata((prev) =>
      prev.includes(field) ? prev.filter((f) => f !== field) : [...prev, field],
    );

  const togglePayload = (field: string) =>
    setSelectedPayload((prev) =>
      prev.includes(field) ? prev.filter((f) => f !== field) : [...prev, field],
    );

  const fieldCount = selectedMetadata.length + selectedPayload.length;

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!eventType.trim() || !outputTopic.trim()) {
      setError('Event type and output topic are required.');
      return;
    }
    if (fieldCount === 0) {
      setError('Select at least one field.');
      return;
    }
    setError('');
    const fieldPaths = [
      ...selectedMetadata.map((f) => `${METADATA_PREFIX}${f}`),
      ...selectedPayload.map((f) => `${PAYLOAD_PREFIX}${f}`),
    ];
    if (isEdit) {
      // No backend update endpoint yet — UI-only for now.
      onClose();
      return;
    }
    try {
      await onSubmit({
        subscriberId: slugify(name),
        subscriberName: name.trim(),
        eventType: eventType.trim(),
        outputTopic: outputTopic.trim(),
        fieldPaths,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    }
  };

  return (
    <div className={styles.overlay} onClick={onClose}>
      {step === 1 && (
        <div className={styles.modal} onClick={stop}>
          <div className={styles.header}>
            <div className={styles.headerLeft}>
              <div className={styles.icon}>
                <Icon name={isEdit ? 'edit' : 'add'} size={23} />
              </div>
              <h2 className={styles.title}>{isEdit ? 'Edit subscription' : 'Add new subscription'}</h2>
            </div>
            <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
              <Icon name="close" size={20} />
            </button>
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <div className={styles.field}>
            <label>Subscription name</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Order Fulfillment Sync"
            />
          </div>

          <div className={styles.field}>
            <label>Type</label>
            <div className={styles.typeGrid}>
              <button
                type="button"
                className={`${styles.typeOption} ${type === 'API Callback' ? styles.typeOptionSelected : ''}`}
                onClick={() => setType('API Callback')}
              >
                <Icon name="webhook" size={20} />
                <div>
                  API Callback
                  <div className={styles.typeSub}>POST to a URL</div>
                </div>
              </button>
              <button
                type="button"
                className={`${styles.typeOption} ${type === 'Event' ? styles.typeOptionSelected : ''}`}
                onClick={() => setType('Event')}
              >
                <Icon name="bolt" size={20} />
                <div>
                  Event
                  <div className={styles.typeSub}>Internal consumer</div>
                </div>
              </button>
            </div>
          </div>

          <div className={styles.footer}>
            <button type="button" className={styles.cancelBtn} onClick={onClose}>
              Cancel
            </button>
            <button
              type="button"
              className={styles.submitBtn}
              disabled={!name.trim()}
              onClick={proceedToFields}
            >
              Next
            </button>
          </div>
        </div>
      )}

      {step === 2 && (
        <div className={`${styles.modal} ${styles.modalWide}`} onClick={stop}>
          <div className={styles.header}>
            <div>
              <h2 className={styles.title}>Select subscription fields</h2>
              <p className={styles.stepHint}>Choose which data fields to include in this subscription.</p>
            </div>
            <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
              <Icon name="close" size={20} />
            </button>
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <div className={styles.grid}>
            <div className={styles.field}>
              <label>Event Type</label>
              <input
                value={eventType}
                onChange={(e) => setEventType(e.target.value)}
                placeholder="e.g. order.created"
              />
            </div>
            <div className={styles.field}>
              <label>Output Topic</label>
              <input
                value={outputTopic}
                onChange={(e) => setOutputTopic(e.target.value)}
                placeholder="e.g. my-service-events"
              />
            </div>
          </div>

          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              Metadata <span>({METADATA_FIELDS.length} fields)</span>
            </h3>
            <div className={styles.fieldGrid}>
              {METADATA_FIELDS.map((f) => {
                const selected = selectedMetadata.includes(f);
                return (
                  <button
                    type="button"
                    key={f}
                    className={`${styles.fieldOption} ${selected ? styles.fieldOptionSelected : ''}`}
                    onClick={() => toggleMetadata(f)}
                  >
                    <span className={styles.checkbox}>
                      {selected && <Icon name="check" size={13} />}
                    </span>
                    {f}
                  </button>
                );
              })}
            </div>
          </div>

          <div className={styles.section}>
            <h3 className={styles.sectionTitle}>
              Payload <span>({PAYLOAD_FIELDS.length} fields)</span>
            </h3>
            <div className={styles.fieldGrid}>
              {PAYLOAD_FIELDS.map((f) => {
                const selected = selectedPayload.includes(f);
                return (
                  <button
                    type="button"
                    key={f}
                    className={`${styles.fieldOption} ${selected ? styles.fieldOptionSelected : ''}`}
                    onClick={() => togglePayload(f)}
                  >
                    <span className={styles.checkbox}>
                      {selected && <Icon name="check" size={13} />}
                    </span>
                    {f}
                  </button>
                );
              })}
            </div>
          </div>

          <div className={styles.selectionSummary}>
            <Icon name="check_circle" size={18} />
            {fieldCount} field{fieldCount === 1 ? '' : 's'} selected
          </div>

          <div className={styles.footer}>
            <button type="button" className={styles.cancelBtn} onClick={onClose}>
              Cancel
            </button>
            <button type="button" className={styles.submitBtn} onClick={handleCreate}>
              {isEdit ? 'Save changes' : 'Create subscription'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
