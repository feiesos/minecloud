import { useNavigate } from 'react-router-dom';
import { getStoredUser, logout } from '../api/auth';
import './Home.css';

export default function Home() {
  const navigate = useNavigate();
  const user = getStoredUser();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="home">
      <header className="home-header">
        <div className="home-brand">Minecloud</div>
        <div className="home-user">
          <span>{user?.nickname || user?.username}</span>
          <button className="btn-logout" onClick={handleLogout}>
            退出
          </button>
        </div>
      </header>
      <main className="home-body">
        <div className="home-placeholder">
          <div className="icon">&#128194;</div>
          <h2>欢迎使用 Minecloud</h2>
          <p>
            {user
              ? `已登录为 ${user.nickname || user.username}，文件管理功能即将上线`
              : '文件管理功能即将上线'}
          </p>
        </div>
      </main>
    </div>
  );
}
