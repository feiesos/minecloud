import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../api/auth';
import './AuthPage.css';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!email.trim()) {
      setError('请输入邮箱地址');
      return;
    }
    setLoading(true);
    try {
      await forgotPassword(email.trim());
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '发送失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>Minecloud</h1>
          <p>找回密码</p>
        </div>
        {success ? (
          <>
            <div className="auth-success">
              如果该邮箱已注册，我们会发送一封密码重置邮件，请查收。
            </div>
            <div className="auth-switch" style={{ marginTop: '1rem' }}>
              <Link to="/login">返回登录</Link>
            </div>
          </>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            {error && <div className="auth-error">{error}</div>}
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.88rem', lineHeight: 1.6 }}>
              请输入注册时使用的邮箱地址，我们会向你发送密码重置链接。
            </p>
            <div className="form-group">
              <label htmlFor="email">邮箱</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="请输入注册邮箱"
                autoComplete="email"
                autoFocus
              />
            </div>
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? '发送中...' : '发送重置邮件'}
            </button>
          </form>
        )}
        {!success && (
          <div className="auth-switch">
            <Link to="/login">返回登录</Link>
          </div>
        )}
      </div>
    </div>
  );
}
