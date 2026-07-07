'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import Icon, { IconName } from '@/components/Icon/Icon';
import styles from '../AuthForm.module.scss';

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

type Scope = 'read' | 'write' | 'admin';

const SCOPES: { key: Scope; label: string; icon: IconName; desc: string }[] = [
  { key: 'read', label: 'Read', icon: 'visibility', desc: 'View subscriptions & event logs' },
  { key: 'write', label: 'Write', icon: 'edit', desc: 'Create & manage subscriptions' },
  { key: 'admin', label: 'Admin', icon: 'shield_person', desc: 'Full access & member control' },
];

export default function SignupPage() {
  const router = useRouter();
  const [step, setStep] = useState<1 | 2>(1);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [scope, setScope] = useState<Scope>('read');
  const [error, setError] = useState('');

  const next = () => {
    if (!name.trim() || !email.trim() || !password.trim()) {
      setError('Please fill in all fields.');
      return;
    }
    if (!EMAIL_RE.test(email)) {
      setError('Enter a valid email address.');
      return;
    }
    if (password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }
    setError('');
    setStep(2);
  };

  const submit = () => {
    router.push('/admin/subscriptions');
  };

  return (
    <div>
      <div className={styles.stepIndicator}>
        <span className={`${styles.stepBar} ${styles.stepBarActive}`} />
        <span className={`${styles.stepBar} ${step === 2 ? styles.stepBarActive : ''}`} />
      </div>

      {step === 1 && (
        <div>
          <h1 className={styles.title}>Create your account</h1>
          <p className={styles.subtitle}>Step 1 of 2 — your details.</p>

          <div className={styles.field}>
            <label>Full name</label>
            <input value={name} onChange={(e) => { setName(e.target.value); setError(''); }} placeholder="Jane Doe" />
          </div>

          <div className={styles.field}>
            <label>Email</label>
            <input
              value={email}
              onChange={(e) => { setEmail(e.target.value); setError(''); }}
              placeholder="you@company.com"
            />
          </div>

          <div className={styles.field}>
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); setError(''); }}
              placeholder="At least 6 characters"
            />
          </div>

          {error && (
            <p className={styles.error}>
              <Icon name="error" size={17} />
              {error}
            </p>
          )}

          <button type="button" className={styles.submitBtn} onClick={next}>Next</button>
        </div>
      )}

      {step === 2 && (
        <div>
          <h1 className={styles.title}>Choose access scope</h1>
          <p className={styles.subtitle}>Step 2 of 2 — pick your permission level.</p>

          <div className={styles.scopeList}>
            {SCOPES.map((s) => {
              const selected = scope === s.key;
              return (
                <button
                  type="button"
                  key={s.key}
                  className={`${styles.scopeCard} ${selected ? styles.scopeCardSelected : ''}`}
                  onClick={() => setScope(s.key)}
                >
                  <span className={styles.scopeIcon}>
                    <Icon name={s.icon} size={20} />
                  </span>
                  <span className={styles.scopeBody}>
                    <span className={styles.scopeLabel}>{s.label}</span>
                    <span className={styles.scopeDesc}>{s.desc}</span>
                  </span>
                  <span className={styles.scopeDot}>
                    {selected && <Icon name="check" size={12} />}
                  </span>
                </button>
              );
            })}
          </div>

          <div className={styles.stepFooter}>
            <button type="button" className={styles.backBtn} onClick={() => setStep(1)}>Back</button>
            <button type="button" className={styles.nextBtn} onClick={submit}>Create account</button>
          </div>
        </div>
      )}

      <p className={styles.switchRow}>
        Already have an account?{' '}
        <Link href="/login" className={styles.switchBtn}>Sign in</Link>
      </p>
    </div>
  );
}
