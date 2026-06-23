import { apiClient } from './client';
import { tokenStore } from './tokenStore';

export interface LoginResponse {
  token: string;
  refreshToken: string;
  userId: string;
  username: string;
  nickname: string;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const data = await apiClient<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
  tokenStore.setTokens(data.token, data.refreshToken);
  return data;
}

export async function register(username: string, password: string, email: string): Promise<void> {
  await apiClient<void>('/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, password, email }),
  });
}

export function logout() {
  tokenStore.clear();
}

export function isAuthenticated(): boolean {
  return tokenStore.getAccessToken() !== null;
}
