import styles from './StatCards.module.scss';

export interface StatCard {
  label: string;
  value: number | string;
}

export default function StatCards({ cards }: { cards: StatCard[] }) {
  return (
    <div className={styles.grid}>
      {cards.map((c) => (
        <div key={c.label} className={styles.card}>
          <div className={styles.label}>{c.label}</div>
          <div className={styles.value}>{c.value}</div>
        </div>
      ))}
    </div>
  );
}
