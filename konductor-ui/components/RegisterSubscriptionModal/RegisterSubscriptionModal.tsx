'use client';

import { FormEvent, KeyboardEvent, MouseEvent, useState } from 'react';
import Icon from '@/components/Icon/Icon';
import { RegisterSubscriptionRequest } from '@/types';
import styles from './RegisterSubscriptionModal.module.scss';

interface Props {
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

const EMPTY_IDENTITY = { subscriberId: '', subscriberName: '', eventType: '', outputTopic: '' };

export default function RegisterSubscriptionModal({ onClose, onSubmit }: Props) {
  const [step, setStep] = useState<1 | 2>(1);
  const [identity, setIdentity] = useState(EMPTY_IDENTITY);
  const [selectedMetadata, setSelectedMetadata] = useState<string[]>([]);
  const [payloadPaths, setPayloadPaths] = useState<string[]>([]);
  const [payloadInput, setPayloadInput] = useState('');
  const [error, setError] = useState('');

  const stop = (e: MouseEvent) => e.stopPropagation();

  const setField = (k: keyof typeof identity, v: string) =>
    setIdentity((f) => ({ ...f, [k]: v }));

  const identityValid =
    identity.subscriberId.trim() &&
    identity.subscriberName.trim() &&
    identity.eventType.trim() &&
    identity.outputTopic.trim();

  const proceedToFields = () => {
    if (!identityValid) return;
    setError('');
    setStep(2);
  };

  const toggleMetadata = (field: string) =>
    setSelectedMetadata((prev) =>
      prev.includes(field) ? prev.filter((f) => f !== field) : [...prev, field],
    );

  const addPayloadPath = () => {
    const path = payloadInput.trim();
    if (!path || payloadPaths.includes(path)) return;
    setPayloadPaths((prev) => [...prev, path]);
    setPayloadInput('');
  };

  const removePayloadPath = (path: string) =>
    setPayloadPaths((prev) => prev.filter((p) => p !== path));

  const handlePayloadKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' || e.key === ',') {
      e.preventDefault();
      addPayloadPath();
    }
  };

  const fieldCount = selectedMetadata.length + payloadPaths.length;

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (fieldCount === 0) {
      setError('Select at least one field.');
      return;
    }
    setError('');
    const fieldPaths = [
      ...selectedMetadata.map((f) => `$.metadata.${f}`),
      ...payloadPaths,
    ];
    try {
      await onSubmit({ ...identity, fieldPaths });
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
                <Icon name="add" size={23} />
              </div>
              <h2 className={styles.title}>Add new subscription</h2>
            </div>
            <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
              <Icon name="close" size={20} />
            </button>
          </div>

          {error && <p className={styles.error}>{error}</p>}

          <div className={styles.grid}>
            <div className={styles.field}>
              <label>Subscriber ID</label>
              <input
                value={identity.subscriberId}
                onChange={(e) => setField('subscriberId', e.target.value)}
                placeholder="e.g. analytics-service"
              />
            </div>
            <div className={styles.field}>
              <label>Subscriber Name</label>
              <input
                value={identity.subscriberName}
                onChange={(e) => setField('subscriberName', e.target.value)}
                placeholder="e.g. Analytics Service"
              />
            </div>
          </div>

          <div className={styles.field}>
            <label>Event Type</label>
            <input
              value={identity.eventType}
              onChange={(e) => setField('eventType', e.target.value)}
              placeholder="e.g. order.created"
            />
          </div>

          <div className={styles.field}>
            <label>Output Topic</label>
            <input
              value={identity.outputTopic}
              onChange={(e) => setField('outputTopic', e.target.value)}
              placeholder="e.g. my-service-events"
            />
          </div>

          <div className={styles.footer}>
            <button type="button" className={styles.cancelBtn} onClick={onClose}>
              Cancel
            </button>
            <button
              type="button"
              className={styles.submitBtn}
              disabled={!identityValid}
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
              Payload <span>(JSONPath expressions)</span>
            </h3>
            <div className={styles.pathInputRow}>
              <input
                value={payloadInput}
                onChange={(e) => setPayloadInput(e.target.value)}
                onKeyDown={handlePayloadKeyDown}
                placeholder="$.payload.orderId"
              />
              <button type="button" className={styles.addBtn} onClick={addPayloadPath}>
                Add
              </button>
            </div>
            {payloadPaths.length > 0 && (
              <div className={styles.chips}>
                {payloadPaths.map((p) => (
                  <span key={p} className={styles.chip}>
                    {p}
                    <button type="button" onClick={() => removePayloadPath(p)} aria-label={`Remove ${p}`}>
                      &times;
                    </button>
                  </span>
                ))}
              </div>
            )}
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
              Create subscription
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
