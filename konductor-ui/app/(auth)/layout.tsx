import Icon from '@/components/Icon/Icon';
import styles from './layout.module.scss';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className={styles.shell}>
      <div className={styles.brand}>
        <div className={styles.brandGlow} />
        <div className={styles.brandLogo}>
          <span className={styles.brandLogoIcon}>
            <Icon name="hub" size={22} />
          </span>
          Konductor
        </div>
        <div className={styles.brandCopy}>
          <h2 className={styles.brandTitle}>Route every event,<br />everywhere.</h2>
          <p className={styles.brandSubtitle}>
            Connect callbacks and consumers to your workspace events with granular,
            scope-based access control.
          </p>
        </div>
        <div className={styles.brandFooter}>© 2026 Konductor</div>
      </div>

      <div className={styles.formPanel}>
        <div className={styles.formInner}>{children}</div>
      </div>
    </div>
  );
}
