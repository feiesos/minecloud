import { apiClient } from './client';

// 分享响应类型
export interface ShareResponse {
  id: number;
  shareToken: string;
  fileNodeId: number;
  ownerId: number;
  needPassword: boolean;
  expireAt: string | null;
  expired: boolean;
  maxDownloads: number;
  downloadCount: number;
  remark: string | null;
  createdAt: string;
  shareUrl: string;
}

// 创建分享请求类型
export interface CreateShareRequest {
  fileNodeId: number;
  accessPassword?: string;
  expireAt?: string;
  maxDownloads?: number;
  remark?: string;
}

// 公开分享信息类型
export interface PublicShareInfoResponse {
  shareToken: string;
  fileNodeId: number;
  needPassword: boolean;
  expireAt: string | null;
  expired: boolean;
  maxDownloads: number;
  downloadCount: number;
  remark: string | null;
}

// 创建分享链接
export async function createShare(request: CreateShareRequest): Promise<ShareResponse> {
  return apiClient<ShareResponse>('/share/create', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

// 获取用户的分享列表
export async function listShares(): Promise<ShareResponse[]> {
  return apiClient<ShareResponse[]>('/share/list');
}

// 获取分享详情
export async function getShareDetail(shareId: number): Promise<ShareResponse> {
  return apiClient<ShareResponse>(`/share/${shareId}`);
}

// 更新分享设置
export async function updateShare(shareId: number, request: CreateShareRequest): Promise<ShareResponse> {
  return apiClient<ShareResponse>(`/share/${shareId}`, {
    method: 'PUT',
    body: JSON.stringify(request),
  });
}

// 删除分享链接
export async function deleteShare(shareId: number): Promise<void> {
  await apiClient<void>(`/share/${shareId}`, {
    method: 'DELETE',
  });
}

// 获取公开分享信息（无需认证）
export async function getPublicShareInfo(shareToken: string): Promise<PublicShareInfoResponse> {
  const res = await fetch(`/api/v1/share/public/${shareToken}`);
  if (!res.ok) {
    const body = await res.json().catch(() => ({ msg: '分享不存在' }));
    throw new Error(body.msg || '分享不存在');
  }
  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.msg || '获取分享信息失败');
  }
  return json.data as PublicShareInfoResponse;
}

// 验证分享访问密码（无需认证）
export async function verifyShareAccess(shareToken: string, password: string): Promise<boolean> {
  const res = await fetch(`/api/v1/share/public/${shareToken}/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ password }),
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({ msg: '验证失败' }));
    throw new Error(body.msg || '验证失败');
  }
  const json = await res.json();
  return json.code === 200;
}

// 获取分享文件下载链接
export function getShareDownloadUrl(shareToken: string): string {
  return `/api/v1/share/public/${shareToken}/download`;
}