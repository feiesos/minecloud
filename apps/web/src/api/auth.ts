import { apiClient } from './client';
import { tokenStore, type StoredUser } from './tokenStore';

interface LoginResponse {
  token: string;
  refreshToken: string;
  userId: number;
  username: string;
  nickname: string;
}

interface RegisterResponse {
  userId: number;
  username: string;
  email: string;
}

export async function login(username: string, password: string): Promise<StoredUser> {
  const data = await apiClient<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  tokenStore.setTokens(data.token, data.refreshToken);
  const user: StoredUser = {
    userId: data.userId,
    username: data.username,
    nickname: data.nickname,
  };
  tokenStore.setUser(user);
  return user;
}

export async function register(
  username: string,
  password: string,
  email: string,
  nickname?: string,
): Promise<RegisterResponse> {
  const data = await apiClient<RegisterResponse>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password, email, nickname }),
  });
  return data;
}

export async function verifyEmail(token: string): Promise<void> {
  const res = await fetch(`/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`, {
    method: 'POST',
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({ msg: '验证失败' }));
    throw new Error(body.msg || '验证失败');
  }
  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.msg || '验证失败');
  }
}

export async function forgotPassword(email: string): Promise<void> {
  await apiClient('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  await apiClient('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify({ token, newPassword }),
  });
}

export function logout() {
  const refreshToken = tokenStore.getRefreshToken();
  if (refreshToken) {
    fetch('/api/v1/auth/logout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    }).catch(() => {});
  }
  tokenStore.clear();
  window.dispatchEvent(new Event('auth:logout'));
}

export function getStoredUser(): StoredUser | null {
  return tokenStore.getUser();
}

export function isAuthenticated(): boolean {
  return !!tokenStore.getAccessToken();
}
