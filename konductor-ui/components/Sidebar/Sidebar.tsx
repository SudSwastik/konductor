'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import styles from './Sidebar.module.scss';

const NAV = [
  { href: '/admin/subscriptions', label: 'Subscriptions' },
  { href: '/admin/dlq', label: 'Dead Letter Queue' },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className={styles.sidebar}>
      <div className={styles.logo}>Konductor</div>
      <nav className={styles.nav}>
        {NAV.map(({ href, label }) => (
          <Link
            key={href}
            href={href}
            className={`${styles.link} ${pathname.startsWith(href) ? styles.active : ''}`}
          >
            {label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
