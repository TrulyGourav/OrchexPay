import React from 'react';
import { Link } from 'react-router-dom';
import styles from './Unauthorized.module.css';

export default function Unauthorized() {
  return (
    <div className={styles.wrapper}>
      <div className={styles.card}>
        <h1 className={styles.title}>403</h1>
        <p className={styles.text}>You don’t have permission to view this page.</p>
        <Link to="/login" className={styles.link}>Back to login</Link>
      </div>
    </div>
  );
}
