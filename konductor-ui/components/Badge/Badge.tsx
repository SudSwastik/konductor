import styles from './Badge.module.scss';

type Variant = 'pending' | 'approved' | 'rejected' | 'retried' | 'discarded';

const VARIANT_MAP: Record<string, Variant> = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
  RETRIED: 'retried',
  DISCARDED: 'discarded',
};

interface BadgeProps {
  label: string;
}

export default function Badge({ label }: BadgeProps) {
  const variant: Variant = VARIANT_MAP[label.toUpperCase()] ?? 'pending';
  return <span className={`${styles.badge} ${styles[variant]}`}>{label}</span>;
}
