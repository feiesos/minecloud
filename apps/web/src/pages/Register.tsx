import { useState } from 'react';
import { Link } from 'react-router-dom';
import { register } from '../api/auth';
import './AuthPage.css';

export default function Register() {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [password2, setPassword2] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (!username.trim() || !email.trim() || !password) {
      setError('请填写所有必填项');
      return;
    }
    if (password !== password2) {
      setError('两次密码不一致');
      return;
    }
    if (password.length < 8) {
      setError('密码至少8位，需包含大小写字母和数字');
      return;
    }
    setLoading(true);
    try {
      await register(username.trim(), password, email.trim());
      setSuccess('注册成功！验证邮件已发送至你的邮箱，请查收并完成验证后登录。');
    } catch (err) {
      setError(err instanceof Error ? err.message : '注册失败');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-logo">
          <h1>Minecloud</h1>
          <p>创建你的私有云账号</p>
        </div>
        <form className="auth-form" onSubmit={handleSubmit}>
          {error && <div className="auth-error">{error}</div>}
          {success && <div className="auth-success">{success}</div>}
          <div className="form-group">
            <label htmlFor="username">用户名</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="请输入用户名"
              autoComplete="username"
              autoFocus
            />
          </div>
          <div className="form-group">
            <label htmlFor="email">邮箱</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="请输入邮箱"
              autoComplete="email"
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">密码</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="至少8位，包含大小写字母和数字"
              autoComplete="new-password"
            />
          </div>
          <div className="form-group">
            <label htmlFor="password2">确认密码</label>
            <input
              id="password2"
              type="password"
              value={password2}
              onChange={(e) => setPassword2(e.target.value)}
              placeholder="再次输入密码"
              autoComplete="new-password"
            />
          </div>
          <button className="btn-primary" type="submit" disabled={loading || !!success}>
            {loading ? '注册中...' : success ? '已注册' : '注册'}
          </button>
        </form>
        <div className="auth-switch">
          已有账号？<Link to="/login">立即登录</Link>
        </div>
      </div>
    </div>
  );
}
