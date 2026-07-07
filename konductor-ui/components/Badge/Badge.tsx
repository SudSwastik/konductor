import styles from './Badge.module.scss';

type Variant = 'pending' | 'approved' | 'rejected' | 'retried' | 'discarded';

const VARIANT_MAP: Record<string, Variant> = {
  PENDING: 'pending',
  APPROVED: 'approved',
  ACTIVE: 'approved',
  REJECTED: 'rejected',
  RETRIED: 'retried',
  DISCARDED: 'discarded',
  DISABLED: 'discarded',
};

interface BadgeProps {
  label: string;
  plain?: boolean;
}

export default function Badge({ label, plain = false }: BadgeProps) {
  const variant: Variant = VARIANT_MAP[label.toUpperCase()] ?? 'pending';
  const className = plain
    ? `${styles.plain} ${styles[variant]}`
    : `${styles.badge} ${styles[variant]}`;
  return <span className={className}>{label}</span>;
}
