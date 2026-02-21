import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usersApi } from '../api/users';
import styles from './Login.module.css';

export default function Signup() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [currencyCode, setCurrencyCode] = useState('INR');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (!username.trim() || username.length < 2) {
      setError('Username must be at least 2 characters');
      return;
    }
    if (!password || password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    setLoading(true);
    try {
      await usersApi.createUser({
        username: username.trim(),
        password,
        roles: ['MERCHANT'],
        currencyCode: currencyCode || 'INR',
      });
      setSuccess('Merchant account created. Sign in with your username and password.');
      setUsername('');
      setPassword('');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.status === 403
        ? 'Only an admin can create accounts, or use the quick login.'
        : err.message || 'Sign up failed';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.card}>
      <h1 className={styles.title}>LedgerX</h1>
      <p className={styles.subtitle}>Register as Merchant</p>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.label}>Username</label>
        <input
          type="text"
          className={styles.input}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          minLength={2}
          autoComplete="username"
        />
        <label className={styles.label}>Password (min 8 characters)</label>
        <input
          type="password"
          className={styles.input}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          minLength={8}
          autoComplete="new-password"
        />
        <label className={styles.label}>Currency</label>
        <input
          type="text"
          className={styles.input}
          value={currencyCode}
          onChange={(e) => setCurrencyCode(e.target.value.toUpperCase().slice(0, 3))}
          placeholder="INR"
          maxLength={3}
        />
        {error && <p className={styles.error}>{error}</p>}
        {success && <p style={{ color: 'green', margin: 0, fontSize: '0.9rem' }}>{success}</p>}
        <button type="submit" className={styles.button} disabled={loading}>
          {loading ? 'Creating...' : 'Create account'}
        </button>
      </form>
      <p className={styles.footer}>
        Already have an account? <a href="/login">Sign in</a>
      </p>
    </div>
  );
}
