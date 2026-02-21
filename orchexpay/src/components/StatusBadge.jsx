import React from 'react';
import styles from './StatusBadge.module.css';

const STATUS_CLASS = {
  CONFIRMED: styles.confirmed,
  PENDING: styles.pending,
  REVERSED: styles.reversed,
  SETTLED: styles.settled,
  FAILED: styles.failed,
  PROCESSING: styles.processing,
  CREATED: styles.created,
  ACTIVE: styles.active,
  SUSPENDED: styles.suspended,
  CLOSED: styles.closed,
};

export default function StatusBadge({ status }) {
  const s = (status || '').toUpperCase();
  const cls = STATUS_CLASS[s] || styles.default;
  return <span className={`${styles.badge} ${cls}`}>{status || '—'}</span>;
}
