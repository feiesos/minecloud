import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { resetPassword } from '../api/auth';
import './AuthPage.css';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [newPassword, setNewPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-logo">
            <h1>Minecloud</h1>
            <p>重置密码</p>
          </div>
          <div className="auth-error">缺少有效的重置令牌</div>
          <div className="auth-switch" style={{ marginTop: '1rem' }}>
            <Link to="/login">返回登录</Link>
          </div>
        </div>
      </div>
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    if (!newPassword) {
      setError('请输入新密码');
      return;
    }
    if (newPassword !== password2) {
      setError('两次密码不一致');
      return;
    }
    if (newPassword.length < 8) {
      setError('密码至少8位，需包含大小写字母和数字');
      return;
    }
    setLoading(true);
    try {
      await resetPassword(token!, newPassword);
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '重置失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>Minecloud</h1>
          <p>重置密码</p>
        </div>
        {success ? (
          <>
            <div className="auth-success">密码重置成功！</div>
            <div className="auth-switch" style={{ marginTop: '1rem' }}>
              <Link to="/login">去登录</Link>
            </div>
          </>
        ) : (
          <form className="auth-form" onSubmit={handleSubmit}>
            {error && <div className="auth-error">{error}</div>}
            <div className="form-group">
              <label htmlFor="newPassword">新密码</label>
              <input
                id="newPassword"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="至少8位，包含大小写字母和数字"
                autoComplete="new-password"
                autoFocus
              />
            </div>
            <div className="form-group">
              <label htmlFor="password2">确认新密码</label>
              <input
                id="password2"
                type="password"
                value={password2}
                onChange={(e) => setPassword2(e.target.value)}
                placeholder="再次输入新密码"
                autoComplete="new-password"
              />
            </div>
            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? '重置中...' : '重置密码'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
