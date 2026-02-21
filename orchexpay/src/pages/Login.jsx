import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import styles from './Login.module.css';

const DEFAULT_LOGINS = [
  { username: 'admin', password: 'password', label: 'Admin' },
  { username: 'merchant1', password: 'password', label: 'Merchant (demo)' },
  { username: 'vendor1', password: 'password', label: 'Vendor (demo)' },
];

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!username.trim() || !password) {
      setError('Username and password required');
      return;
    }
    setLoading(true);
    try {
      const user = await login(username.trim(), password);
      if (user.roles.includes('ADMIN')) navigate('/admin', { replace: true });
      else if (user.roles.includes('MERCHANT')) navigate('/merchant', { replace: true });
      else if (user.roles.includes('VENDOR')) navigate('/vendor', { replace: true });
      else navigate('/unauthorized', { replace: true });
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.status === 401
        ? 'Invalid username or password'
        : err.message || 'Login failed';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const fillDemo = (cred) => {
    setUsername(cred.username);
    setPassword(cred.password);
    setError('');
  };

  return (
    <div className={styles.card}>
      <h1 className={styles.title}>LedgerX</h1>
      <p className={styles.subtitle}>Sign in</p>
      <form onSubmit={handleSubmit} className={styles.form}>
        <label className={styles.label}>Quick login (select to fill)</label>
        <select
          className={styles.select}
          value=""
          onChange={(e) => {
            const opt = DEFAULT_LOGINS[e.target.selectedIndex - 1];
            if (opt) fillDemo(opt);
          }}
        >
          <option value="">Choose a role...</option>
          {DEFAULT_LOGINS.map((d) => (
            <option key={d.username} value={d.username}>
              {d.label}
            </option>
          ))}
        </select>
        <label className={styles.label}>Username</label>
        <input
          type="text"
          className={styles.input}
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
        />
        <label className={styles.label}>Password</label>
        <input
          type="password"
          className={styles.input}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
        {error && <p className={styles.error}>{error}</p>}
        <button type="submit" className={styles.button} disabled={loading}>
          {loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
      <p className={styles.footer}>
        No account? <a href="/signup">Sign up</a> (Merchant)
      </p>
    </div>
  );
}
