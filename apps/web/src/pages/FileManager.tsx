import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { logout } from '../api/auth';
import type { FileItem } from '../api/files';
import {
  listFiles,
  createDirectory,
  renameFile,
  deleteFile,
  downloadFile,
  batchDelete,
  batchMove,
  batchCopy,
} from '../api/files';
import { useUpload } from '../hooks/useUpload';
import { searchFiles, type SearchResult, type SearchParams } from '../api/search';
import AppHeader from '../components/AppHeader';
import DirIcon from '../components/DirIcon';
import FileIcon from '../components/FileIcon';
import LoadingSpinner from '../components/LoadingSpinner';
import PencilIcon from '../components/icons/PencilIcon';
import DownloadIcon from '../components/icons/DownloadIcon';
import TrashIcon from '../components/icons/TrashIcon';
import UploadIcon from '../components/icons/UploadIcon';
import PlusIcon from '../components/icons/PlusIcon';
import ShareIcon from '../components/icons/ShareIcon';
import CopyIcon from '../components/icons/CopyIcon';
import PreviewModal from '../components/PreviewModal';
import ShareDialog from '../components/ShareDialog';
import { formatSize, formatRelativeDate } from '../utils/format';
import './FileManager.css';

interface Breadcrumb {
  id: string;
  name: string;
}

export default function FileManager() {
  const navigate = useNavigate();

  const [items, setItems] = useState<FileItem[]>([]);
  const [error, setError] = useState('');
  const [breadcrumb, setBreadcrumb] = useState<Breadcrumb[]>([
    { id: '0', name: 'root' },
  ]);

  const { upload: uploadFileAction, uploading, uploadProgress } = useUpload();
  const [showNewFolder, setShowNewFolder] = useState(false);
  const [newFolderName, setNewFolderName] = useState('');
  const [renaming, setRenaming] = useState<{ id: string; name: string } | null>(null);
  const [renameValue, setRenameValue] = useState('');
  const [previewItem, setPreviewItem] = useState<{ id: string; name: string } | null>(null);

  const [searchQuery, setSearchQuery] = useState('');
  const [searchType, setSearchType] = useState<'all' | 'file' | 'folder'>('all');
  const [searchSort, setSearchSort] = useState<'date' | 'name' | 'size'>('date');
  const [searchOrder, setSearchOrder] = useState<'desc' | 'asc'>('desc');
  const [searchResults, setSearchResults] = useState<SearchResult[] | null>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);

  const uploadRef = useRef<HTMLInputElement>(null);
  const newFolderRef = useRef<HTMLInputElement>(null);

  const currentParentId = breadcrumb[breadcrumb.length - 1].id;

  const [isLoading, setIsLoading] = useState(false);

  // ── Multi-select ──
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const lastClickedRef = useRef<string | null>(null);

  // ── Batch action dialogs ──
  const [showShareDialog, setShowShareDialog] = useState(false);
  const [dirPickerMode, setDirPickerMode] = useState<'move' | 'copy' | null>(null);
  const [dirPickerItems, setDirPickerItems] = useState<FileItem[]>([]);
  const [dirPickerBreadcrumb, setDirPickerBreadcrumb] = useState<Breadcrumb[]>([
    { id: '0', name: 'root' },
  ]);
  const [dirPickerLoading, setDirPickerLoading] = useState(false);
  const [dirPickerSelected, setDirPickerSelected] = useState<string>('0');

  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setError('');
    setSelectedIds(new Set());
    listFiles(currentParentId).then((data) => {
      if (cancelled) return;
      setItems(data);
    }).catch((err) => {
      if (cancelled) return;
      setError(err instanceof Error ? err.message : '加载失败');
    }).finally(() => {
      if (!cancelled) setIsLoading(false);
    });
    return () => { cancelled = true; };
  }, [currentParentId]);

  useEffect(() => {
    if (showNewFolder) {
      newFolderRef.current?.focus();
    }
  }, [showNewFolder]);

  const navigateToDir = useCallback((id: string, name: string) => {
    setBreadcrumb((prev) => [...prev, { id, name }]);
  }, []);

  function navigateBreadcrumb(index: number) {
    setBreadcrumb((prev) => prev.slice(0, index + 1));
  }

  async function reloadFiles() {
    try {
      const data = await listFiles(currentParentId);
      setItems(data);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    }
  }

  function clearSearch() {
    setSearchQuery('');
    setSearchResults(null);
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    const q = searchQuery.trim();
    if (!q) {
      setSearchResults(null);
      return;
    }
    try {
      const params: SearchParams = { q };
      if (searchType !== 'all') params.type = searchType;
      params.sort = searchSort;
      params.order = searchOrder;
      const res = await searchFiles(params);
      setSearchResults(res.items);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : '搜索失败');
    }
  }

  // ── Row click: toggle selection (default multi-select) ──
  function handleRowClick(item: FileItem, event: React.MouseEvent) {
    const target = event.target as HTMLElement;
    if (target.closest('.fm-name-link') || target.closest('.fm-row-actions') || target.closest('.fm-btn-row')) {
      return;
    }
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (event.shiftKey && lastClickedRef.current) {
        const ids = items.map((i) => i.id);
        const lastIdx = ids.indexOf(lastClickedRef.current);
        const curIdx = ids.indexOf(item.id);
        if (lastIdx !== -1 && curIdx !== -1) {
          const [start, end] = lastIdx < curIdx ? [lastIdx, curIdx] : [curIdx, lastIdx];
          for (let i = start; i <= end; i++) {
            next.add(ids[i]);
          }
        }
      } else {
        if (next.has(item.id)) {
          next.delete(item.id);
        } else {
          next.add(item.id);
        }
      }
      lastClickedRef.current = item.id;
      return next;
    });
  }

  function clearSelection() {
    setSelectedIds(new Set());
    lastClickedRef.current = null;
  }

  // ── Batch actions ──
  async function handleBatchDelete() {
    if (selectedIds.size === 0) return;
    if (!confirm(`确定删除选中的 ${selectedIds.size} 个文件/文件夹？`)) return;
    try {
      await batchDelete([...selectedIds]);
      clearSelection();
      await reloadFiles();
    } catch (err) {
      setError(err instanceof Error ? err.message : '批量删除失败');
    }
  }

  async function handleBatchMoveCopy() {
    if (!dirPickerMode || selectedIds.size === 0) return;
    const targetId = dirPickerSelected;
    try {
      const ids = [...selectedIds];
      if (dirPickerMode === 'move') {
        await batchMove(ids, targetId);
      } else {
        await batchCopy(ids, targetId);
      }
      clearSelection();
      setDirPickerMode(null);
      await reloadFiles();
    } catch (err) {
      setError(err instanceof Error ? err.message : `批量${dirPickerMode === 'move' ? '移动' : '复制'}失败`);
    }
  }

  // ── Dir picker ──
  function openDirPicker(mode: 'move' | 'copy') {
    setDirPickerMode(mode);
    setDirPickerBreadcrumb([{ id: '0', name: 'root' }]);
    setDirPickerSelected('0');
    loadDirPickerItems('0');
  }

  function loadDirPickerItems(parentId: string) {
    setDirPickerLoading(true);
    listFiles(parentId).then((data) => {
      setDirPickerItems(data.filter((f) => f.isDir));
    }).catch(() => {
      setDirPickerItems([]);
    }).finally(() => {
      setDirPickerLoading(false);
    });
  }

  function dirPickerNavigate(id: string, name: string) {
    setDirPickerBreadcrumb((prev) => [...prev, { id, name }]);
    loadDirPickerItems(id);
  }

  function dirPickerNavigateBreadcrumb(index: number) {
    const segs = dirPickerBreadcrumb.slice(0, index + 1);
    setDirPickerBreadcrumb(segs);
    loadDirPickerItems(segs[segs.length - 1].id);
  }

  // ── Upload ──
  async function handleUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setError('');
    try {
      const currentPath = '/' + breadcrumb.slice(1).map((s) => s.name).join('/');
      await uploadFileAction(file, currentPath);
      await reloadFiles();
    } catch (err) {
      setError(err instanceof Error ? err.message : '上传失败');
    } finally {
      if (uploadRef.current) uploadRef.current.value = '';
    }
  }

  async function handleCreateFolder(e: React.FormEvent) {
    e.preventDefault();
    const name = newFolderName.trim();
    if (!name) return;
    try {
      const currentPath = '/' + breadcrumb.slice(1).map((s) => s.name).join('/');
      await createDirectory(name, currentPath);
      setNewFolderName('');
      setShowNewFolder(false);
      await reloadFiles();
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建失败');
    }
  }

  async function handleRename(e: React.FormEvent) {
    e.preventDefault();
    if (!renaming) return;
    const name = renameValue.trim();
    if (!name) return;
    try {
      await renameFile(renaming.id, name);
      setRenaming(null);
      setRenameValue('');
      await reloadFiles();
    } catch (err) {
      setError(err instanceof Error ? err.message : '重命名失败');
    }
  }

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="fm">
      <AppHeader title="文件" onLogout={handleLogout} />

      <div className="fm-body">
        <div className="fm-content">
          <div className="fm-breadcrumb-bar">
            <nav className="fm-breadcrumb">
              {breadcrumb.map((seg, i) => (
                <span key={i} className="fm-bc-segment">
                  {i > 0 && <span className="fm-bc-sep">/</span>}
                  {i === breadcrumb.length - 1 ? (
                    <span className="fm-bc-current">{seg.name}</span>
                  ) : (
                    <button className="fm-bc-link" onClick={() => navigateBreadcrumb(i)}>
                      {seg.name}
                    </button>
                  )}
                </span>
              ))}
            </nav>

            <div className="fm-actions">
              <Link to="/recycle" className="fm-btn fm-btn-action fm-recycle-link">
                <TrashIcon size={14} />回收站
              </Link>
              <button
                className="fm-btn fm-btn-action"
                onClick={() => uploadRef.current?.click()}
                disabled={uploading}
              >
                {uploading ? (uploadProgress || '上传中…') : <><UploadIcon size={14} />上传文件</>}
              </button>
              <button className="fm-btn fm-btn-action" onClick={() => setShowNewFolder(true)}>
                <PlusIcon size={14} />新建文件夹
              </button>
              <input
                ref={uploadRef}
                type="file"
                className="fm-hidden-input"
                onChange={handleUpload}
              />
            </div>
          </div>

          <form className="fm-search-bar" onSubmit={handleSearch}>
            <input
              ref={searchInputRef}
              className="fm-search-input"
              type="text"
              placeholder="搜索文件…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
            <select
              className="fm-search-select"
              value={searchType}
              onChange={(e) => setSearchType(e.target.value as typeof searchType)}
            >
              <option value="all">全部</option>
              <option value="file">文件</option>
              <option value="folder">文件夹</option>
            </select>
            <select
              className="fm-search-select"
              value={searchSort}
              onChange={(e) => setSearchSort(e.target.value as typeof searchSort)}
            >
              <option value="date">日期</option>
              <option value="name">名称</option>
              <option value="size">大小</option>
            </select>
            <button
              type="button"
              className="fm-search-order"
              onClick={() => setSearchOrder((o) => (o === 'desc' ? 'asc' : 'desc'))}
              title={searchOrder === 'desc' ? '降序' : '升序'}
            >
              {searchOrder === 'desc' ? '↓' : '↑'}
            </button>
            <button className="fm-btn fm-btn-action" type="submit">搜索</button>
            {searchResults !== null && (
              <button className="fm-btn fm-btn-link" type="button" onClick={clearSearch}>
                清除
              </button>
            )}
          </form>

          {error && <div className="fm-error">{error}</div>}

          {searchResults !== null ? (
            searchResults.length === 0 ? (
              <div className="fm-empty">
                <p className="fm-empty-text">未找到匹配的文件</p>
              </div>
            ) : (
              <div className="fm-table-wrap">
                <table className="fm-table">
                  <thead>
                    <tr>
                      <th className="fm-th-name">名称</th>
                      <th className="fm-th-path">路径</th>
                      <th className="fm-th-size">大小</th>
                      <th className="fm-th-date">创建时间</th>
                      <th className="fm-th-action" />
                    </tr>
                  </thead>
                  <tbody>
                    {searchResults.map((item) => (
                      <tr key={item.id} className="fm-row">
                        <td className="fm-cell-name">
                          {item.isDir ? (
                            <button className="fm-name-link" onClick={() => { navigateToDir(item.id, item.name); clearSearch(); }}>
                              <DirIcon />
                              <span>{item.name}</span>
                            </button>
                          ) : (
                            <button
                              className="fm-name-link fm-btn-name"
                              onClick={() => setPreviewItem({ id: item.id, name: item.name })}
                            >
                              <FileIcon name={item.name} />
                              <span>{item.name}</span>
                            </button>
                          )}
                        </td>
                        <td className="fm-cell-path">{item.path}</td>
                        <td className="fm-cell-size">{item.isDir ? '—' : formatSize(item.size)}</td>
                        <td className="fm-cell-date">{formatRelativeDate(item.createTime)}</td>
                        <td className="fm-cell-action">
                          <div className="fm-row-actions">
                            {!item.isDir && (
                              <button
                                className="fm-btn fm-btn-row"
                                title="下载"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  downloadFile(item.id, item.name);
                                }}
                              >
                                <DownloadIcon size={14} />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
          ) : isLoading ? (
            <LoadingSpinner />
          ) : items.length === 0 && !showNewFolder && selectedIds.size === 0 ? (
            <div className="fm-empty">
              <DirIcon className="fm-empty-icon" fill="#8b949e" />
              <p className="fm-empty-text">此目录为空</p>
            </div>
          ) : (
            <>
              {showNewFolder && (
                <form className="fm-new-folder" onSubmit={handleCreateFolder}>
                  <DirIcon />
                  <input
                    ref={newFolderRef}
                    className="fm-input-inline"
                    value={newFolderName}
                    onChange={(e) => setNewFolderName(e.target.value)}
                    placeholder="文件夹名称"
                    onBlur={() => {
                      if (!newFolderName.trim()) setShowNewFolder(false);
                    }}
                  />
                  <button className="fm-btn fm-btn-primary-sm" type="submit">创建</button>
                  <button className="fm-btn fm-btn-link" type="button" onClick={() => setShowNewFolder(false)}>
                    取消
                  </button>
                </form>
              )}

              <div className={`fm-table-wrap${selectedIds.size > 0 ? ' fm-checkboxes-visible' : ''}`}>
                <table className="fm-table">
                  <thead>
                    <tr>
                      <th className="fm-th-check">
                        <input
                          type="checkbox"
                          className="fm-checkbox"
                          checked={items.length > 0 && selectedIds.size === items.length}
                          onChange={() => {
                            if (selectedIds.size === items.length) {
                              clearSelection();
                            } else {
                              setSelectedIds(new Set(items.map((i) => i.id)));
                            }
                          }}
                        />
                      </th>
                      <th className="fm-th-name">名称</th>
                      <th className="fm-th-size">大小</th>
                      <th className="fm-th-date">修改时间</th>
                      <th className="fm-th-action" />
                    </tr>
                  </thead>
                  <tbody>
                    {items.map((item) => (
                      <tr
                        key={item.id}
                        className={`fm-row${selectedIds.has(item.id) ? ' fm-row-selected' : ''}`}
                        onClick={(e) => handleRowClick(item, e)}
                      >
                        <td className="fm-cell-check">
                          <input
                            type="checkbox"
                            className="fm-checkbox"
                            checked={selectedIds.has(item.id)}
                            onChange={() => {}}
                            onClick={(e) => {
                              e.stopPropagation();
                              setSelectedIds((prev) => {
                                const next = new Set(prev);
                                if (next.has(item.id)) {
                                  next.delete(item.id);
                                } else {
                                  next.add(item.id);
                                }
                                return next;
                              });
                              lastClickedRef.current = item.id;
                            }}
                          />
                        </td>
                        <td className="fm-cell-name">
                          {item.isDir ? (
                            <button
                              className="fm-name-link"
                              onClick={(e) => {
                                e.stopPropagation();
                                navigateToDir(item.id, item.name);
                              }}
                            >
                              <DirIcon />
                              <span>{item.name}</span>
                            </button>
                          ) : (
                            <button
                              className="fm-name-link fm-btn-name"
                              onClick={(e) => {
                                e.stopPropagation();
                                setPreviewItem({ id: item.id, name: item.name });
                              }}
                            >
                              <FileIcon name={item.name} />
                              <span>{item.name}</span>
                            </button>
                          )}
                        </td>
                        <td className="fm-cell-size">{item.isDir ? '—' : formatSize(item.size)}</td>
                        <td className="fm-cell-date">{formatRelativeDate(item.createTime)}</td>
                        <td className="fm-cell-action">
                          <div className="fm-row-actions">
                            <button
                              className="fm-btn fm-btn-row"
                              title="重命名"
                              onClick={(e) => {
                                e.stopPropagation();
                                setRenaming({ id: item.id, name: item.name });
                                setRenameValue(item.name);
                              }}
                            >
                              <PencilIcon size={14} />
                            </button>
                            {!item.isDir && (
                              <button
                                className="fm-btn fm-btn-row"
                                title="下载"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  downloadFile(item.id, item.name);
                                }}
                              >
                                <DownloadIcon size={14} />
                              </button>
                            )}
                            <button
                              className="fm-btn fm-btn-row fm-btn-row-danger"
                              title="删除"
                              onClick={(e) => {
                                e.stopPropagation();
                                if (confirm('确定删除？')) {
                                  deleteFile(item.id).then(() => reloadFiles());
                                }
                              }}
                            >
                              <TrashIcon size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>

      {previewItem && (
        <PreviewModal
          id={previewItem.id}
          name={previewItem.name}
          onClose={() => setPreviewItem(null)}
        />
      )}

      {renaming && (
        <div className="fm-overlay" onClick={() => setRenaming(null)}>
          <form className="fm-modal" onSubmit={handleRename} onClick={(e) => e.stopPropagation()}>
            <h3 className="fm-modal-title">重命名</h3>
            <input
              className="fm-input"
              value={renameValue}
              onChange={(e) => setRenameValue(e.target.value)}
              autoFocus
            />
            <div className="fm-modal-actions">
              <button className="fm-btn fm-btn-primary" type="submit">确定</button>
              <button className="fm-btn" type="button" onClick={() => setRenaming(null)}>取消</button>
            </div>
          </form>
        </div>
      )}

      {showShareDialog && (
        <ShareDialog
          fileIds={[...selectedIds]}
          onClose={() => setShowShareDialog(false)}
          onSuccess={() => clearSelection()}
        />
      )}

      {dirPickerMode && (
        <div className="fm-overlay" onClick={() => setDirPickerMode(null)}>
          <div className="fm-modal fm-modal-wide" onClick={(e) => e.stopPropagation()}>
            <h3 className="fm-modal-title">
              {dirPickerMode === 'move' ? '移动到…' : '复制到…'}
            </h3>
            <nav className="fm-breadcrumb" style={{ marginBottom: 12 }}>
              {dirPickerBreadcrumb.map((seg, i) => (
                <span key={i} className="fm-bc-segment">
                  {i > 0 && <span className="fm-bc-sep">/</span>}
                  {i === dirPickerBreadcrumb.length - 1 ? (
                    <span className="fm-bc-current">{seg.name}</span>
                  ) : (
                    <button className="fm-bc-link" onClick={() => dirPickerNavigateBreadcrumb(i)}>
                      {seg.name}
                    </button>
                  )}
                </span>
              ))}
            </nav>
            {dirPickerLoading ? (
              <LoadingSpinner />
            ) : dirPickerItems.length === 0 ? (
              <p style={{ color: 'var(--color-text-tertiary)', fontSize: 13, padding: '8px 0' }}>
                此目录下没有文件夹
              </p>
            ) : (
              <div className="fm-dir-picker-list">
                {dirPickerItems.map((dir) => (
                  <div
                    key={dir.id}
                    className={`fm-dir-picker-item${dirPickerSelected === dir.id ? ' fm-dir-picker-selected' : ''}`}
                    onClick={() => setDirPickerSelected(dir.id)}
                  >
                    <DirIcon />
                    <span className="fm-dir-picker-name">{dir.name}</span>
                    <button
                      className="fm-dir-picker-open"
                      title="打开此目录"
                      onClick={(e) => {
                        e.stopPropagation();
                        setDirPickerSelected(dir.id);
                        dirPickerNavigate(dir.id, dir.name);
                      }}
                    >
                      打开
                    </button>
                  </div>
                ))}
              </div>
            )}
            <div className="fm-modal-actions">
              <button className="fm-btn fm-btn-primary" onClick={handleBatchMoveCopy}>
                确定
              </button>
              <button className="fm-btn" type="button" onClick={() => setDirPickerMode(null)}>
                取消
              </button>
            </div>
          </div>
        </div>
      )}

      {selectedIds.size > 0 && (
        <div className="fm-batch-bar">
          <span className="fm-batch-count">已选择 {selectedIds.size} 项</span>
          <div className="fm-batch-actions">
            <button className="fm-btn fm-btn-batch" onClick={() => openDirPicker('copy')}>
              <CopyIcon size={14} />复制
            </button>
            <button className="fm-btn fm-btn-batch" onClick={() => openDirPicker('move')}>
              <DirIcon />移动
            </button>
            <button className="fm-btn fm-btn-batch" onClick={() => setShowShareDialog(true)}>
              <ShareIcon size={14} />分享
            </button>
            <button className="fm-btn fm-btn-batch fm-btn-batch-danger" onClick={handleBatchDelete}>
              <TrashIcon size={14} />删除
            </button>
          </div>
          <button className="fm-btn fm-btn-batch" onClick={clearSelection}>
            取消选择
          </button>
        </div>
      )}
    </div>
  );
}
