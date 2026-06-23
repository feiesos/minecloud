import { apiClient } from './client';
import { API_BASE_URL } from './config';

export interface FileItem {
  id: string;
  name: string;
  parentId: string;
  isDir: boolean;
  size: number;
  createTime: string;
}

export interface SearchResult {
  id: string;
  name: string;
  parentId: string | null;
  isDir: boolean;
  size: number;
  createTime: string;
  path: string;
}

export interface SearchResponse {
  items: SearchResult[];
  total: number;
}

export async function listFiles(parentId: string): Promise<FileItem[]> {
  return apiClient<FileItem[]>(`/files/list?parentId=${parentId}`);
}

export async function searchFiles(
  q: string,
  type?: string,
  sort?: string,
  order?: string,
): Promise<SearchResponse> {
  const params = new URLSearchParams({ q });
  if (type) params.set('type', type);
  if (sort) params.set('sort', sort);
  if (order) params.set('order', order);
  return apiClient<SearchResponse>(`/search?${params.toString()}`);
}

export interface PreviewResponse {
  url: string;
}

export async function renameFile(id: string, name: string): Promise<void> {
  await apiClient<void>(`/files/${id}/rename`, {
    method: 'PUT',
    body: JSON.stringify({ name }),
  });
}

export async function deleteFile(id: string): Promise<void> {
  await apiClient<void>(`/files/${id}`, { method: 'DELETE' });
}

export function getDownloadUrl(id: string): string {
  return `${API_BASE_URL}/files/download/${id}`;
}

export function getPreviewUrl(id: string): string {
  return `${API_BASE_URL}/files/${id}/preview`;
}
