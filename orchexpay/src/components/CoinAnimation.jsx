import React from 'react';
import styles from './CoinAnimation.module.css';

export default function CoinAnimation({ tagline = 'Your balance, your control' }) {
  return (
    <div className={styles.wrapper} role="presentation" aria-hidden="true">
      <div className={styles.coinStack}>
        <div className={styles.coin} />
        <div className={styles.coin} />
        <div className={styles.coin} />
        <span className={styles.sparkle} />
      </div>
      {tagline && <p className={styles.tagline}>{tagline}</p>}
    </div>
  );
}
