import React from 'react';
import styles from './ErrorMessage.module.css';

export default function ErrorMessage({ message, onDismiss }) {
  if (!message) return null;
  return (
    <div className={styles.wrapper} role="alert">
      {message}
      {onDismiss && (
        <button type="button" className={styles.dismiss} onClick={onDismiss} aria-label="Dismiss">×</button>
      )}
    </div>
  );
}
