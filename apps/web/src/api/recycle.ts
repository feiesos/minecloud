import { apiClient } from './client';

export interface RecycleItem {
  id: string;
  name: string;
  parentId: string | null;
  isDir: boolean;
  size: number;
  createTime: string;
  deleteTime: string;
}

export async function listRecycle(): Promise<RecycleItem[]> {
  return apiClient<RecycleItem[]>('/recycle');
}

export async function restoreItem(id: string): Promise<void> {
  await apiClient<void>(`/recycle/${id}/restore`, { method: 'POST' });
}

export async function purgeItem(id: string): Promise<void> {
  await apiClient<void>(`/recycle/${id}`, { method: 'DELETE' });
}
