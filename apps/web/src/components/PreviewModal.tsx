import { useEffect, useRef, useState } from 'react';
import hljs from 'highlight.js';
import { fetchPreviewBlobUrl, fetchPreviewText } from '../api/files';
import LoadingSpinner from './LoadingSpinner';
import 'highlight.js/styles/github-dark.css';
import './PreviewModal.css';

type FileType = 'image' | 'text' | 'pdf' | 'audio' | 'video' | 'other';

function detectType(name: string): FileType {
  const ext = name.split('.').pop()?.toLowerCase() || '';
  if (['jpg', 'jpeg', 'png', 'gif', 'svg', 'webp', 'ico', 'bmp'].includes(ext)) return 'image';
  if (ext === 'pdf') return 'pdf';
  if (['mp3', 'wav', 'ogg', 'flac', 'aac', 'wma'].includes(ext)) return 'audio';
  if (['mp4', 'webm', 'avi', 'mov', 'mkv'].includes(ext)) return 'video';
  if (['txt', 'md', 'markdown', 'html', 'htm', 'css', 'js', 'ts', 'tsx', 'jsx',
       'json', 'xml', 'yml', 'yaml', 'java', 'kt', 'kts', 'py', 'rb', 'go', 'rs',
       'c', 'cpp', 'h', 'hpp', 'sql', 'sh', 'bash', 'zsh', 'bat', 'cmd', 'ps1',
       'log', 'cfg', 'ini', 'conf', 'env', 'gradle', 'properties'].includes(ext)) return 'text';
  return 'other';
}

interface PreviewModalProps {
  id: string;
  name: string;
  onClose: () => void;
}

export default function PreviewModal({ id, name, onClose }: PreviewModalProps) {
  const type = detectType(name);
  const [blobUrl, setBlobUrl] = useState('');
  const [textContent, setTextContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const overlayRef = useRef<HTMLDivElement>(null);
  const codeRef = useRef<HTMLElement>(null);

  useEffect(() => {
    if (type === 'text' && textContent && codeRef.current) {
      codeRef.current.textContent = textContent;
      hljs.highlightElement(codeRef.current);
    }
  }, [textContent, type]);

  useEffect(() => {
    let cancelled = false;
    const blobUrlRef = { current: '' };
    async function load() {
      try {
        if (type === 'text') {
          const text = await fetchPreviewText(id);
          if (!cancelled) { setTextContent(text); setLoading(false); }
        } else if (type === 'other') {
          setLoading(false);
        } else {
          const url = await fetchPreviewBlobUrl(id);
          if (!cancelled) { setBlobUrl(url); blobUrlRef.current = url; setLoading(false); }
        }
      } catch (err) {
        if (!cancelled) { setError(err instanceof Error ? err.message : '加载失败'); setLoading(false); }
      }
    }
    load();
    return () => { cancelled = true; if (blobUrlRef.current) URL.revokeObjectURL(blobUrlRef.current); };
  }, [id]);

  useEffect(() => {
    function handleKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [onClose]);

  function handleOverlayClick(e: React.MouseEvent) {
    if (e.target === overlayRef.current) onClose();
  }

  return (
    <div className="preview-overlay" ref={overlayRef} onClick={handleOverlayClick}>
      <div className="preview-modal">
        <div className="preview-header">
          <span className="preview-name">{name}</span>
          <button className="preview-close" onClick={onClose}>✕</button>
        </div>
        <div className="preview-body">
          {loading && <LoadingSpinner />}
          {error && <div className="preview-error">{error}</div>}
          {!loading && !error && type === 'image' && blobUrl && (
            <img className="preview-image" src={blobUrl} alt={name} />
          )}
          {!loading && !error && type === 'text' && (
            <pre className="preview-text"><code ref={codeRef} /></pre>
          )}
          {!loading && !error && type === 'pdf' && blobUrl && (
            <embed className="preview-pdf" src={blobUrl} type="application/pdf" />
          )}
          {!loading && !error && type === 'audio' && blobUrl && (
            <div className="preview-media-wrapper">
              <audio className="preview-audio" src={blobUrl} controls autoPlay />
            </div>
          )}
          {!loading && !error && type === 'video' && blobUrl && (
            <div className="preview-media-wrapper">
              <video className="preview-video" src={blobUrl} controls autoPlay />
            </div>
          )}
          {!loading && !error && type === 'other' && (
            <div className="preview-unsupported">该格式暂不支持预览</div>
          )}
        </div>
      </div>
    </div>
  );
}
