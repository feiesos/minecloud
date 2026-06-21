import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { verifyEmail } from '../api/auth';
import './AuthPage.css';

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!token) {
      setStatus('error');
      setMessage('缺少验证令牌');
      return;
    }
    verifyEmail(token)
      .then(() => {
        setStatus('success');
        setMessage('邮箱验证成功！你现在可以登录了。');
      })
      .catch((err) => {
        setStatus('error');
        setMessage(err instanceof Error ? err.message : '验证失败');
      });
  }, [token]);

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>Minecloud</h1>
          <p>邮箱验证</p>
        </div>
        {status === 'loading' && (
          <p style={{ textAlign: 'center', color: 'var(--color-text-secondary)' }}>
            正在验证...
          </p>
        )}
        {status === 'success' && (
          <div className="auth-success">{message}</div>
        )}
        {status === 'error' && (
          <div className="auth-error">{message}</div>
        )}
        {(status === 'success' || status === 'error') && (
          <div className="auth-switch" style={{ marginTop: '1rem' }}>
            <Link to="/login">去登录</Link>
          </div>
        )}
      </div>
    </div>
  );
}
