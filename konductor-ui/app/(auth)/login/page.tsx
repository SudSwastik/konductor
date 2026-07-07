'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import Icon from '@/components/Icon/Icon';
import styles from '../AuthForm.module.scss';

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim() || !password.trim()) {
      setError('Enter your email and password.');
      return;
    }
    if (!EMAIL_RE.test(email)) {
      setError('Enter a valid email address.');
      return;
    }
    setError('');
    router.push('/admin/subscriptions');
  };

  return (
    <form onSubmit={handleSubmit}>
      <h1 className={styles.title}>Welcome back</h1>
      <p className={styles.subtitle}>Sign in to manage your data subscriptions.</p>

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
          placeholder="••••••••"
        />
      </div>

      {error && (
        <p className={styles.error}>
          <Icon name="error" size={17} />
          {error}
        </p>
      )}

      <button type="submit" className={styles.submitBtn}>Sign in</button>

      <p className={styles.switchRow}>
        Don&apos;t have an account?{' '}
        <Link href="/signup" className={styles.switchBtn}>Create one</Link>
      </p>
    </form>
  );
}
