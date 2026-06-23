import { useState, useEffect, startTransition } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { logout } from '../api/auth';
import { listRecycle, restoreItem, purgeItem, type RecycleItem } from '../api/recycle';
import AppHeader from '../components/AppHeader';
import DirIcon from '../components/DirIcon';
import FileIcon from '../components/FileIcon';
import TrashIcon from '../components/icons/TrashIcon';
import LoadingSpinner from '../components/LoadingSpinner';
import { formatRelativeDate } from '../utils/format';
import './RecycleBin.css';

export default function RecycleBin() {
  const navigate = useNavigate();
  const [items, setItems] = useState<RecycleItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [operating, setOperating] = useState<string | null>(null);

  async function loadItems() {
    setLoading(true);
    setError('');
    try {
      const data = await listRecycle();
      setItems(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载回收站失败');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    startTransition(() => {
      loadItems();
    });
  }, []);

  async function handleRestore(id: string) {
    setOperating(id);
    try {
      await restoreItem(id);
      setItems((prev) => prev.filter((i) => i.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : '还原失败');
    } finally {
      setOperating(null);
    }
  }

  async function handlePurge(id: string, name: string) {
    if (!confirm(`确定永久删除「${name}」？此操作不可撤销。`)) return;
    setOperating(id);
    try {
      await purgeItem(id);
      setItems((prev) => prev.filter((i) => i.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : '永久删除失败');
    } finally {
      setOperating(null);
    }
  }

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="recycle-page">
      <AppHeader title="回收站" onLogout={handleLogout} />
      <div className="recycle-body">
        <div className="recycle-container">
          <div className="recycle-title">
            <h2>回收站</h2>
            <Link to="/files" className="recycle-back-link">&larr; 返回文件管理</Link>
          </div>

          {error && <div className="recycle-error">{error}</div>}

          {loading ? (
            <LoadingSpinner />
          ) : items.length === 0 ? (
            <div className="recycle-empty">
              <TrashIcon size={48} />
              <p>回收站为空</p>
              <span className="recycle-empty-tip">删除的文件将出现在这里</span>
            </div>
          ) : (
            <div className="recycle-list">
              {items.map((item) => (
                <div key={item.id} className="recycle-item">
                  <div className="recycle-item-icon">
                    {item.isDir ? <DirIcon /> : <FileIcon name={item.name} />}
                  </div>
                  <div className="recycle-item-info">
                    <div className="recycle-item-name">{item.name}</div>
                    <div className="recycle-item-meta">
                      {item.isDir ? '文件夹' : `${formatFileSize(item.size)}`}
                      <span className="recycle-meta-sep">·</span>
                      删除于 {formatRelativeDate(item.deleteTime)}
                    </div>
                  </div>
                  <div className="recycle-item-actions">
                    <button
                      className="btn-restore"
                      disabled={operating === item.id}
                      onClick={() => handleRestore(item.id)}
                    >
                      {operating === item.id ? '处理中…' : '还原'}
                    </button>
                    <button
                      className="btn-purge"
                      disabled={operating === item.id}
                      onClick={() => handlePurge(item.id, item.name)}
                    >
                      永久删除
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
}
