import { useState, useCallback } from 'react';
import { uploadFile, quickUpload, uploadChunk, mergeChunks } from '../api/files';
import { computeMD5 } from '../utils/hash';
import { DIRECT_UPLOAD_MAX_SIZE, UPLOAD_CHUNK_SIZE } from '../constants';

export interface UseUploadReturn {
  upload: (file: File, currentPath: string) => Promise<void>;
  uploading: boolean;
  uploadProgress: string;
}

export function useUpload(): UseUploadReturn {
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState('');

  const upload = useCallback(async (file: File, currentPath: string) => {
    setUploading(true);
    setUploadProgress('计算哈希中…');
    try {
      const md5 = await computeMD5(file);

      if (file.size <= DIRECT_UPLOAD_MAX_SIZE) {
        await uploadFile(file, currentPath, md5);
        return;
      }

      setUploadProgress('检测中…');
      try {
        await quickUpload(md5, file.name, currentPath);
        return;
      } catch (err) {
        const msg = err instanceof Error ? err.message : '';
        if (!msg.includes('未找到匹配的文件哈希')) throw err;
      }

      const totalChunks = Math.ceil(file.size / UPLOAD_CHUNK_SIZE);
      for (let i = 0; i < totalChunks; i++) {
        setUploadProgress(`上传中 ${i + 1}/${totalChunks}`);
        const start = i * UPLOAD_CHUNK_SIZE;
        const end = Math.min(start + UPLOAD_CHUNK_SIZE, file.size);
        await uploadChunk(file.slice(start, end), md5, i);
      }

      setUploadProgress('合并中…');
      await mergeChunks(md5, file.name, currentPath);
    } finally {
      setUploading(false);
      setUploadProgress('');
    }
  }, []);

  return { upload, uploading, uploadProgress };
}
